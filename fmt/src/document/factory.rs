use std::{collections::HashMap, path::PathBuf};

use crate::{config::Mapping, header::model::HeaderDef};

pub struct DocumentFactory {
    mapping: Vec<Mapping>,
    definitions: HashMap<String, HeaderDef>,
    properties: HashMap<String, String>,

    basedir: PathBuf,
    keywords: Vec<String>,
}

impl DocumentFactory {
    pub fn new(
        mapping: Vec<Mapping>,
        definitions: HashMap<String, HeaderDef>,
        properties: HashMap<String, String>,
        basedir: PathBuf,
        keywords: Vec<String>,
    ) -> Self {
        Self {
            mapping,
            definitions,
            properties,
            basedir,
            keywords,
        }
    }
}
