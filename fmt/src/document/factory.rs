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
