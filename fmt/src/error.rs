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

use snafu::Snafu;

#[derive(Snafu, Debug)]
#[snafu(visibility(pub))]
pub enum Error {
    #[snafu(display("select with ignore failed: {}", source))]
    SelectWithIgnore {
        #[snafu(source)]
        source: ignore::Error,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("select files failed: {}", message))]
    SelectFiles {
        message: String,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("cannot to create document {}: {}", path, source))]
    CreateDocument {
        path: String,
        #[snafu(source)]
        source: std::io::Error,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("cannot to save document {}: {}", path, source))]
    SaveDocument {
        path: String,
        #[snafu(source)]
        source: std::io::Error,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("cannot try to matching header: {}", source))]
    TryMatchHeader {
        #[snafu(source)]
        source: std::io::Error,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("cannot load config {}: {}", name, source))]
    LoadConfig {
        name: String,
        #[snafu(source)]
        source: std::io::Error,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("cannot parse {}: {}", name, source))]
    Deserialize {
        name: String,
        #[snafu(source)]
        source: Box<toml::de::Error>,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("malformed regex {}: {}", payload, source))]
    MalformedRegex {
        payload: String,
        #[snafu(source)]
        source: regex::Error,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("empty regex in header {}", header))]
    EmptyRegex {
        header: String,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("invalid config: {}", message))]
    InvalidConfig {
        message: String,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("cannot open git repository with gix: {}", source))]
    GixOpenOp {
        #[snafu(source)]
        source: Box<gix::open::Error>,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("cannot create gix exclude stack: {}", source))]
    GixExcludeOp {
        #[snafu(source)]
        source: Box<gix::worktree::excludes::Error>,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("cannot check gix exclude: {}", source))]
    GixCheckExcludeOp {
        #[snafu(source)]
        source: std::io::Error,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("path not found {}", path))]
    GixPathNotFount {
        path: String,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("cannot resolve absolute path {}: {}", path, source))]
    ResolveAbsolutePath {
        path: String,
        #[snafu(source)]
        source: std::io::Error,
        #[snafu(implicit)]
        loc: snafu::Location,
    },

    #[snafu(display("cannot traverse directory: {}", source))]
    TraverseDir {
        #[snafu(source)]
        source: walkdir::Error,
        #[snafu(implicit)]
        loc: snafu::Location,
    },
}
