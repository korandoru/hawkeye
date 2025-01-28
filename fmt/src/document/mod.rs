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

use std::collections::HashMap;
use std::fs;
use std::fs::File;
use std::io::BufRead;
use std::path::PathBuf;

use crate::header::matcher::HeaderMatcher;
use crate::header::model::HeaderDef;
use crate::header::parser::parse_header;
use crate::header::parser::FileContent;
use crate::header::parser::HeaderParser;
use anyhow::Context;
use minijinja::{context, Environment};
use serde::{Deserialize, Serialize};

pub mod factory;
pub mod model;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Attributes {
    pub filename: Option<String>,
    pub git_created_time: Option<String>,
    pub git_modified_time: Option<String>,
    pub git_authors: Vec<String>,
}

#[derive(Debug)]
pub struct Document {
    pub filepath: PathBuf,

    header_def: HeaderDef,
    props: HashMap<String, String>,
    attrs: Attributes,
    parser: HeaderParser,
}

impl Document {
    pub fn new(
        filepath: PathBuf,
        header_def: HeaderDef,
        keywords: &[String],
        props: HashMap<String, String>,
        attrs: Attributes,
    ) -> anyhow::Result<Option<Self>> {
        match FileContent::new(&filepath) {
            Ok(content) => Ok(Some(Self {
                parser: parse_header(content, &header_def, keywords),
                filepath,
                header_def,
                props,
                attrs,
            })),
            Err(e) => {
                if matches!(e.kind(), std::io::ErrorKind::InvalidData) {
                    log::debug!("skip non-textual file: {}", filepath.display());
                    Ok(None)
                } else {
                    Err(e).with_context(|| {
                        format!("cannot to create document: {}", filepath.display())
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
    ) -> anyhow::Result<bool> {
        if strict_check {
            let file_header = {
                let mut lines = self.read_file_first_lines(header)?.join("\n");
                lines.push_str("\n\n");
                lines.replace(" *\r?\n", "\n")
            };
            let expected_header = {
                let raw_header = header.build_for_definition(&self.header_def);
                let resolved_header = self.merge_properties(&raw_header)?;
                resolved_header.replace(" *\r?\n", "\n")
            };
            Ok(file_header.contains(expected_header.as_str()))
        } else {
            let file_header = self.read_file_header_on_one_line(header)?;
            let expected_header = self.merge_properties(header.header_content_one_line())?;
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

    pub fn update_header(&mut self, header: &HeaderMatcher) -> anyhow::Result<()> {
        let header_str = header.build_for_definition(&self.header_def);
        let header_str = self.merge_properties(&header_str)?;
        let begin_pos = self.parser.begin_pos;
        self.parser
            .file_content
            .insert(begin_pos, header_str.as_str());
        Ok(())
    }

    pub fn remove_header(&mut self) {
        if let Some(end_pos) = self.parser.end_pos {
            self.parser
                .file_content
                .delete(self.parser.begin_pos, end_pos);
        }
    }

    pub fn save(&mut self, filepath: Option<&PathBuf>) -> anyhow::Result<()> {
        let filepath = filepath.unwrap_or(&self.filepath);
        fs::write(filepath, self.parser.file_content.content())
            .context(format!("cannot save document {}", filepath.display()))
    }

    pub(crate) fn merge_properties(&self, s: &str) -> anyhow::Result<String> {
        let mut env = Environment::new();
        env.add_template("template", s)
            .context("malformed template")?;

        let tmpl = env.get_template("template").expect("template must exist");
        let mut result = tmpl.render(context! {
            props => &self.props,
            attrs => &self.attrs,
        })?;
        result.push_str("\n");
        Ok(result)
    }
}
