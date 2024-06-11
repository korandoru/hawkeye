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
    borrow::Cow,
    collections::{HashMap, HashSet},
    convert::Infallible,
    path::Path,
};

use snafu::ResultExt;
use time::format_description;

use crate::{
    config::Mapping,
    document::Document,
    error::{CreateDocumentSnafu, GitFileAttrsSnafu},
    git::GitContext,
    header::model::HeaderDef,
    Result,
};

pub struct DocumentFactory {
    mapping: HashSet<Mapping>,
    definitions: HashMap<String, HeaderDef>,
    properties: HashMap<String, String>,

    keywords: Vec<String>,
    git_context: GitContext,
}

impl DocumentFactory {
    pub fn new(
        mapping: HashSet<Mapping>,
        definitions: HashMap<String, HeaderDef>,
        properties: HashMap<String, String>,
        keywords: Vec<String>,
        git_context: GitContext,
    ) -> Self {
        Self {
            mapping,
            definitions,
            properties,
            keywords,
            git_context,
        }
    }

    pub fn create_document(&self, filepath: &Path) -> Result<Option<Document>> {
        let lower_file_name = filepath
            .file_name()
            .map(|n| n.to_string_lossy().to_lowercase())
            .unwrap_or_default();
        let header_type = self
            .mapping
            .iter()
            .find_map(|m| m.header_type(&lower_file_name))
            .unwrap_or_else(|| "unknown".to_string())
            .to_lowercase();
        let header_def = self
            .definitions
            .get(&header_type)
            .ok_or_else(|| std::io::Error::other(format!("header type {header_type} not found")))
            .context(CreateDocumentSnafu {
                path: filepath.display().to_string(),
            })?;

        let mut properties = self.properties.clone();

        let filename = filepath
            .file_name()
            .map(|s| s.to_string_lossy())
            .unwrap_or_else(|| Cow::Borrowed("<unknown>"))
            .to_string();
        properties.insert("hawkeye.core.filename".to_string(), filename);

        let year_formatter = format_description::parse("[year]").expect("cannot parse format");
        let git_file_attrs =
            resolve_git_file_attrs(&self.git_context, filepath).context(GitFileAttrsSnafu)?;
        if let Some(time) = git_file_attrs.created_time {
            properties.insert(
                "hawkeye.git.fileCreatedYear".to_string(),
                time.format(year_formatter.as_slice()),
            );
        }
        if let Some(time) = git_file_attrs.modified_time {
            properties.insert(
                "hawkeye.git.fileModifiedYear".to_string(),
                time.format(year_formatter.as_slice()),
            );
        }

        Document::new(
            filepath.to_path_buf(),
            header_def.clone(),
            &self.keywords,
            properties,
        )
    }
}

#[derive(Debug)]
struct GitFileAttrs {
    created_time: Option<gix::date::Time>,
    modified_time: Option<gix::date::Time>,
}

fn resolve_git_file_attrs(git_context: &GitContext, path: &Path) -> anyhow::Result<GitFileAttrs> {
    let mut attrs = GitFileAttrs {
        created_time: None,
        modified_time: None,
    };

    if let Some(ref repo) = git_context.repo {
        let workdir = repo.work_dir().expect("workdir cannot be absent");
        let workdir = workdir.canonicalize()?;
        let rela_path = path.strip_prefix(&workdir)?;
        let location = rela_path.display().to_string();

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
                    if change.location == location {
                        let time = this_commit.time().expect("commit always has time");
                        attrs.created_time = match attrs.created_time {
                            None => Some(time),
                            Some(t) => Some(t.min(time)),
                        };
                        attrs.modified_time = match attrs.modified_time {
                            None => Some(time),
                            Some(t) => Some(t.max(time)),
                        };
                    }
                    Ok::<_, Infallible>(Default::default())
                },
            )?;
            prev_commit = this_commit;
        }
    }

    Ok(attrs)
}
