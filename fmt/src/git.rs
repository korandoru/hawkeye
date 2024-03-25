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

use std::path::Path;

use gix::Repository;
use snafu::IntoError;
use tracing::info;

use crate::{
    config,
    error::{GixDiscoverOpSnafu, InvalidConfigSnafu},
    Result,
};

pub fn discover(basedir: &Path, config: config::Git) -> Result<Option<Repository>> {
    if config.ignore.is_disable() {
        return Ok(None);
    }

    let is_auto = config.ignore.is_auto();
    match gix::discover(basedir) {
        Ok(repo) => match repo.worktree() {
            None => {
                let message = "bare repository detected";
                if is_auto {
                    info!("git.ignore=auto is resolved to fallback; {message}");
                    Ok(None)
                } else {
                    InvalidConfigSnafu { message }.fail()
                }
            }
            Some(_) => {
                info!("git.ignore=auto is resolved to enabled");
                Ok(Some(repo))
            }
        },
        Err(err) => {
            if is_auto {
                info!(?err, "git.ignore=auto is resolved to disabled");
                Ok(None)
            } else {
                Err(GixDiscoverOpSnafu {}.into_error(Box::new(err)))
            }
        }
    }
}
