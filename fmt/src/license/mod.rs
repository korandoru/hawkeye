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

use crate::config::Config;

#[derive(Debug, Clone)]
pub struct HeaderSource {
    pub content: String,
}

impl HeaderSource {
    pub fn from_config(config: &Config) -> Option<Self> {
        config
            .inline_header
            .as_ref()
            .map(|content| HeaderSource {
                content: content.clone(),
            })
            .or_else(|| config.header_path.as_deref().and_then(bundled_headers))
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
