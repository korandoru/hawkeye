use std::{fs, path::PathBuf};

use snafu::{ensure, OptionExt, ResultExt};

use crate::{
    config::Config,
    document::{factory::DocumentFactory, model::default_mapping},
    error::{DeserializeSnafu, InvalidConfigSnafu, LoadConfigSnafu, TryMatchHeaderSnafu},
    header::{matcher::HeaderMatcher, model::default_headers},
    license::HeaderSource,
    selection::Selection,
    Result,
};

#[derive(Debug, Clone)]
pub enum CheckResult {
    Matched(PathBuf),
    NotMatched(PathBuf),
    Unsupported(PathBuf),
}

pub fn do_check(run_config: PathBuf) -> Result<()> {
    let config = fs::read_to_string(&run_config).with_context(|_| LoadConfigSnafu {
        name: run_config.display().to_string(),
    })?;

    let config = toml::from_str::<Config>(&config).with_context(|_| DeserializeSnafu {
        name: run_config.display().to_string(),
    })?;

    println!("{:#?}", check_license_header(config).unwrap());

    Ok(())
}

pub fn check_license_header(config: Config) -> Result<Vec<CheckResult>> {
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
            basedir.clone(),
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

    let document_factory = DocumentFactory::new(
        mapping,
        definitions,
        config.properties,
        basedir,
        config.keywords,
    );

    let mut result = vec![];
    for file in selected_files {
        let document = document_factory.create_document(&file)?;
        if document.is_unsupported() {
            result.push(CheckResult::Unsupported(file));
        } else if document
            .header_matched(&header_matcher, config.strict_check)
            .context(TryMatchHeaderSnafu)?
        {
            result.push(CheckResult::Matched(file));
        } else {
            result.push(CheckResult::NotMatched(file));
        }
    }
    Ok(result)
}
