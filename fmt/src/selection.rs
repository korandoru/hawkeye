use std::path::PathBuf;

use ignore::{overrides::OverrideBuilder, DirEntry, Error};
use snafu::{ensure, ResultExt};
use tracing::debug;

use crate::{
    error::{SelectionSnafu, SelectionWalkerSnafu},
    Result,
};

pub struct Selection {
    basedir: PathBuf,
    includes: Vec<String>,
    excludes: Vec<String>,
}

impl Selection {
    pub fn new(
        basedir: PathBuf,
        includes: Vec<String>,
        excludes: Vec<String>,
        use_default_excludes: bool,
    ) -> Selection {
        let includes = if includes.is_empty() {
            INCLUDES.iter().map(|s| s.to_string()).collect()
        } else {
            includes
        };

        let used_default_excludes = if use_default_excludes {
            EXCLUDES.iter().map(|s| s.to_string()).collect()
        } else {
            vec![]
        };
        let excludes = [used_default_excludes, excludes].concat();

        Selection {
            basedir,
            includes,
            excludes,
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
                .extract_if(|pat| pat.starts_with("!"))
                .map(|mut pat| {
                    pat.remove(0);
                    pat
                })
                .collect::<Vec<_>>();
            (excludes, reverse_excludes)
        };

        let includes = self.includes;
        ensure!(
            includes.iter().all(|pat| !pat.starts_with("!")),
            SelectionSnafu {
                msg: format!("reverse pattern is not allowed for includes: {includes:?}"),
            },
        );

        let mut result = vec![];
        let walker = ignore::WalkBuilder::new(&self.basedir)
            .standard_filters(false) // [TODO] respect git.ignore option
            .hidden(false) // check hidden files
            .follow_links(true) // proper path name
            .overrides({
                let mut builder = OverrideBuilder::new(&self.basedir);
                for pat in includes.iter() {
                    builder.add(pat).context(SelectionWalkerSnafu)?;
                }
                for pat in excludes.iter() {
                    let pat = format!("!{pat}");
                    builder.add(pat.as_str()).context(SelectionWalkerSnafu)?;
                }
                for pat in reverse_excludes.iter() {
                    builder.add(pat).context(SelectionWalkerSnafu)?;
                }
                builder.build().context(SelectionWalkerSnafu)?
            })
            .build();

        for mat in walker {
            let mat = mat.context(SelectionWalkerSnafu)?;
            if let Some(filetype) = mat.file_type() {
                if filetype.is_file() {
                    result.push(mat.into_path());
                }
            }
        }
        Ok(result)
    }
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
