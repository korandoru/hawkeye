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

use snafu::ResultExt;

use crate::{
    config::Config,
    error::{InvalidConfigSnafu, LoadConfigSnafu},
    Result,
};

#[derive(Debug, Clone)]
pub struct HeaderSource {
    pub content: String,
}

impl HeaderSource {
    pub fn from_config(config: &Config) -> Result<Self> {
        if let Some(inline_header) = &config.inline_header {
            return Ok(HeaderSource {
                content: inline_header.clone(),
            });
        }

        if let Some(header_path) = &config.header_path {
            if let Some(content) = bundled_headers(header_path) {
                return Ok(content);
            }

            let mut path = config.base_dir.clone();
            path.push(header_path);
            let content = std::fs::read_to_string(header_path)
                .context(LoadConfigSnafu { name: header_path })?;
            return Ok(HeaderSource { content });
        }

        InvalidConfigSnafu {
            message: "no header source found in config",
        }
        .fail()
    }
}

pub fn bundled_headers(name: &str) -> Option<HeaderSource> {
    match name {
        "Apache-2.0.txt" => Some(HeaderSource {
            content: include_str!("Apache-2.0.txt").to_string(),
        }),
        "Apache-2.0-ASF.txt" => Some(HeaderSource {
            content: include_str!("Apache-2.0-ASF.txt").to_string(),
        }),
        _ => None,
    }
}
