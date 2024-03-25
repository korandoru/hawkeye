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

use clap::{crate_description, FromArgMatches, Subcommand};
use tracing::level_filters::LevelFilter;
use tracing_subscriber::{fmt, layer::SubscriberExt, util::SubscriberInitExt, EnvFilter};

use crate::cli::SubCommand;

pub mod cli;

fn version() -> &'static str {
    concat!(
        "\nbranch: ",
        env!("GIT_BRANCH"),
        "\ncommit: ",
        env!("GIT_COMMIT"),
        "\ndirty: ",
        env!("GIT_DIRTY"),
        "\nversion: v",
        env!("CARGO_PKG_VERSION"),
        "\ntoolchain: ",
        env!("RUSTC_VERSION"),
        "\nbuild: ",
        env!("SOURCE_TIMESTAMP"),
    )
}

fn main() -> hawkeye_fmt::Result<()> {
    tracing_subscriber::registry()
        .with(fmt::layer())
        .with(
            EnvFilter::builder()
                .with_default_directive(LevelFilter::INFO.into())
                .from_env_lossy(),
        )
        .init();

    let cli = clap::Command::new("hawkeye")
        .subcommand_required(true)
        .version(version())
        .about(crate_description!());
    let cli = SubCommand::augment_subcommands(cli);
    let args = cli.get_matches();
    match SubCommand::from_arg_matches(&args) {
        Ok(cmd) => cmd.run(),
        Err(e) => e.exit(),
    }
}
