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

use std::path::PathBuf;

use ignore::overrides::OverrideBuilder;
use snafu::{ensure, ResultExt};
use tracing::debug;
use walkdir::WalkDir;

use crate::{
    config,
    error::{SelectFilesSnafu, SelectWithIgnoreSnafu, TraverseDirSnafu},
    git::GitHelper,
    Result,
};

pub struct Selection {
    basedir: PathBuf,
    includes: Vec<String>,
    excludes: Vec<String>,

    git: config::Git,
}

impl Selection {
    pub fn new(
        basedir: PathBuf,
        includes: &[String],
        excludes: &[String],
        use_default_excludes: bool,
        git: config::Git,
    ) -> Selection {
        let includes = if includes.is_empty() {
            INCLUDES.iter().map(|s| s.to_string()).collect()
        } else {
            includes.to_vec()
        };

        let used_default_excludes = if use_default_excludes {
            EXCLUDES.iter().map(|s| s.to_string()).collect()
        } else {
            vec![]
        };
        let excludes = [used_default_excludes, excludes.to_vec()].concat();

        Selection {
            basedir,
            includes,
            excludes,
            git,
        }
    }

    pub fn select(self) -> Result<Vec<PathBuf>> {
        debug!(
            "Selecting files with baseDir: {}, included: {:?}, excluded: {:?}",
            self.basedir.display(),
            self.includes,
            self.excludes,
        );

        let (excludes, reverse_excludes) = {
            let mut excludes = self.excludes;
            let reverse_excludes = excludes
                .extract_if(|pat| pat.starts_with('!'))
                .map(|mut pat| {
                    pat.remove(0);
                    pat
                })
                .collect::<Vec<_>>();
            (excludes, reverse_excludes)
        };

        let includes = self.includes;
        ensure!(
            includes.iter().all(|pat| !pat.starts_with('!')),
            SelectFilesSnafu {
                message: format!("reverse pattern is not allowed for includes: {includes:?}"),
            },
        );

        let result = match GitHelper::create(&self.basedir, self.git)? {
            None => select_files_with_ignore(
                &self.basedir,
                &includes,
                &excludes,
                &reverse_excludes,
                self.git.ignore.is_auto(),
            )?,
            Some(git_helper) => select_files_with_git(
                &self.basedir,
                &includes,
                &excludes,
                &reverse_excludes,
                git_helper,
            )?,
        };

        debug!("selected files: {:?} (count: {})", result, result.len());
        Ok(result)
    }
}

fn select_files_with_ignore(
    basedir: &PathBuf,
    includes: &[String],
    excludes: &[String],
    reverse_excludes: &[String],
    turn_on_git_ignore: bool,
) -> Result<Vec<PathBuf>> {
    debug!("Selecting files with ignore crate");
    let mut result = vec![];

    let walker = ignore::WalkBuilder::new(basedir)
        .ignore(false) // do not use .ignore file
        .hidden(false) // check hidden files
        .follow_links(true) // proper path name
        .parents(turn_on_git_ignore)
        .git_exclude(turn_on_git_ignore)
        .git_global(turn_on_git_ignore)
        .git_ignore(turn_on_git_ignore)
        .overrides({
            let mut builder = OverrideBuilder::new(basedir);
            for pat in includes.iter() {
                builder.add(pat).context(SelectWithIgnoreSnafu)?;
            }
            for pat in excludes.iter() {
                let pat = format!("!{pat}");
                builder.add(pat.as_str()).context(SelectWithIgnoreSnafu)?;
            }
            for pat in reverse_excludes.iter() {
                builder.add(pat).context(SelectWithIgnoreSnafu)?;
            }
            builder.build().context(SelectWithIgnoreSnafu)?
        })
        .build();

    for mat in walker {
        let mat = mat.context(SelectWithIgnoreSnafu)?;
        if mat.file_type().map(|ft| ft.is_file()).unwrap_or(false) {
            result.push(mat.into_path())
        }
    }

    Ok(result)
}

fn select_files_with_git(
    basedir: &PathBuf,
    includes: &[String],
    excludes: &[String],
    reverse_excludes: &[String],
    git_helper: GitHelper,
) -> Result<Vec<PathBuf>> {
    debug!("Selecting files with git helper");
    let mut result = vec![];

    let matcher = {
        let mut builder = OverrideBuilder::new(basedir);
        for pat in includes.iter() {
            builder.add(pat).context(SelectWithIgnoreSnafu)?;
        }
        for pat in excludes.iter() {
            let pat = format!("!{pat}");
            builder.add(pat.as_str()).context(SelectWithIgnoreSnafu)?;
        }
        for pat in reverse_excludes.iter() {
            builder.add(pat).context(SelectWithIgnoreSnafu)?;
        }
        builder.build().context(SelectWithIgnoreSnafu)?
    };

    let mut it = WalkDir::new(basedir).into_iter();
    while let Some(entry) = it.next() {
        let entry = entry.context(TraverseDirSnafu)?;
        let path = entry.path();
        let file_type = entry.file_type();
        if file_type.is_symlink() {
            debug!("skip symlink: {:?}", path);
        } else if file_type.is_dir() {
            if git_helper.ignored(path, true)? {
                debug!("skip git ignored directory: {:?}", path);
                it.skip_current_dir();
                continue;
            }
            if matcher.matched(path, file_type.is_dir()).is_ignore() {
                debug!("skip glob ignored directory: {:?}", path);
                it.skip_current_dir();
                continue;
            }
        } else if file_type.is_file() {
            if git_helper.ignored(path, false)? {
                debug!("skip git ignored file: {:?}", path);
                continue;
            }
            if !matcher.matched(path, file_type.is_dir()).is_whitelist() {
                debug!("skip glob ignored file: {:?}", path);
                continue;
            }
            result.push(path.to_path_buf());
        } else {
            debug!("skip unknown file type ({:?}): {:?}", file_type, path);
        }
    }

    Ok(result)
}

pub const INCLUDES: [&str; 1] = ["**"];
pub const EXCLUDES: [&str; 140] = [
    // Miscellaneous typical temporary files
    "**/*~",
    "**/#*#",
    "**/.#*",
    "**/%*%",
    "**/._*",
    "**/.repository/**",
    "**/*.lck",
    // CVS
    "**/CVS",
    "**/CVS/**",
    "**/.cvsignore",
    // RCS
    "**/RCS",
    "**/RCS/**",
    // SCCS
    "**/SCCS",
    "**/SCCS/**",
    // Visual SourceSafe
    "**/vssver.scc",
    // Subversion
    "**/.svn",
    "**/.svn/**",
    // Arch
    "**/.arch-ids",
    "**/.arch-ids/**",
    // Bazaar
    "**/.bzr",
    "**/.bzr/**",
    // SurroundSCM
    "**/.MySCMServerInfo",
    // Mac
    "**/.DS_Store",
    // Docker
    ".dockerignore",
    // Serena Dimensions Version 10
    "**/.metadata",
    "**/.metadata/**",
    // Mercurial
    "**/.hg",
    "**/.hg/**",
    "**/.hgignore",
    // git
    "**/.git",
    "**/.git/**",
    "**/.gitattributes",
    "**/.gitignore",
    "**/.gitkeep",
    "**/.gitmodules",
    // BitKeeper
    "**/BitKeeper",
    "**/BitKeeper/**",
    "**/ChangeSet",
    "**/ChangeSet/**",
    // darcs
    "**/_darcs",
    "**/_darcs/**",
    "**/.darcsrepo",
    "**/.darcsrepo/**",
    "**/-darcs-backup*",
    "**/.darcs-temp-mail",
    // maven project's temporary files
    "**/target/**",
    "**/test-output/**",
    "**/release.properties",
    "**/dependency-reduced-pom.xml",
    "**/release-pom.xml",
    "**/pom.xml.releaseBackup",
    "**/pom.xml.versionsBackup",
    // Node
    "**/node/**",
    "**/node_modules/**",
    // Yarn
    "**/.yarn/**",
    "**/yarn.lock",
    // pnpm
    "pnpm-lock.yaml",
    // Golang
    "**/go.sum",
    // Cargo
    "**/Cargo.lock",
    // code coverage tools
    "**/cobertura.ser",
    "**/.clover/**",
    "**/jacoco.exec",
    // eclipse project files
    "**/.classpath",
    "**/.project",
    "**/.settings/**",
    // IDEA projet files
    "**/*.iml",
    "**/*.ipr",
    "**/*.iws",
    "**/.idea/**",
    // Netbeans
    "**/nb-configuration.xml",
    // Hibernate Validator Annotation Processor
    "**/.factorypath",
    // descriptors
    "**/MANIFEST.MF",
    // License files
    "**/LICENSE",
    "**/LICENSE_HEADER",
    // binary files - images
    "**/*.jpg",
    "**/*.png",
    "**/*.gif",
    "**/*.ico",
    "**/*.bmp",
    "**/*.tiff",
    "**/*.tif",
    "**/*.cr2",
    "**/*.xcf",
    // binary files - programs
    "**/*.class",
    "**/*.exe",
    "**/*.dll",
    "**/*.so",
    // checksum files
    "**/*.md5",
    "**/*.sha1",
    "**/*.sha256",
    "**/*.sha512",
    // Security files
    "**/*.asc",
    "**/*.jks",
    "**/*.keytab",
    "**/*.lic",
    "**/*.p12",
    "**/*.pub",
    // binary files - archives
    "**/*.jar",
    "**/*.zip",
    "**/*.rar",
    "**/*.tar",
    "**/*.tar.gz",
    "**/*.tar.bz2",
    "**/*.gz",
    "**/*.7z",
    // ServiceLoader files
    "**/META-INF/services/**",
    // Markdown files
    "**/*.md",
    // Office documents
    "**/*.xls",
    "**/*.doc",
    "**/*.odt",
    "**/*.ods",
    "**/*.pdf",
    // Travis
    "**/.travis.yml",
    // AppVeyor
    "**/.appveyor.yml",
    "**/appveyor.yml",
    // CircleCI
    "**/.circleci",
    "**/.circleci/**",
    // SourceHut
    "**/.build.yml",
    // Maven 3.3+ configs
    "**/jvm.config",
    "**/maven.config",
    // Wrappers
    "**/gradlew",
    "**/gradlew.bat",
    "**/gradle-wrapper.properties",
    "**/mvnw",
    "**/mvnw.cmd",
    "**/maven-wrapper.properties",
    "**/MavenWrapperDownloader.java",
    // flash
    "**/*.swf",
    // json files
    "**/*.json",
    // fonts
    "**/*.svg",
    "**/*.eot",
    "**/*.otf",
    "**/*.ttf",
    "**/*.woff",
    "**/*.woff2",
    // logs
    "**/*.log",
    // office documents
    "**/*.xlsx",
    "**/*.docx",
    "**/*.ppt",
    "**/*.pptx",
];
