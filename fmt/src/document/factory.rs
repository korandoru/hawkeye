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
use std::collections::HashSet;
use std::fs;
use std::path::Path;
use std::path::PathBuf;
use std::time::SystemTime;

use exn::OptionExt;
use exn::Result;

use crate::config::Mapping;
use crate::document::Attributes;
use crate::document::Document;
use crate::error::Error;
use crate::git::GitFileAttrs;
use crate::header::model::HeaderDef;

pub struct DocumentFactory {
    mapping: HashSet<Mapping>,
    definitions: HashMap<String, HeaderDef>,
    properties: HashMap<String, String>,

    keywords: Vec<String>,
    git_file_attrs: HashMap<PathBuf, GitFileAttrs>,
}

impl DocumentFactory {
    pub fn new(
        mapping: HashSet<Mapping>,
        definitions: HashMap<String, HeaderDef>,
        properties: HashMap<String, String>,
        keywords: Vec<String>,
        git_file_attrs: HashMap<PathBuf, GitFileAttrs>,
    ) -> Self {
        Self {
            mapping,
            definitions,
            properties,
            keywords,
            git_file_attrs,
        }
    }

    pub fn create_document(&self, filepath: &Path) -> Result<Option<Document>, Error> {
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
        let header_def = self.definitions.get(&header_type).ok_or_raise(|| {
            Error::new(format!(
                "cannot create document: {}, header type {} not found",
                filepath.display(),
                header_type
            ))
        })?;

        let props = self.properties.clone();

        let filemeta = fs::metadata(filepath).ok();
        let attrs = Attributes {
            filename: filepath
                .file_name()
                .map(|s| s.to_string_lossy().to_string()),
            disk_file_created_year: filemeta
                .as_ref()
                .and_then(|m| m.created().ok())
                .and_then(file_time_to_year),
            git_file_created_year: self
                .git_file_attrs
                .get(filepath)
                .and_then(|attrs| git_time_to_year(attrs.created_time)),
            git_file_modified_year: self
                .git_file_attrs
                .get(filepath)
                .and_then(|attrs| git_time_to_year(attrs.modified_time)),
            git_authors: self
                .git_file_attrs
                .get(filepath)
                .map(|attrs| attrs.authors.clone())
                .unwrap_or_default(),
        };

        Document::new(
            filepath.to_path_buf(),
            header_def.clone(),
            &self.keywords,
            props,
            attrs,
        )
    }
}

fn file_time_to_year(time: SystemTime) -> Option<i16> {
    let ts = jiff::Timestamp::try_from(time).ok()?;
    Some(ts.to_zoned(jiff::tz::TimeZone::system()).year())
}

fn git_time_to_year(t: gix::date::Time) -> Option<i16> {
    let offset = jiff::tz::Offset::from_seconds(t.offset).expect("valid offset");
    let zoned = jiff::Timestamp::from_second(t.seconds)
        .expect("always valid unix time")
        .to_zoned(offset.to_time_zone());
    Some(zoned.year())
}
