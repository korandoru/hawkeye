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

use gix::{hash::Kind, index::State, worktree::stack::state::ignore::Source, Repository};
use snafu::{IntoError, ResultExt};
use tracing::info;

use crate::{
    config,
    error::{
        GixCheckExcludeOpSnafu, GixExcludeOpSnafu, GixOpenOpSnafu, InvalidConfigSnafu,
        ResolveAbsolutePathSnafu,
    },
    Result,
};

pub struct GitHelper {
    repo: Repository,
    state: State,
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
                    let state = State::new(Kind::Sha1);
                    Ok(Some(GitHelper { repo, state }))
                }
            }
            Err(err) => {
                if config.ignore.is_auto() {
                    info!(?err, "git.ignore=auto is resolved to disabled");
                    Ok(None)
                } else {
                    Err(GixOpenOpSnafu {}.into_error(Box::new(err)))
                }
            }
        }
    }

    pub fn ignored(&self, path: &Path, is_dir: bool) -> Result<bool> {
        let path = path.canonicalize().context(ResolveAbsolutePathSnafu {
            path: path.display().to_string(),
        })?;
        let mut stack = self
            .repo
            .excludes(&self.state, None, Source::default())
            .map_err(Box::new)
            .context(GixExcludeOpSnafu)?;
        let result = stack
            .at_path(&path, Some(is_dir))
            .context(GixCheckExcludeOpSnafu)?;
        Ok(result.is_excluded())
    }
}
