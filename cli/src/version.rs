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

use shadow_rs::shadow;

shadow!(build);

#[derive(Clone, Debug, PartialEq)]
pub struct BuildInfo {
    pub branch: &'static str,
    pub commit: &'static str,
    pub commit_short: &'static str,
    pub clean: bool,
    pub description: &'static str,
    pub source_time: &'static str,
    pub build_time: &'static str,
    pub rustc: &'static str,
    pub target: &'static str,
    pub version: &'static str,
}

pub const fn build_info() -> BuildInfo {
    BuildInfo {
        branch: build::BRANCH,
        commit: build::COMMIT_HASH,
        commit_short: build::SHORT_COMMIT,
        clean: build::GIT_CLEAN,
        description: build::PKG_DESCRIPTION,
        source_time: env!("SOURCE_TIMESTAMP"),
        build_time: env!("BUILD_TIMESTAMP"),
        rustc: build::RUST_VERSION,
        target: build::BUILD_TARGET,
        version: build::PKG_VERSION,
    }
}

pub const fn version() -> &'static str {
    const BUILD_INFO: BuildInfo = build_info();

    const_format::formatcp!(
        "\nversion: {}\nbranch: {}\ncommit: {}\nclean: {}\nsource_time: {}\nbuild_time: {}\nrustc: {}\ntarget: {}",
        BUILD_INFO.version,
        BUILD_INFO.branch,
        BUILD_INFO.commit,
        BUILD_INFO.clean,
        BUILD_INFO.source_time,
        BUILD_INFO.build_time,
        BUILD_INFO.rustc,
        BUILD_INFO.target,
    )
}
