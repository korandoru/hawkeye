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
//
// Copyright 2024 - 2024, tison <wander4096@gmail.com> and the HawkEye contributors
// SPDX-License-Identifier: Apache-2.0

use std::borrow::Cow;
use std::collections::HashMap;
use std::collections::HashSet;
use std::path::Path;
use std::path::PathBuf;

use anyhow::Context;
use time::format_description;
use time::format_description::FormatItem;

use crate::config::Mapping;
use crate::document::Document;
use crate::git::GitFileAttrs;
use crate::header::model::HeaderDef;

pub struct DocumentFactory {
    mapping: HashSet<Mapping>,
    definitions: HashMap<String, HeaderDef>,
    properties: HashMap<String, String>,

    keywords: Vec<String>,
    git_file_attrs: HashMap<PathBuf, GitFileAttrs>,
    year_formatter: Vec<FormatItem<'static>>,
}

impl DocumentFactory {
    pub fn new(
        mapping: HashSet<Mapping>,
        definitions: HashMap<String, HeaderDef>,
        properties: HashMap<String, String>,
        keywords: Vec<String>,
        git_file_attrs: HashMap<PathBuf, GitFileAttrs>,
    ) -> Self {
        let year_formatter = format_description::parse("[year]").expect("cannot parse format");
        Self {
            mapping,
            definitions,
            properties,
            keywords,
            git_file_attrs,
            year_formatter,
        }
    }

    pub fn create_document(&self, filepath: &Path) -> anyhow::Result<Option<Document>> {
        let lower_file_name = filepath
            .file_name()
            .map(|n| n.to_string_lossy().to_lowercase())
            .unwrap_or_default();
        let header_type = self
            .mapping
            .iter()
            .find_map(|m| m.header_type(&lower_file_name))
            .unwrap_or_else(|| "unknown".to_string())
            .to_lowercase();
        let header_def = self
            .definitions
            .get(&header_type)
            .ok_or_else(|| std::io::Error::other(format!("header type {header_type} not found")))
            .with_context(|| format!("cannot to create document: {}", filepath.display()))?;

        let mut properties = self.properties.clone();

        let filename = filepath
            .file_name()
            .map(|s| s.to_string_lossy())
            .unwrap_or_else(|| Cow::Borrowed("<unknown>"))
            .to_string();
        properties.insert("hawkeye.core.filename".to_string(), filename);

        if let Some(attrs) = self.git_file_attrs.get(filepath) {
            properties.insert(
                "hawkeye.git.fileCreatedYear".to_string(),
                attrs.created_time.format(self.year_formatter.as_slice()),
            );
            properties.insert(
                "hawkeye.git.fileModifiedYear".to_string(),
                attrs.modified_time.format(self.year_formatter.as_slice()),
            );
        }

        Document::new(
            filepath.to_path_buf(),
            header_def.clone(),
            &self.keywords,
            properties,
        )
    }
}
