#![feature(extract_if)]

use clap::Parser;
use tracing_subscriber::filter::LevelFilter;

use crate::cli::Command;

pub mod cli;

fn main() -> hawkeye_fmt::Result<()> {
    tracing_subscriber::fmt()
        // .with_max_level(LevelFilter::DEBUG)
        .init();
    let cmd = Command::parse();
    cmd.run()
}
