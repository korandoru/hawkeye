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

use snafu::ResultExt;
use tracing::info;

use crate::{
    config,
    error::{GixOpenOpSnafu, InvalidConfigSnafu, ResolveAbsolutePathSnafu},
    Result,
};

pub struct GitHelper {
    repo: gix::Repository,
}

impl GitHelper {
    pub fn create(basedir: &Path, config: config::Git) -> Result<Option<GitHelper>> {
        if config.ignore.is_disable() {
            return Ok(None);
        }

        match gix::open(basedir) {
            Ok(repo) => {
                if repo.worktree().is_none() {
                    let message = "bare repository detected";
                    if config.ignore.is_auto() {
                        info!("git.ignore=auto is resolved to fallback; {message}");
                        Ok(None)
                    } else {
                        InvalidConfigSnafu { message }.fail()
                    }
                } else {
                    info!("git.ignore=auto is resolved to enabled");
                    Ok(Some(GitHelper { repo }))
                }
            }
            Err(err) => {
                if config.ignore.is_auto() {
                    info!(?err, "git.ignore=auto is resolved to disabled");
                    Ok(None)
                } else {
                    Err(err).context(GixOpenOpSnafu)
                }
            }
        }
    }

    pub fn ignored(&self, path: &Path) -> Result<bool> {
        let path = path.canonicalize().context(ResolveAbsolutePathSnafu {
            path: path.display().to_string(),
        })?;
        todo!("how to get attributes and check ignore")
    }
}
