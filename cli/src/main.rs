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

use clap::FromArgMatches;
use clap::Subcommand;
use logforth::filter::env_filter::EnvFilterBuilder;

use crate::subcommand::SubCommand;

pub mod subcommand;
pub mod version;

fn main() {
    logforth::starter_log::stderr()
        .filter(EnvFilterBuilder::from_default_env_or("info").build())
        .apply();

    let build_info = version::build_info();
    let command = clap::Command::new("hawkeye")
        .subcommand_required(true)
        .about(build_info.description)
        .version(build_info.version)
        .long_version(version::version());
    let command = SubCommand::augment_subcommands(command);
    let args = command.get_matches();
    match SubCommand::from_arg_matches(&args) {
        Ok(cmd) => cmd.run(),
        Err(e) => e.exit(),
    }
}
