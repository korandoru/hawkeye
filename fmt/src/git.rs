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
use snafu::{IntoError, OptionExt, ResultExt};
use tracing::info;

use crate::{
    config,
    error::{
        GixCheckExcludeOpSnafu, GixExcludeOpSnafu, GixOpenOpSnafu, GixPathNotFountSnafu,
        InvalidConfigSnafu, ResolveAbsolutePathSnafu,
    },
    Result,
};

pub struct GitHelper {
    repo: Repository,
}

impl GitHelper {
    pub fn create(basedir: &Path, config: config::Git) -> Result<Option<GitHelper>> {
        if config.ignore.is_disable() {
            return Ok(None);
        }

        let is_auto = config.ignore.is_auto();
        match gix::open(basedir) {
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
                    Ok(Some(GitHelper { repo }))
                }
            },
            Err(err) => {
                if is_auto {
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
        let workdir = self
            .repo
            .work_dir()
            .context(GixPathNotFountSnafu { path: "workdir" })?;
        let workdir = workdir.canonicalize().context(ResolveAbsolutePathSnafu {
            path: workdir.display().to_string(),
        })?;
        let at_path = pathdiff::diff_paths(path, workdir)
            .context(GixPathNotFountSnafu { path: "<relative>" })?;
        let worktree = self
            .repo
            .worktree()
            .context(GixPathNotFountSnafu { path: "worktree" })?;
        let mut attrs = worktree
            .excludes(None)
            .map_err(Box::new)
            .context(GixExcludeOpSnafu)?;
        let platform = attrs
            .at_path(at_path, Some(is_dir))
            .context(GixCheckExcludeOpSnafu)?;
        Ok(platform.is_excluded())
    }
}
