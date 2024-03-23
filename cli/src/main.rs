use clap::Parser;
use crate::cli::Command;

pub mod cli;

fn main() -> hawkeye_fmt::Result<()> {
    let cmd = Command::parse();
    cmd.run()
}
