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

use snafu::OptionExt;

use crate::{config::Config, error::InvalidConfigSnafu, Result};

#[derive(Debug, Clone)]
pub struct HeaderSource {
    pub content: String,
}

impl HeaderSource {
    pub fn from_config(config: &Config) -> Result<Self> {
        // 1. inline_header takes priority.
        if let Some(content) = config.inline_header.as_ref().cloned() {
            return Ok(HeaderSource { content });
        }

        // 2. Then, header_path tries to load from base_dir.
        let header_path = config.header_path.as_ref().context(InvalidConfigSnafu {
            message: "no header source found (both inline_header and header_path are None)",
        })?;
        let path = {
            let mut path = config.base_dir.clone();
            path.push(header_path);
            path
        };
        if let Ok(content) = std::fs::read_to_string(path) {
            return Ok(HeaderSource { content });
        }

        // 3. Finally, fallback to try bundled headers.
        bundled_headers(header_path).context(InvalidConfigSnafu {
            message: format!("no header source found (header_path is invalid: {header_path})"),
        })
    }
}

macro_rules! match_bundled_headers {
    ($name:expr, $($file:expr),*) => {
        match $name {
            $(
                $file => Some(HeaderSource { content: include_str!($file).to_string() }),
            )*
            _ => None,
        }
    }
}

fn bundled_headers(name: &str) -> Option<HeaderSource> {
    match_bundled_headers!(name, "Apache-2.0.txt", "Apache-2.0-ASF.txt")
}
