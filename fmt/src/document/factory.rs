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

use snafu::ResultExt;
use std::borrow::Cow;
use std::{
    collections::{HashMap, HashSet},
    path::Path,
};

use crate::error::CreateDocumentSnafu;
use crate::Result;
use crate::{config::Mapping, document::Document, header::model::HeaderDef};

pub struct DocumentFactory {
    mapping: HashSet<Mapping>,
    definitions: HashMap<String, HeaderDef>,
    properties: HashMap<String, String>,

    keywords: Vec<String>,
}

impl DocumentFactory {
    pub fn new(
        mapping: HashSet<Mapping>,
        definitions: HashMap<String, HeaderDef>,
        properties: HashMap<String, String>,
        keywords: Vec<String>,
    ) -> Self {
        Self {
            mapping,
            definitions,
            properties,
            keywords,
        }
    }

    pub fn create_document(&self, filepath: &Path) -> Result<Option<Document>> {
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
            .context(CreateDocumentSnafu {
                path: filepath.display().to_string(),
            })?;

        let mut properties = self.properties.clone();
        let filename = filepath
            .file_name()
            .map(|s| s.to_string_lossy())
            .unwrap_or_else(|| Cow::Borrowed("<unknown>"))
            .to_string();
        properties.insert("hawkeye.core.filename".to_string(), filename);

        Document::new(
            filepath.to_path_buf(),
            header_def.clone(),
            &self.keywords,
            properties,
        )
    }
}
