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

use clap::{Parser, Subcommand};
use hawkeye_fmt::{
    document::Document,
    header::matcher::HeaderMatcher,
    processor::{check_license_header, Callback},
    Result,
};
use tracing::{error, info, warn};

#[derive(Parser)]
#[command(version)]
pub struct Command {
    #[clap(subcommand)]
    sub: SubCommand,
}

impl Command {
    pub fn run(self) -> Result<()> {
        match self.sub {
            SubCommand::Check(cmd) => cmd.run(),
            SubCommand::Format(cmd) => cmd.run(),
            SubCommand::Remove(cmd) => cmd.run(),
        }
    }
}

#[derive(Subcommand)]
enum SubCommand {
    #[clap(about = "check license header")]
    Check(CommandCheck),
    #[clap(about = "format license header")]
    Format(CommandFormat),
    #[clap(about = "remove license header")]
    Remove(CommandRemove),
}

#[derive(Parser)]
pub struct CommandCheck {
    #[arg(long, help = "path to the config file")]
    pub config: Option<PathBuf>,
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
        let config = self.config.unwrap_or_else(default_config);
        let mut context = CheckContext {
            unknown: vec![],
            missing: vec![],
        };
        check_license_header(config, &mut context)?;
        if !context.unknown.is_empty() {
            warn!("Processing unknown files: {:?}", context.unknown);
        }
        if !context.missing.is_empty() {
            error!("Found missing header files: {:?}", context.missing);
            std::process::exit(1);
        }
        info!("No missing header file has been found.");
        Ok(())
    }
}

#[derive(Parser)]
pub struct CommandFormat {
    #[arg(long, help = "path to the config file")]
    pub config: Option<PathBuf>,

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
        let config = self.config.unwrap_or_else(default_config);
        let mut context = FormatContext {
            dry_run: self.dry_run,
            unknown: vec![],
            updated: vec![],
        };
        check_license_header(config, &mut context)?;
        if !context.unknown.is_empty() {
            warn!("Processing unknown files: {:?}", context.unknown);
        }
        if !context.updated.is_empty() {
            error!(
                "Updated header for files (dryRun={}): {:?}",
                self.dry_run, context.updated
            );
            std::process::exit(1);
        }
        info!("All files have proper header.");
        Ok(())
    }
}

#[derive(Parser)]
pub struct CommandRemove {
    #[arg(long, help = "path to the config file")]
    pub config: Option<PathBuf>,

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
        let config = self.config.unwrap_or_else(default_config);
        let mut context = RemoveContext {
            dry_run: self.dry_run,
            unknown: vec![],
            removed: vec![],
        };
        check_license_header(config, &mut context)?;
        if !context.unknown.is_empty() {
            warn!("Processing unknown files: {:?}", context.unknown);
        }
        if !context.removed.is_empty() {
            error!(
                "Removed header for files (dryRun={}): {:?}",
                self.dry_run, context.removed
            );
            std::process::exit(1);
        }
        info!("No file has been removed header.");
        Ok(())
    }
}

fn default_config() -> PathBuf {
    PathBuf::new().join("licenserc.toml")
}
