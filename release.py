#!/usr/bin/env python3

# Copyright 2024 tison <wander4096@gmail.com>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from argparse import ArgumentDefaultsHelpFormatter, ArgumentParser, BooleanOptionalAction
import fileinput
from pathlib import Path
import re
import shutil
import subprocess
import tomllib

parser = ArgumentParser(formatter_class=ArgumentDefaultsHelpFormatter)
parser.add_argument('version', help='version or level to bump')
parser.add_argument('--execute', '-x', action=BooleanOptionalAction, help='whether to execute the command (default to dry-run)')
args = parser.parse_args()

basedir = Path(__file__).parent.absolute()

# 0. Pull latest
subprocess.run(["git", "pull", "--rebase=true", "--autostash"], cwd=basedir, check=True)

# 1. Bump version
if args.execute:
    cmd = ["cargo", "release", "version", args.version, "-x"]
else:
    cmd = ["cargo", "release", "version", args.version]

subprocess.run(cmd, cwd=basedir, check=True)
info = tomllib.loads((basedir / 'Cargo.toml').read_text())
version = info['workspace']['package']['version']
version = f'v{version}'

# 2. Update action.yml
pattern = re.compile(r'docker://ghcr.io/korandoru/hawkeye.*')
with fileinput.FileInput(basedir / 'action.yml', inplace=True, backup='.bak') as content:
    for line in content:
        print(pattern.sub(f'docker://ghcr.io/korandoru/hawkeye:{version}', line), end='')

subprocess.run(["git", "--no-pager", "diff", "."], cwd=basedir, check=True)
if args.execute:
    subprocess.run(["git", "add", "-A", "."], cwd=basedir, check=True)
    subprocess.run(["git", "status"], cwd=basedir, check=True)
    subprocess.run(["git", "commit", "-s", "-m", f"chore: release {version}"], cwd=basedir, check=True)

# 3. Release
if args.execute:
    cmd = ["cargo", "release", "-x"]
else:
    cmd = ["cargo", "release"]
subprocess.run(cmd, cwd=basedir, check=args.execute)

# 4. Change back action.yml
shutil.copy2(basedir / 'action.yml.bak', basedir / 'action.yml')
subprocess.run(["git", "--no-pager", "diff", "."], cwd=basedir, check=True)
if args.execute:
    subprocess.run(["git", "add", "-A", "."], cwd=basedir, check=True)
    subprocess.run(["git", "status"], cwd=basedir, check=True)
    subprocess.run(["git", "commit", "-s", "-m", f"chore: post-release {version}"], cwd=basedir, check=True)
    subprocess.run(["git", "push"], cwd=basedir, check=True)
