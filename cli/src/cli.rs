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

use clap::{Parser, Subcommand};
use hawkeye_fmt::{processor::check_license_header, Result};
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

impl CommandCheck {
    fn run(self) -> Result<()> {
        let config = self.config.unwrap_or_else(default_config);
        let (_, unknown, _, missing) = check_license_header(config)?;
        if !unknown.is_empty() {
            warn!(
                "Processing unknown files: {:?}",
                unknown.iter().map(|r| &r.filepath).collect::<Vec<_>>()
            );
        }
        if !missing.is_empty() {
            error!(
                "Found missing header files: {:?}",
                missing.iter().map(|r| &r.filepath).collect::<Vec<_>>()
            );
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

impl CommandFormat {
    fn run(self) -> Result<()> {
        let config = self.config.unwrap_or_else(default_config);
        let (header, unknown, _, missing) = check_license_header(config)?;

        if !unknown.is_empty() {
            warn!(
                "Processing unknown files: {:?}",
                unknown.iter().map(|r| &r.filepath).collect::<Vec<_>>()
            );
        }

        let mut updated_results = vec![];
        for mut doc in missing {
            if doc.header_detected() {
                doc.remove_header();
                doc.update_header(&header);
                updated_results.push(format!("{}=replaced", doc.filepath.display()));
            } else {
                doc.update_header(&header);
                updated_results.push(format!("{}=added", doc.filepath.display()));
            }
            if self.dry_run {
                let mut extension = doc.filepath.extension().unwrap_or_default().to_os_string();
                extension.push(".formatted");
                let copied = doc.filepath.with_extension(extension);
                doc.save(Some(&copied))?;
            } else {
                doc.save(None)?
            }
        }

        if !updated_results.is_empty() {
            error!(
                "Updated header for files (dryRun={}): {updated_results:?}",
                self.dry_run,
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

impl CommandRemove {
    fn run(self) -> Result<()> {
        let config = self.config.unwrap_or_else(default_config);
        let (_, unknown, matches, missing) = check_license_header(config)?;

        if !unknown.is_empty() {
            warn!(
                "Processing unknown files: {:?}",
                unknown.iter().map(|r| &r.filepath).collect::<Vec<_>>()
            );
        }

        let mut removed_results = vec![];
        for mut doc in matches.into_iter().chain(missing.into_iter()) {
            if !doc.header_detected() {
                continue;
            }
            doc.remove_header();
            removed_results.push(format!("{}=removed", doc.filepath.display()));
            if self.dry_run {
                let mut extension = doc.filepath.extension().unwrap_or_default().to_os_string();
                extension.push(".removed");
                let copied = doc.filepath.with_extension(extension);
                doc.save(Some(&copied))?;
            } else {
                doc.save(None)?
            }
        }

        if !removed_results.is_empty() {
            error!(
                "Removed header for files (dryRun={}): {removed_results:?}",
                self.dry_run,
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
