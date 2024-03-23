use std::path::PathBuf;

use clap::{Parser, Subcommand};

use hawkeye_fmt::Result;

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
        hawkeye_fmt::processor::do_check(self.config)
    }
}

fn default_config() -> PathBuf {
    PathBuf::new().join("licenserc.toml")
}
