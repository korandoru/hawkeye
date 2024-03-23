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
    collections::{HashMap, HashSet},
    path::PathBuf,
};

use snafu::{OptionExt, ResultExt};

use crate::{
    config::Mapping,
    document::Document,
    error::{CreateDocumentSnafu, HeaderDefinitionNotFoundSnafu},
    header::model::HeaderDef,
    Result,
};

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

    pub fn create_document(&self, filepath: &PathBuf) -> Result<Document> {
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
            .context(HeaderDefinitionNotFoundSnafu { header_type })?;
        let document = Document::new(
            filepath.clone(),
            header_def.clone(),
            &self.keywords,
            self.properties.clone(),
        );
        document.context(CreateDocumentSnafu {
            path: filepath.display().to_string(),
        })
    }
}
