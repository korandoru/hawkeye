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
//
// Copyright 2024 - 2024, tison <wander4096@gmail.com> and the HawkEye contributors
// SPDX-License-Identifier: Apache-2.0

use std::{
    collections::{hash_map::Entry, HashMap},
    convert::Infallible,
    path::Path,
};

use gix::Repository;
use snafu::IntoError;
use tracing::info;

use crate::{
    config,
    config::FeatureGate,
    error::{GixDiscoverOpSnafu, InvalidConfigSnafu},
    Result,
};

#[derive(Debug, Clone)]
pub struct GitContext {
    pub repo: Option<Repository>,
    pub config: config::Git,
}

pub fn discover(basedir: &Path, config: config::Git) -> Result<GitContext> {
    let feature = resolve_features(&config);

    if feature.is_disable() {
        return Ok(GitContext { repo: None, config });
    }

    match gix::discover(basedir) {
        Ok(repo) => match repo.worktree() {
            None => {
                let message = "bare repository detected";
                if feature.is_auto() {
                    info!(?config, "git config is resolved to disabled; {message}");
                    Ok(GitContext { repo: None, config })
                } else {
                    InvalidConfigSnafu { message }.fail()
                }
            }
            Some(_) => {
                info!("git config is resolved to enabled");
                Ok(GitContext {
                    repo: Some(repo),
                    config,
                })
            }
        },
        Err(err) => {
            if feature.is_auto() {
                info!(?err, ?config, "git config is resolved to disabled");
                Ok(GitContext { repo: None, config })
            } else {
                Err(GixDiscoverOpSnafu {}.into_error(Box::new(err)))
            }
        }
    }
}

fn resolve_features(config: &config::Git) -> FeatureGate {
    let features = [config.attrs, config.ignore];
    for feature in features.iter() {
        if feature.is_enable() {
            return FeatureGate::Enable;
        }
    }
    for feature in features.iter() {
        if feature.is_auto() {
            return FeatureGate::Auto;
        }
    }
    FeatureGate::Disable
}

#[derive(Debug)]
pub struct GitFileAttrs {
    pub created_time: gix::date::Time,
    pub modified_time: gix::date::Time,
}

pub fn resolve_file_attrs(repo: &Repository) -> anyhow::Result<HashMap<String, GitFileAttrs>> {
    let mut attrs = HashMap::new();

    let workdir = repo.work_dir().expect("workdir cannot be absent");
    let workdir = workdir.canonicalize()?;

    let mode = gix::diff::blob::pipeline::Mode::ToGit;
    let mut cache = repo.diff_resource_cache(mode, Default::default())?;

    let head = repo.head_commit()?;
    let mut prev_commit = head.clone();

    for info in head.ancestors().all()? {
        let info = info?;
        let this_commit = info.object()?;
        let tree = this_commit.tree()?;
        let mut changes = tree.changes()?;
        changes.track_path().for_each_to_obtain_tree_with_cache(
            &prev_commit.tree()?,
            &mut cache,
            |change| {
                let filepath = workdir.join(change.location.to_string());
                let filepath = filepath.display().to_string();

                let time = this_commit.time().expect("commit always has time");
                match attrs.entry(filepath) {
                    Entry::Occupied(mut ent) => {
                        let attrs: &GitFileAttrs = ent.get();
                        ent.insert(GitFileAttrs {
                            created_time: time.min(attrs.created_time),
                            modified_time: time.max(attrs.modified_time),
                        });
                    }
                    Entry::Vacant(ent) => {
                        ent.insert(GitFileAttrs {
                            created_time: time,
                            modified_time: time,
                        });
                    }
                }

                Ok::<_, Infallible>(Default::default())
            },
        )?;
        prev_commit = this_commit;
        cache.clear_resource_cache();
    }

    Ok(attrs)
}
