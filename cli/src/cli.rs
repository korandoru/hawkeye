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

use std::path::{Path, PathBuf};

use clap::{Args, Parser};
use hawkeye_fmt::{
    document::Document,
    header::matcher::HeaderMatcher,
    processor::{check_license_header, Callback},
    Result,
};
use tracing::{error, info, warn};

#[derive(Parser)]
pub enum SubCommand {
    #[clap(name = "check", about = "check license header")]
    Check(CommandCheck),
    #[clap(name = "format", about = "format license header")]
    Format(CommandFormat),
    #[clap(name = "remove", about = "remove license header")]
    Remove(CommandRemove),
}

#[derive(Args)]
struct SharedOptions {
    #[arg(long, help = "path to the config file")]
    config: Option<PathBuf>,
    #[arg(long, help = "fail if process unknown files", default_value_t = false)]
    fail_if_unknown: bool,
}

impl SubCommand {
    pub fn run(self) -> Result<()> {
        match self {
            SubCommand::Check(cmd) => cmd.run(),
            SubCommand::Format(cmd) => cmd.run(),
            SubCommand::Remove(cmd) => cmd.run(),
        }
    }
}

#[derive(Parser)]
pub struct CommandCheck {
    #[command(flatten)]
    shared: SharedOptions,
}

struct CheckContext {
    unknown: Vec<String>,
    missing: Vec<String>,
}

impl Callback for CheckContext {
    fn on_unknown(&mut self, path: &Path) -> Result<()> {
        self.unknown.push(path.display().to_string());
        Ok(())
    }

    fn on_matched(&mut self, _: &HeaderMatcher, _: Document) -> Result<()> {
        Ok(())
    }

    fn on_not_matched(&mut self, _: &HeaderMatcher, document: Document) -> Result<()> {
        self.missing.push(document.filepath.display().to_string());
        Ok(())
    }
}

impl CommandCheck {
    fn run(self) -> Result<()> {
        let config = self.shared.config.unwrap_or_else(default_config);
        let mut context = CheckContext {
            unknown: vec![],
            missing: vec![],
        };
        check_license_header(config, &mut context)?;
        let mut exit_code = check_unknown_files(context.unknown, self.shared.fail_if_unknown);
        if !context.missing.is_empty() {
            error!("Found missing header files: {:?}", context.missing);
            exit_code = 1;
        }
        if exit_code != 0 {
            std::process::exit(exit_code);
        }
        info!("No missing header file has been found.");
        Ok(())
    }
}

#[derive(Parser)]
pub struct CommandFormat {
    #[command(flatten)]
    shared: SharedOptions,

    #[arg(long, help = "whether update file in place", default_value_t = false)]
    pub dry_run: bool,
}

struct FormatContext {
    dry_run: bool,
    unknown: Vec<String>,
    updated: Vec<String>,
}

impl Callback for FormatContext {
    fn on_unknown(&mut self, path: &Path) -> Result<()> {
        self.unknown.push(path.display().to_string());
        Ok(())
    }

    fn on_matched(&mut self, _: &HeaderMatcher, _: Document) -> Result<()> {
        Ok(())
    }

    fn on_not_matched(&mut self, header: &HeaderMatcher, mut doc: Document) -> Result<()> {
        if doc.header_detected() {
            doc.remove_header();
            doc.update_header(header);
            self.updated
                .push(format!("{}=replaced", doc.filepath.display()));
        } else {
            doc.update_header(header);
            self.updated
                .push(format!("{}=added", doc.filepath.display()));
        }
        if self.dry_run {
            let mut extension = doc.filepath.extension().unwrap_or_default().to_os_string();
            extension.push(".formatted");
            let copied = doc.filepath.with_extension(extension);
            doc.save(Some(&copied))
        } else {
            doc.save(None)
        }
    }
}

impl CommandFormat {
    fn run(self) -> Result<()> {
        let config = self.shared.config.unwrap_or_else(default_config);
        let mut context = FormatContext {
            dry_run: self.dry_run,
            unknown: vec![],
            updated: vec![],
        };
        check_license_header(config, &mut context)?;
        let mut exit_code = check_unknown_files(context.unknown, self.shared.fail_if_unknown);
        if !context.updated.is_empty() {
            warn!(
                "Updated header for files (dryRun={}): {:?}",
                self.dry_run, context.updated
            );
            exit_code = 1;
        }
        if exit_code != 0 {
            std::process::exit(exit_code);
        }
        info!("All files have proper header.");
        Ok(())
    }
}

#[derive(Parser)]
pub struct CommandRemove {
    #[command(flatten)]
    shared: SharedOptions,

    #[arg(long, help = "whether update file in place", default_value_t = false)]
    pub dry_run: bool,
}

struct RemoveContext {
    dry_run: bool,
    unknown: Vec<String>,
    removed: Vec<String>,
}

impl RemoveContext {
    fn remove(&mut self, doc: &mut Document) -> Result<()> {
        if !doc.header_detected() {
            return Ok(());
        }
        doc.remove_header();
        self.removed
            .push(format!("{}=removed", doc.filepath.display()));
        if self.dry_run {
            let mut extension = doc.filepath.extension().unwrap_or_default().to_os_string();
            extension.push(".removed");
            let copied = doc.filepath.with_extension(extension);
            doc.save(Some(&copied))
        } else {
            doc.save(None)
        }
    }
}

impl Callback for RemoveContext {
    fn on_unknown(&mut self, path: &Path) -> Result<()> {
        self.unknown.push(path.display().to_string());
        Ok(())
    }

    fn on_matched(&mut self, _: &HeaderMatcher, mut doc: Document) -> Result<()> {
        self.remove(&mut doc)
    }

    fn on_not_matched(&mut self, _: &HeaderMatcher, mut doc: Document) -> Result<()> {
        self.remove(&mut doc)
    }
}

impl CommandRemove {
    fn run(self) -> Result<()> {
        let config = self.shared.config.unwrap_or_else(default_config);
        let mut context = RemoveContext {
            dry_run: self.dry_run,
            unknown: vec![],
            removed: vec![],
        };
        check_license_header(config, &mut context)?;
        let mut exit_code = check_unknown_files(context.unknown, self.shared.fail_if_unknown);
        if !context.removed.is_empty() {
            warn!(
                "Removed header for files (dryRun={}): {:?}",
                self.dry_run, context.removed
            );
            exit_code = 1;
        }
        if exit_code != 0 {
            std::process::exit(exit_code);
        }
        info!("No file has been removed header.");
        Ok(())
    }
}

fn check_unknown_files(unknown: Vec<String>, fail_if_unknown: bool) -> i32 {
    if !unknown.is_empty() {
        if fail_if_unknown {
            error!("Processing unknown files: {:?}", unknown);
            return 1;
        } else {
            warn!("Processing unknown files: {:?}", unknown);
        }
    }
    0
}

fn default_config() -> PathBuf {
    PathBuf::new().join("licenserc.toml")
}
