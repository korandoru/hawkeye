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

use std::collections::hash_map::Entry;
use std::collections::BTreeSet;
use std::collections::HashMap;
use std::convert::Infallible;
use std::path::Path;
use std::path::PathBuf;

use anyhow::bail;
use anyhow::Context;
use gix::Repository;

use crate::config;
use crate::config::FeatureGate;

#[derive(Debug, Clone)]
pub struct GitContext {
    pub repo: Option<Repository>,
    pub config: config::Git,
}

pub fn discover(basedir: &Path, config: config::Git) -> anyhow::Result<GitContext> {
    let feature = resolve_features(&config);

    if feature.is_disable() {
        return Ok(GitContext { repo: None, config });
    }

    match gix::discover(basedir) {
        Ok(repo) => match repo.worktree() {
            None => {
                let message = "bare repository detected";
                if feature.is_auto() {
                    log::info!(config:?; "git config is resolved to disabled; {message}");
                    Ok(GitContext { repo: None, config })
                } else {
                    bail!("invalid config: {}", message);
                }
            }
            Some(_) => {
                log::info!("git config is resolved to enabled");
                Ok(GitContext {
                    repo: Some(repo),
                    config,
                })
            }
        },
        Err(err) => {
            if feature.is_auto() {
                log::info!(err:?, config:?; "git config is resolved to disabled");
                Ok(GitContext { repo: None, config })
            } else {
                Err(err).context("cannot discover git repository with gix")
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
    pub authors: BTreeSet<String>,
}

pub fn resolve_file_attrs(
    git_context: GitContext,
) -> anyhow::Result<HashMap<PathBuf, GitFileAttrs>> {
    let mut attrs = HashMap::new();

    if git_context.config.attrs.is_disable() {
        return Ok(attrs);
    }

    let repo = match git_context.repo {
        Some(repo) => repo,
        None => return Ok(attrs),
    };

    let mut do_insert_attrs =
        |filepath: PathBuf, time: gix::date::Time, author: &str| match attrs.entry(filepath) {
            Entry::Occupied(mut ent) => {
                let attrs: &mut GitFileAttrs = ent.get_mut();
                attrs.created_time = time.min(attrs.created_time);
                attrs.modified_time = time.max(attrs.modified_time);
                attrs.authors.insert(author.to_string());
            }
            Entry::Vacant(ent) => {
                ent.insert(GitFileAttrs {
                    created_time: time,
                    modified_time: time,
                    authors: {
                        let mut authors = BTreeSet::new();
                        authors.insert(author.to_string());
                        authors
                    },
                });
            }
        };

    let workdir = repo.work_dir().expect("workdir cannot be absent");
    let workdir = workdir.canonicalize()?;

    let mode = gix::diff::blob::pipeline::Mode::ToGit;
    let mut cache = repo.diff_resource_cache(mode, Default::default())?;

    let head = repo.head_commit()?;
    let mut next_commit = head.clone();

    for info in head.ancestors().all()? {
        let info = info?;
        let this_commit = info.object()?;
        let time = next_commit.time()?;
        let author = next_commit.author()?.name.to_string();

        let tree = next_commit.tree()?;
        let mut changes = tree.changes()?;
        changes
            .options(|opts| {
                opts.track_path();
            })
            .for_each_to_obtain_tree_with_cache(&this_commit.tree()?, &mut cache, |change| {
                let filepath = gix::path::from_bstr(change.location());
                let filepath = workdir.join(filepath);
                do_insert_attrs(filepath, time, author.as_str());
                Ok::<_, Infallible>(Default::default())
            })?;
        next_commit = this_commit;
        cache.clear_resource_cache();
    }

    // process the root commit
    let time = next_commit.time()?;
    let author = next_commit.author()?.name.to_string();
    let tree = next_commit.tree()?;
    for ent in tree.iter() {
        let ent = ent?;
        let filepath = gix::path::from_bstr(ent.filename());
        let filepath = workdir.join(filepath);
        do_insert_attrs(filepath, time, author.as_str());
    }

    Ok(attrs)
}
