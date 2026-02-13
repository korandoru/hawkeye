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
use std::path::Path;
use std::path::PathBuf;

use exn::bail;
use exn::ErrorExt;
use exn::Result;
use exn::ResultExt;
use gix::diff::tree_with_rewrites::Change;
use gix::status::Item;
use gix::Repository;
use walkdir::WalkDir;

use crate::config;
use crate::config::FeatureGate;
use crate::error::Error;

#[derive(Debug, Clone)]
pub struct GitContext {
    pub repo: Option<Repository>,
    pub config: config::Git,
}

pub fn discover(basedir: &Path, config: config::Git) -> Result<GitContext, Error> {
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
                    bail!(Error::new(format!("invalid config: {message}")))
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
                Err(err
                    .raise()
                    .raise(Error::new("cannot discover git repository with gix")))
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
) -> Result<HashMap<PathBuf, GitFileAttrs>, Error> {
    let mut attrs = HashMap::new();

    if git_context.config.attrs.is_disable() {
        return Ok(attrs);
    }

    let repo = match git_context.repo {
        Some(repo) => repo,
        None => return Ok(attrs),
    };

    let current_username = match repo.committer() {
        Some(Ok(username)) => username.name.to_string(),
        _ => "<unknown>".to_string(),
    };

    let worktree = repo.worktree().expect("worktree cannot be absent");
    let workdir = repo.workdir().expect("workdir cannot be absent");
    let workdir = workdir.canonicalize().or_raise(|| {
        Error::new(format!(
            "cannot resolve absolute path: {}",
            workdir.display()
        ))
    })?;

    let mut excludes = worktree
        .excludes(None)
        .or_raise(|| Error::new("cannot create gix exclude stack"))?;

    let mut update_attrs = |rela_path: &Path, time: gix::date::Time, author: &str| {
        let filepath = workdir.join(rela_path);
        match attrs.entry(filepath) {
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
        }
    };

    let mut process_changes = |changes: Vec<Change>, time: gix::date::Time, author: &str| {
        for change in changes {
            match change {
                Change::Addition { location, .. } | Change::Modification { location, .. } => {
                    update_attrs(&gix::path::from_bstring(location), time, author);
                }
                Change::Deletion { .. } => continue, // skip deletion
                Change::Rewrite { .. } => unreachable!("rewrite has been disabled"),
            }
        }
    };

    let option = {
        let mut option = gix::diff::Options::default();
        option.track_path();
        option
    };

    let make_error = || Error::new("cannot resolve git file attributes");

    let head = repo.head_commit().or_raise(make_error)?;
    let mut next_commit = head.clone();

    for info in head.ancestors().all().or_raise(make_error)? {
        let info = info.or_raise(make_error)?;
        let this_commit = info.object().or_raise(make_error)?;
        let time = next_commit.time().or_raise(make_error)?;
        let author = next_commit.author().or_raise(make_error)?.name.to_string();

        let this_tree = this_commit.tree().or_raise(make_error)?;
        let next_tree = next_commit.tree().or_raise(make_error)?;

        let changes = repo
            .diff_tree_to_tree(Some(&this_tree), Some(&next_tree), Some(option))
            .or_raise(make_error)?;
        process_changes(changes, time, &author);

        next_commit = this_commit;
    }

    // process the root commit
    let time = next_commit.time().or_raise(make_error)?;
    let author = next_commit.author().or_raise(make_error)?.name.to_string();
    let next_tree = next_commit.tree().or_raise(make_error)?;
    let changes = repo
        .diff_tree_to_tree(None, Some(&next_tree), Some(option))
        .or_raise(make_error)?;
    process_changes(changes, time, &author);

    // process dirty working tree
    let status_platform = repo.status(gix::progress::Discard).or_raise(make_error)?;
    let status_iter = status_platform.into_iter(None).or_raise(make_error)?;
    let now = gix::date::Time::now_local_or_utc();
    for item in status_iter {
        match item.or_raise(|| Error::new("failed to check git status item"))? {
            Item::IndexWorktree(item) => match item {
                gix::status::index_worktree::Item::Modification { rela_path, .. } => {
                    let rela_path = gix::path::from_bstring(rela_path);
                    update_attrs(&rela_path, now, current_username.as_str());
                }
                gix::status::index_worktree::Item::DirectoryContents { entry, .. } => {
                    if entry.disk_kind.is_some_and(|k| k.is_dir()) {
                        let dirpath = workdir
                            .join(gix::path::from_bstr(&entry.rela_path))
                            .canonicalize()
                            .or_raise(|| {
                                Error::new(format!(
                                    "cannot resolve absolute path: {}",
                                    &entry.rela_path
                                ))
                            })?;

                        let mut it = WalkDir::new(dirpath).follow_links(false).into_iter();
                        while let Some(entry) = it.next() {
                            let entry =
                                entry.or_raise(|| Error::new("cannot traverse directory"))?;
                            let path = entry.path();
                            let file_type = entry.file_type();
                            if !file_type.is_file() && !file_type.is_dir() {
                                log::debug!(file_type:?; "skip file: {path:?}");
                                continue;
                            }

                            let rela_path = path
                                .strip_prefix(&workdir)
                                .expect("git repository encloses iteration");
                            let mode = Some(if file_type.is_dir() {
                                gix::index::entry::Mode::DIR
                            } else {
                                gix::index::entry::Mode::FILE
                            });
                            let platform = excludes
                                .at_path(rela_path, mode)
                                .or_raise(|| Error::new("cannot check gix exclude"))?;

                            if file_type.is_dir() {
                                if platform.is_excluded() {
                                    log::debug!(path:?, rela_path:?; "skip git ignored directory");
                                    it.skip_current_dir();
                                    continue;
                                }
                            } else if file_type.is_file() {
                                if platform.is_excluded() {
                                    log::debug!(path:?, rela_path:?; "skip git ignored file");
                                    continue;
                                }
                                update_attrs(rela_path, now, current_username.as_str());
                            }
                        }
                    } else {
                        let rela_path = gix::path::from_bstring(entry.rela_path);
                        update_attrs(&rela_path, now, current_username.as_str());
                    }
                }
                gix::status::index_worktree::Item::Rewrite { .. } => {
                    unreachable!("rewrite has been disabled")
                }
            },
            Item::TreeIndex(item) => {
                let rela_path = gix::path::from_bstr(item.location());
                update_attrs(&rela_path, now, current_username.as_str());
            }
        }
    }

    Ok(attrs)
}
