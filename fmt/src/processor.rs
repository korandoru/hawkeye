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

use std::collections::hash_map::Entry;
use std::collections::HashMap;
use std::fs;
use std::path::Path;
use std::path::PathBuf;

use anyhow::Context;

use crate::config::Config;
use crate::document::factory::DocumentFactory;
use crate::document::model::default_mapping;
use crate::document::Document;
use crate::git;
use crate::header::matcher::HeaderMatcher;
use crate::header::model::default_headers;
use crate::header::model::deserialize_header_definitions;
use crate::license::HeaderSource;
use crate::selection::Selection;

/// Callback for processing the result of checking license headers.
pub trait Callback {
    /// Called when the header is unknown.
    fn on_unknown(&mut self, path: &Path);

    /// Called when the header is matched.
    fn on_matched(&mut self, header: &HeaderMatcher, document: Document) -> anyhow::Result<()>;

    /// Called when the header is not matched.
    fn on_not_matched(&mut self, header: &HeaderMatcher, document: Document) -> anyhow::Result<()>;
}

pub fn check_license_header<C: Callback>(
    run_config: PathBuf,
    callback: &mut C,
) -> anyhow::Result<()> {
    let config = {
        let name = run_config.display().to_string();
        let config = fs::read_to_string(&run_config)
            .with_context(|| format!("cannot load config: {name}"))?;
        toml::from_str::<Config>(&config)
            .map_err(Box::new)
            .with_context(|| format!("cannot parse config file: {name}"))?
    };

    let basedir = config.base_dir.clone();
    anyhow::ensure!(
        basedir.is_dir(),
        format!(
            "{} does not exist or is not a directory.",
            basedir.display()
        )
    );

    let git_context = git::discover(&basedir, config.git)?;

    let selected_files = {
        let selection = Selection::new(
            basedir,
            config.header_path.as_ref(),
            &config.includes,
            &config.excludes,
            config.use_default_excludes,
            git_context.clone(),
        );
        selection.select()?
    };

    let mapping = {
        let mut mapping = config.mapping.clone();
        if config.use_default_mapping {
            let default_mapping = default_mapping();
            for m in default_mapping {
                if let Some(o) = mapping.get(&m) {
                    log::warn!("default mapping {m:?} is override by {o:?}");
                    continue;
                }
                mapping.insert(m);
            }
        }
        mapping
    };

    let definitions = {
        let mut defs = HashMap::new();
        for (k, v) in default_headers() {
            match defs.entry(k) {
                Entry::Occupied(mut ent) => {
                    log::warn!("Default header {} is override", ent.key());
                    ent.insert(v);
                }
                Entry::Vacant(ent) => {
                    ent.insert(v);
                }
            }
        }
        for additional_header in &config.additional_headers {
            let additional_defs = fs::read_to_string(additional_header)
                .with_context(|| format!("cannot load header definitions: {additional_header}"))
                .and_then(deserialize_header_definitions)?;
            for (k, v) in additional_defs {
                match defs.entry(k) {
                    Entry::Occupied(mut ent) => {
                        log::warn!("Additional header {} is override", ent.key());
                        ent.insert(v);
                    }
                    Entry::Vacant(ent) => {
                        ent.insert(v);
                    }
                }
            }
        }
        defs
    };

    let header_matcher = {
        let header_source = HeaderSource::from_config(&config)?;
        HeaderMatcher::new(header_source.content)
    };

    let git_file_attrs = git::resolve_file_attrs(git_context)?;

    let document_factory = DocumentFactory::new(
        mapping,
        definitions,
        config.properties,
        config.keywords,
        git_file_attrs,
    );

    for file in selected_files {
        let document = match document_factory.create_document(&file)? {
            Some(document) => document,
            None => {
                callback.on_unknown(&file);
                continue;
            }
        };

        if document.is_unsupported() {
            callback.on_unknown(&file);
        } else if document
            .header_matched(&header_matcher, config.strict_check)
            .context("failed to match header")?
        {
            callback.on_matched(&header_matcher, document)?;
        } else {
            callback.on_not_matched(&header_matcher, document)?;
        }
    }

    Ok(())
}
