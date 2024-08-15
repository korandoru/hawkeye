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
#
# Copyright 2024 - 2024, tison <wander4096@gmail.com> and the HawkEye contributors
# SPDX-License-Identifier: Apache-2.0

from pathlib import Path
import difflib
import subprocess

def diff_files(file1, file2):
    with file1.open("r", encoding="utf8") as f1, file2.open("r", encoding="utf8") as f2:
        diff = difflib.unified_diff(f1.readlines(), f2.readlines(), str(file1), str(file2))
        diff = list(diff)
        if diff:
            for line in diff:
                print(line, end="")
            exit(1)


basedir = Path(__file__).parent.absolute()
rootdir = basedir.parent

subprocess.run(["cargo", "build", "--bin", "hawkeye"], cwd=rootdir, check=True)
hawkeye = rootdir / "target" / "debug" / "hawkeye"

subprocess.run([hawkeye, "format", "--fail-if-unknown", "--fail-if-updated=false", "--dry-run"], cwd=(basedir / "load_header_path"), check=True)
diff_files(basedir / "load_header_path" / "main.rs.expected", basedir / "load_header_path" / "main.rs.formatted")

subprocess.run([hawkeye, "format", "--fail-if-unknown", "--fail-if-updated=false", "--dry-run"], cwd=(basedir / "regression_blank_line"), check=True)
diff_files(basedir / "regression_blank_line" / "main.rs.expected", basedir / "regression_blank_line" / "main.rs.formatted")
