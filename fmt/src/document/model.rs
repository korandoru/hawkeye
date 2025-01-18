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

use serde::Deserialize;
use serde::Serialize;

use crate::config::Mapping;

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct DocumentType {
    pub pattern: String,
    pub header_type: String,
    pub extension: bool,
    pub filename: bool,
}

pub fn default_mapping() -> Vec<Mapping> {
    let defaults = include_str!("defaults.toml");
    let mapping: HashMap<String, DocumentType> =
        toml::from_str(defaults).expect("default mapping must be valid");

    mapping
        .into_iter()
        .flat_map(|(_, doctype)| {
            let mut ms = vec![];
            if doctype.extension {
                ms.push(Mapping::Extension {
                    pattern: doctype.pattern.clone(),
                    header_type: doctype.header_type.clone(),
                })
            }
            if doctype.filename {
                ms.push(Mapping::Filename {
                    pattern: doctype.pattern,
                    header_type: doctype.header_type,
                })
            }
            ms
        })
        .collect()
}
