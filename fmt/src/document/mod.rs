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

use std::{collections::HashMap, fs, fs::File, io::BufRead, path::PathBuf};

use snafu::ResultExt;
use tracing::debug;

use crate::error::CreateDocumentSnafu;
use crate::header::parser::{parse_header, FileContent};
use crate::{
    error::SaveDocumentSnafu,
    header::{matcher::HeaderMatcher, model::HeaderDef, parser::HeaderParser},
    Result,
};

pub mod factory;
pub mod model;

#[derive(Debug)]
pub struct Document {
    pub filepath: PathBuf,

    header_def: HeaderDef,
    properties: HashMap<String, String>,
    parser: HeaderParser,
}

impl Document {
    pub fn new(
        filepath: PathBuf,
        header_def: HeaderDef,
        keywords: &[String],
        properties: HashMap<String, String>,
    ) -> Result<Option<Self>> {
        match FileContent::new(&filepath) {
            Ok(content) => Ok(Some(Self {
                parser: parse_header(content, &header_def, keywords),
                filepath,
                header_def,
                properties,
            })),
            Err(e) => {
                if matches!(e.kind(), std::io::ErrorKind::InvalidData) {
                    debug!("skip non-textual file: {}", filepath.display());
                    Ok(None)
                } else {
                    Err(e).context(CreateDocumentSnafu {
                        path: filepath.display().to_string(),
                    })
                }
            }
        }
    }

    pub fn is_unsupported(&self) -> bool {
        self.header_def.name.eq_ignore_ascii_case("unknown")
    }

    /// Detected but not necessarily a valid header
    pub fn header_detected(&self) -> bool {
        self.parser.end_pos.is_some()
    }

    /// Detected and valid header
    pub fn header_matched(
        &self,
        header: &HeaderMatcher,
        strict_check: bool,
    ) -> std::io::Result<bool> {
        if strict_check {
            let file_header = {
                let mut lines = self.read_file_first_lines(header)?.join("\n");
                lines.push_str("\n\n");
                lines.replace(" *\r?\n", "\n")
            };
            let expected_header = {
                let raw_header = header.build_for_definition(&self.header_def);
                let resolved_header = self.merge_properties(&raw_header);
                resolved_header.replace(" *\r?\n", "\n")
            };
            Ok(file_header.contains(expected_header.as_str()))
        } else {
            let file_header = self.read_file_header_on_one_line(header)?;
            let expected_header = self.merge_properties(header.header_content_one_line());
            Ok(file_header.contains(expected_header.as_str()))
        }
    }

    fn read_file_first_lines(&self, header: &HeaderMatcher) -> std::io::Result<Vec<String>> {
        let file = File::open(&self.filepath)?;
        std::io::BufReader::new(file)
            .lines()
            .take(header.header_content_lines_count() + 10)
            .collect::<std::io::Result<Vec<_>>>()
    }

    fn read_file_header_on_one_line(&self, header: &HeaderMatcher) -> std::io::Result<String> {
        let first_lines = self.read_file_first_lines(header)?;
        let file_header = first_lines
            .join("")
            .trim()
            .replace(self.header_def.first_line.trim(), "")
            .replace(self.header_def.end_line.trim(), "")
            .replace(self.header_def.before_each_line.trim(), "")
            .replace(self.header_def.after_each_line.trim(), "")
            .split_whitespace()
            .collect();
        Ok(file_header)
    }

    pub fn update_header(&mut self, header: &HeaderMatcher) {
        let header_str = header.build_for_definition(&self.header_def);
        let header_str = self.merge_properties(&header_str);
        let begin_pos = self.parser.begin_pos;
        self.parser
            .file_content
            .insert(begin_pos, header_str.as_str());
    }

    pub fn remove_header(&mut self) {
        if let Some(end_pos) = self.parser.end_pos {
            self.parser
                .file_content
                .delete(self.parser.begin_pos, end_pos);
        }
    }

    pub fn save(&mut self, filepath: Option<&PathBuf>) -> Result<()> {
        let filepath = filepath.unwrap_or(&self.filepath);
        fs::write(filepath, self.parser.file_content.content()).context(SaveDocumentSnafu {
            path: filepath.display().to_string(),
        })
    }

    pub(crate) fn merge_properties(&self, s: &str) -> String {
        merge_properties(&self.properties, s)
    }
}

pub fn merge_properties(properties: &HashMap<String, String>, s: &str) -> String {
    let mut result = s.to_string();
    for (key, value) in properties {
        result = result.replace(&format!("${{{key}}}"), value);
    }
    result
}
