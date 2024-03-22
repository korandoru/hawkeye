use std::{
    collections::{HashMap, HashSet},
    hash::{Hash, Hasher},
    path::PathBuf,
};

use serde::{de::Error, Deserialize, Deserializer, Serialize};
use toml::Value;

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
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct Git {
    pub check_ignore: FeatureGate,
}

#[derive(Debug, Clone, Default, Ord, PartialOrd, Eq, PartialEq, Serialize, Deserialize)]
pub enum FeatureGate {
    /// Determinate whether turn on the feature.
    #[default]
    Auto,
    /// Force enable the feature.
    Enable,
    /// Force disable the feature.
    Disable,
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
                let pattern = pattern.to_lowercase();
                filename.ends_with(&pattern).then(|| header_type.clone())
            }
        }
    }
}

const fn default_true() -> bool {
    true
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
