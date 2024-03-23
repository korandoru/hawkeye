use std::path::PathBuf;

use clap::{Parser, Subcommand};
use hawkeye_fmt::{processor::check_license_header, Result};
use tracing::{error, info, warn};

#[derive(Parser)]
pub struct Command {
    #[clap(subcommand)]
    sub: SubCommand,
}

impl Command {
    pub fn run(self) -> Result<()> {
        match self.sub {
            SubCommand::Check(cmd) => cmd.run(),
            SubCommand::Format(cmd) => cmd.run(),
        }
    }
}

#[derive(Subcommand)]
enum SubCommand {
    #[clap(about = "check license header")]
    Check(CommandCheck),
    #[clap(about = "format license header")]
    Format(CommandFormat),
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

fn default_config() -> PathBuf {
    PathBuf::new().join("licenserc.toml")
}
