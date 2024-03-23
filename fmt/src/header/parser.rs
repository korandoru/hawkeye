// Copyright 2024 tison <wander4096@gmail.com>
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use std::{
    fmt::{Display, Formatter},
    fs::File,
    io::BufRead,
    path::Path,
};

use crate::header::model::HeaderDef;

#[derive(Debug)]
pub struct HeaderParser {
    pub begin_pos: usize,
    /// Some if header exists; None if header does not exist.
    pub end_pos: Option<usize>,
    pub header_def: HeaderDef,
    pub file_content: FileContent,
}

pub fn parse_header(
    file: &Path,
    header_def: HeaderDef,
    keywords: &[String],
) -> std::io::Result<HeaderParser> {
    let mut file_content = FileContent::new(file)?;
    let mut line = file_content.next_line();

    // 1. find begin position
    let begin_pos = {
        let mut begin_pos = 0;
        if header_def.skip_line_pattern.is_some() {
            // the format expect to find lines to be skipped
            while let Some(l) = line.as_ref()
                && !header_def.is_skip_line(l)
            {
                begin_pos = file_content.pos;
                line = file_content.next_line();
            }

            // at least we have found the line to skip, or we are the end of the file
            // this time we are going to skip next lines if they match the skip pattern
            while let Some(l) = line.as_ref()
                && header_def.is_skip_line(l)
            {
                begin_pos = file_content.pos;
                line = file_content.next_line();
            }

            // After skipping everything we are at the end of the file
            // Header has to be at the file beginning
            if line.is_none() {
                begin_pos = 0;
                file_content.reset();
                line = file_content.next_line();
            }
        }
        begin_pos
    };

    // 2. has header
    let existing_header = {
        // skip blank lines
        while let Some(l) = line.as_ref()
            && l.trim().is_empty()
        {
            line = file_content.next_line();
        }

        // check if there is already a header
        let mut got_header = false;
        if let Some(l) = line.as_ref()
            && header_def.is_first_header_line(l)
        {
            let mut in_place_header = String::new();
            in_place_header.push_str(&l.to_lowercase());

            line = file_content.next_line();

            // skip blank lines before header text
            if header_def.allow_blank_lines {
                while let Some(l) = line.as_ref()
                    && l.trim().is_empty()
                {
                    line = file_content.next_line();
                }
            }

            // first header detected line & potential blank lines have been detected
            // following lines should be header lines
            if let Some(l) = line.as_ref() {
                in_place_header.push_str(&l.to_lowercase());

                let before = {
                    let mut before = header_def.before_each_line.trim_end();
                    if before.is_empty() && !header_def.multiple_lines {
                        before = header_def.before_each_line.as_str();
                    }
                    before
                };

                let found_end = {
                    let mut found_end = false;
                    if header_def.multiple_lines && header_def.is_last_header_line(l) {
                        found_end = true;
                    } else if l.trim().is_empty() {
                        found_end = true;
                    } else {
                        loop {
                            line = file_content.next_line();
                            if let Some(l) = line.as_ref()
                                && l.starts_with(before)
                            {
                                in_place_header.push_str(&l.to_lowercase());
                                if header_def.multiple_lines && header_def.is_last_header_line(l) {
                                    found_end = true;
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    }
                    found_end
                };

                // skip blank lines after header text
                if header_def.multiple_lines && header_def.allow_blank_lines && !found_end {
                    loop {
                        line = file_content.next_line();
                        if !line.as_ref().map(|l| l.trim().is_empty()).unwrap_or(false) {
                            break;
                        }
                    }
                    file_content.rewind();
                } else if !header_def.multiple_lines && !found_end {
                    file_content.rewind();
                }

                if !header_def.multiple_lines {
                    // keep track of the position for headers where the end line is the same as the
                    // before each line
                    let pos = file_content.pos;
                    // check if the line is the end line
                    while let Some(l) = line.as_ref()
                        && !header_def.is_last_header_line(l)
                        && (header_def.allow_blank_lines || !l.trim().is_empty())
                        && l.starts_with(before)
                    {
                        line = file_content.next_line();
                    }
                    if line.is_none() {
                        file_content.reset_to(pos);
                    }
                } else if line.is_some() {
                    // we could end up there if we still have some lines, but not matching "before".
                    // This can be the last line in a multi line header
                    let pos = file_content.pos;
                    line = file_content.next_line();
                    if line
                        .as_ref()
                        .map(|l| !header_def.is_last_header_line(l))
                        .unwrap_or(true)
                    {
                        file_content.reset_to(pos);
                    }
                }

                got_header = true;
                for keyword in keywords {
                    if !in_place_header.contains(keyword) {
                        got_header = false;
                        break;
                    }
                }
            }
            // else - we detected previously a one line comment block that matches the header
            // detection it is not a header it is a comment
        }

        got_header
    };

    // 3. find end position
    let end_pos = if existing_header {
        // we check if there is a header, if the next line is the blank line of the header
        let mut end = file_content.pos;
        line = file_content.next_line();
        if begin_pos == 0 {
            while let Some(l) = line.as_ref()
                && l.trim().is_empty()
            {
                end = file_content.pos;
                line = file_content.next_line();
            }
        }
        if header_def.end_line.ends_with("\n")
            && line.as_ref().map(|l| l.trim().is_empty()).unwrap_or(false)
        {
            end = file_content.pos;
        }
        Some(end)
    } else {
        None
    };

    Ok(HeaderParser {
        begin_pos,
        end_pos,
        header_def,
        file_content,
    })
}

#[derive(Debug)]
pub struct FileContent {
    pos: usize,
    old_pos: usize,
    content: String,
    filepath: String,
}

impl Display for FileContent {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.filepath)
    }
}

impl FileContent {
    pub fn new(file: &Path) -> std::io::Result<Self> {
        Ok(Self {
            pos: 0,
            old_pos: 0,
            content: {
                let f = File::open(file)?;
                std::io::BufReader::new(f)
                    .lines()
                    .collect::<std::io::Result<Vec<_>>>()?
                    .join("\n")
            },
            filepath: file.to_string_lossy().to_string(),
        })
    }

    pub fn reset_to(&mut self, pos: usize) {
        self.old_pos = pos;
        self.pos = pos;
    }

    pub fn reset(&mut self) {
        self.reset_to(0);
    }

    pub fn rewind(&mut self) {
        self.pos = self.old_pos;
    }

    pub fn end_reached(&self) -> bool {
        self.pos >= self.content.len()
    }

    pub fn next_line(&mut self) -> Option<String> {
        if self.end_reached() {
            return None;
        }

        let lf = self.content[self.pos..].find("\n").map(|i| i + self.pos);
        let eol = match lf {
            None | Some(0) => self.content.len(),
            Some(lf) => lf - (self.content.as_bytes()[lf - 1] == b'\r') as usize,
        };
        let result = self.content[self.pos..eol].to_string();

        self.old_pos = self.pos;
        self.pos = if let Some(lf) = lf {
            lf + 1
        } else {
            self.content.len()
        };

        Some(result)
    }

    pub fn content(&self) -> String {
        self.content.clone()
    }

    pub fn insert(&mut self, index: usize, s: &str) {
        self.content.insert_str(index, s);
    }

    pub fn delete(&mut self, start: usize, end: usize) {
        self.content.drain(start..end);
    }
}
