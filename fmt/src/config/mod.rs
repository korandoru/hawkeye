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

use std::{
    collections::{HashMap, HashSet},
    hash::{Hash, Hasher},
    path::PathBuf,
};

use serde::{de::Error, Deserialize, Deserializer, Serialize};
use toml::Value;

use crate::default_true;

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(default, rename_all = "camelCase")]
pub struct Config {
    #[serde(default = "default_cwd")]
    pub base_dir: PathBuf,

    pub inline_header: Option<String>,
    pub header_path: Option<String>,

    #[serde(default = "default_true")]
    pub strict_check: bool,
    #[serde(default = "default_true")]
    pub use_default_excludes: bool,
    #[serde(default = "default_true")]
    pub use_default_mapping: bool,
    #[serde(default = "default_keywords")]
    pub keywords: Vec<String>,

    pub includes: Vec<String>,
    pub excludes: Vec<String>,

    #[serde(deserialize_with = "de_properties")]
    pub properties: HashMap<String, String>,
    #[serde(deserialize_with = "de_mapping")]
    pub mapping: HashSet<Mapping>,

    pub git: Git,

    pub additional_headers: Vec<String>,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub struct Git {
    pub attrs: FeatureGate,
    pub ignore: FeatureGate,
}

impl Default for Git {
    fn default() -> Self {
        Git {
            attrs: FeatureGate::Disable, // expensive
            ignore: FeatureGate::Auto,
        }
    }
}

#[derive(Debug, Clone, Copy, Ord, PartialOrd, Eq, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum FeatureGate {
    /// Determinate whether turn on the feature.
    Auto,
    /// Force enable the feature.
    Enable,
    /// Force disable the feature.
    Disable,
}

impl FeatureGate {
    pub fn is_enable(&self) -> bool {
        match self {
            FeatureGate::Auto => false,
            FeatureGate::Enable => true,
            FeatureGate::Disable => false,
        }
    }

    pub fn is_disable(&self) -> bool {
        match self {
            FeatureGate::Auto => false,
            FeatureGate::Enable => false,
            FeatureGate::Disable => true,
        }
    }

    pub fn is_auto(&self) -> bool {
        match self {
            FeatureGate::Auto => true,
            FeatureGate::Enable => false,
            FeatureGate::Disable => false,
        }
    }
}

#[derive(Debug, Clone, Eq, PartialEq, Serialize, Deserialize)]
pub enum Mapping {
    Filename {
        pattern: String,
        header_type: String,
    },
    Extension {
        pattern: String,
        header_type: String,
    },
}

impl Hash for Mapping {
    fn hash<H: Hasher>(&self, state: &mut H) {
        state.write(match self {
            Mapping::Filename { pattern, .. } => pattern.as_bytes(),
            Mapping::Extension { pattern, .. } => pattern.as_bytes(),
        });
    }
}

impl Mapping {
    pub fn header_type(&self, filename: &str) -> Option<String> {
        let filename = filename.to_lowercase();
        match self {
            Mapping::Filename {
                header_type,
                pattern,
            } => {
                let pattern = pattern.to_lowercase();
                (filename == pattern).then(|| header_type.clone())
            }
            Mapping::Extension {
                header_type,
                pattern,
            } => {
                let pattern = format!(".{pattern}").to_lowercase();
                filename.ends_with(&pattern).then(|| header_type.clone())
            }
        }
    }
}

fn default_cwd() -> PathBuf {
    ".".into()
}

fn default_keywords() -> Vec<String> {
    vec!["copyright".to_string()]
}

fn de_properties<'de, D>(de: D) -> Result<HashMap<String, String>, D::Error>
where
    D: Deserializer<'de>,
{
    HashMap::<String, Value>::deserialize(de)?
        .into_iter()
        .map(|(k, v)| {
            let v = match v {
                Value::String(v) => Ok(v),
                Value::Integer(v) => Ok(v.to_string()),
                Value::Float(v) => Ok(v.to_string()),
                Value::Boolean(v) => Ok(v.to_string()),
                Value::Datetime(v) => Ok(v.to_string()),
                Value::Array(_) => Err(Error::custom("array cannot be property value")),
                Value::Table(_) => Err(Error::custom("table cannot be property value")),
            }?;
            Ok((k, v))
        })
        .collect()
}

fn de_mapping<'de, D>(de: D) -> Result<HashSet<Mapping>, D::Error>
where
    D: Deserializer<'de>,
{
    #[derive(Debug, Default, Clone, Deserialize)]
    #[serde(default)]
    struct MappingModel {
        extensions: Vec<String>,
        filenames: Vec<String>,
    }

    let mappings = HashMap::<String, MappingModel>::deserialize(de)?;
    let mut set = HashSet::new();
    for (header_type, model) in mappings {
        for pattern in model.extensions {
            set.insert(Mapping::Extension {
                pattern,
                header_type: header_type.clone(),
            });
        }
        for pattern in model.filenames {
            set.insert(Mapping::Filename {
                pattern,
                header_type: header_type.clone(),
            });
        }
    }
    Ok(set)
}
