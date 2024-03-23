use std::path::PathBuf;

use clap::{Parser, Subcommand};
use tracing::{error, info, warn};

use hawkeye_fmt::{processor::CheckResult, Result};
use hawkeye_fmt::processor::check_license_header;

#[derive(Parser)]
pub struct Command {
    #[clap(subcommand)]
    sub: SubCommand,
}

impl Command {
    pub fn run(self) -> Result<()> {
        match self.sub {
            SubCommand::Check(cmd) => cmd.run(),
        }
    }
}

#[derive(Subcommand)]
enum SubCommand {
    #[clap(about = "check license header")]
    Check(CommandCheck),
}

#[derive(Parser)]
pub struct CommandCheck {
    #[arg(
        long,
        help = "path to the config file",
        default_value = default_config().into_os_string(),
    )]
    pub config: PathBuf,
}

impl CommandCheck {
    fn run(self) -> Result<()> {
        let mut reports = check_license_header(self.config)?;
        let unknown = reports
            .extract_if(|r| matches!(r, CheckResult::Unsupported(_)))
            .collect::<Vec<_>>();
        let missing = reports
            .extract_if(|r| matches!(r, CheckResult::NotMatched(_)))
            .collect::<Vec<_>>();
        if !unknown.is_empty() {
            warn!(
                "Processing unknown files: {:?}",
                unknown.iter().map(|r| r.path()).collect::<Vec<_>>()
            );
        }
        if !missing.is_empty() {
            error!(
                "Found missing header files: {:?}",
                missing.iter().map(|r| r.path()).collect::<Vec<_>>()
            );
            std::process::exit(1);
        }
        info!("No missing header file has been found.");
        Ok(())
    }
}

fn default_config() -> PathBuf {
    PathBuf::new().join("licenserc.toml")
}
