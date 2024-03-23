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

use std::fmt::{Display, Formatter};
use crate::header::model::HeaderDef;

#[derive(Debug)]
pub struct HeaderMatcher {
    header_content: String,
    header_content_one_line: String,
    header_content_lines: Vec<String>,
    max_length: usize,
}

impl HeaderMatcher {
    pub fn new(header_content: String) -> Self {
        let header_content_one_line = header_content.split_whitespace().collect();
        let header_content_lines = header_content
            .lines()
            .map(ToString::to_string)
            .collect::<Vec<_>>();
        let max_length = header_content_lines
            .iter()
            .map(|l| l.len())
            .max()
            .unwrap_or(0);
        Self {
            header_content,
            header_content_one_line,
            header_content_lines,
            max_length,
        }
    }

    pub fn build_for_definition(&self, def: &HeaderDef) -> String {
        let eol = "\n";
        let mut result = String::new();

        if !def.first_line.is_empty() {
            result.push_str(&def.first_line);
            if def.first_line != eol {
                result.push_str(eol);
            }
        }

        for line in &self.header_content_lines {
            let before = &def.before_each_line;
            let after = &def.after_each_line;
            let this_line = if def.pad_lines {
                let max_length = self.max_length;
                format!("{before}{line: <max_length$}{after}")
            } else {
                format!("{before}{line}{after}")
            };
            result.push_str(this_line.trim_end());
            result.push_str(eol);
        }

        if !def.end_line.is_empty() {
            result.push_str(&def.end_line);
            if def.end_line != eol {
                result.push_str(eol);
            }
        }

        result
    }

    pub fn header_content_lines_count(&self) -> usize {
        self.header_content_lines.len()
    }

    pub fn header_content_one_line(&self) -> &str {
        &self.header_content_one_line
    }
}

impl Display for HeaderMatcher {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.header_content)
    }
}