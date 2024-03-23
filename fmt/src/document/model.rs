use std::collections::HashMap;

use serde::{Deserialize, Serialize};

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
        toml::from_str(&defaults).expect("default mapping must be valid");

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
