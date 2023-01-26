#!/usr/bin/env python3
# Copyright 2023 Korandoru Contributors
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


import fileinput
import re

from argparse import ArgumentDefaultsHelpFormatter, ArgumentParser
from pathlib import Path


if __name__ == '__main__':
    parser = ArgumentParser(formatter_class=ArgumentDefaultsHelpFormatter)
    parser.add_argument('version', default='latest', nargs='?', help='version of hawkeye-native image')
    args = parser.parse_args()

    pattern = re.compile(r'docker://ghcr.io/korandoru/hawkeye-native.*')
    basedir = Path(__file__).parent.parent.absolute()
    with fileinput.FileInput(basedir / 'action.yml', inplace=True, backup='.bak') as content:
        for line in content:
            print(pattern.sub(f'docker://ghcr.io/korandoru/hawkeye-native:{args.version}', line), end='')
