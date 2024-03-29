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
    fs,
    path::{Path, PathBuf},
};

use snafu::{ensure, OptionExt, ResultExt};
use tracing::debug;

use crate::{
    config::Config,
    document::{factory::DocumentFactory, model::default_mapping, Document},
    error::{
        CreateDocumentSnafu, DeserializeSnafu, InvalidConfigSnafu, LoadConfigSnafu,
        TryMatchHeaderSnafu,
    },
    header::{
        matcher::HeaderMatcher,
        model::{default_headers, deserialize_header_definitions},
    },
    license::HeaderSource,
    selection::Selection,
    Result,
};

/// Callback for processing the result of checking license headers.
pub trait Callback {
    /// Called when the header is unknown.
    fn on_unknown(&mut self, path: &Path) -> Result<()>;

    /// Called when the header is matched.
    fn on_matched(&mut self, header: &HeaderMatcher, document: Document) -> Result<()>;

    /// Called when the header is not matched.
    fn on_not_matched(&mut self, header: &HeaderMatcher, document: Document) -> Result<()>;
}

#[allow(clippy::type_complexity)]
pub fn check_license_header<C: Callback>(run_config: PathBuf, callback: &mut C) -> Result<()> {
    let config = {
        let name = run_config.display().to_string();
        let config =
            fs::read_to_string(&run_config).context(LoadConfigSnafu { name: name.clone() })?;
        toml::from_str::<Config>(&config)
            .map_err(Box::new)
            .context(DeserializeSnafu { name })?
    };

    let basedir = config.base_dir.clone();
    ensure!(
        basedir.is_dir(),
        InvalidConfigSnafu {
            message: format!(
                "{} does not exist or is not a directory.",
                basedir.display()
            )
        }
    );

    let header_matcher = {
        let header_source = HeaderSource::from_config(&config)?;
        HeaderMatcher::new(header_source.content)
    };

    let selected_files = {
        let selection = Selection::new(
            basedir,
            &config.includes,
            &config.excludes,
            config.use_default_excludes,
            config.git,
        );
        selection.select()?
    };

    let mapping = {
        let mut mapping = config.mapping;
        if config.use_default_mapping {
            let default_mapping = default_mapping();
            mapping.extend(default_mapping);
        }
        mapping
    };

    let definitions = {
        let mut defs = default_headers()?;
        for additional_header in &config.additional_headers {
            let additional_defs = fs::read_to_string(additional_header)
                .context(LoadConfigSnafu {
                    name: additional_header.clone(),
                })
                .and_then(deserialize_header_definitions)?;
            defs.extend(additional_defs);
        }
        defs
    };

    let document_factory =
        DocumentFactory::new(mapping, definitions, config.properties, config.keywords);

    for file in selected_files {
        let document = document_factory.create_document(&file);
        let document = match document {
            Ok(document) => document,
            Err(e) => {
                if matches!(e.kind(), std::io::ErrorKind::InvalidData) {
                    debug!("skip non-textual file: {}", file.display());
                    callback.on_unknown(&file)?;
                    continue;
                } else {
                    return Err(e).context(CreateDocumentSnafu {
                        path: file.display().to_string(),
                    });
                }
            }
        };

        if document.is_unsupported() {
            callback.on_unknown(&file)?;
        } else if document
            .header_matched(&header_matcher, config.strict_check)
            .context(TryMatchHeaderSnafu)?
        {
            callback.on_matched(&header_matcher, document)?;
        } else {
            callback.on_not_matched(&header_matcher, document)?;
        }
    }

    Ok(())
}
