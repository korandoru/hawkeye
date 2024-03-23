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

use std::{fs, path::PathBuf};

use snafu::{ensure, OptionExt, ResultExt};

use crate::{
    config::Config,
    document::{factory::DocumentFactory, model::default_mapping, Document},
    error::{DeserializeSnafu, InvalidConfigSnafu, LoadConfigSnafu, TryMatchHeaderSnafu},
    header::{matcher::HeaderMatcher, model::default_headers},
    license::HeaderSource,
    selection::Selection,
    Result,
};

#[allow(clippy::type_complexity)]
pub fn check_license_header(
    run_config: PathBuf,
) -> Result<(
    HeaderMatcher,
    // unknown
    Vec<Document>,
    // matched
    Vec<Document>,
    // not matched
    Vec<Document>,
)> {
    let config = fs::read_to_string(&run_config).with_context(|_| LoadConfigSnafu {
        name: run_config.display().to_string(),
    })?;
    let config = toml::from_str::<Config>(&config).with_context(|_| DeserializeSnafu {
        name: run_config.display().to_string(),
    })?;

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
        let header_source = HeaderSource::from_config(&config).context(InvalidConfigSnafu {
            message: "no header source found in config",
        })?;
        HeaderMatcher::new(header_source.content)
    };

    let selected_files = {
        let selection = Selection::new(
            basedir,
            &config.includes,
            &config.excludes,
            config.use_default_excludes,
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

    let definitions = default_headers()?;

    let document_factory =
        DocumentFactory::new(mapping, definitions, config.properties, config.keywords);

    let mut unknown = vec![];
    let mut matched = vec![];
    let mut not_matched = vec![];
    for file in selected_files {
        let document = document_factory.create_document(&file)?;
        if document.is_unsupported() {
            unknown.push(document);
        } else if document
            .header_matched(&header_matcher, config.strict_check)
            .context(TryMatchHeaderSnafu)?
        {
            matched.push(document);
        } else {
            not_matched.push(document);
        }
    }
    Ok((header_matcher, unknown, matched, not_matched))
}
