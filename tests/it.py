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

from pathlib import Path
import difflib
import subprocess
import os
import shutil

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

def drive(name, files, create_temp_copy=False):
    temp_paths = []
    case_dir = basedir / name
    try:
        if create_temp_copy:
            # Copy all files, appending _temp before the extension
            for filepath in files:
                base, ext = os.path.splitext(filepath)
                temp_path = f"{base}_temp{ext}"
                shutil.copy2(case_dir / filepath, case_dir / temp_path)
                temp_paths.append(temp_path)
        else:
            temp_paths = files
        subprocess.run([hawkeye, "format", "--fail-if-unknown", "--fail-if-updated=false", "--dry-run"], cwd=case_dir, check=True)

        for file in temp_paths:
            file = file.replace("_temp", "")
            diff_files(case_dir / f"{file}.expected", case_dir / f"{file}.formatted")
    finally:
        # Remove all temp files at the end
        if create_temp_copy:
            for temp_path in temp_paths:
                if os.path.exists(case_dir / temp_path):
                    os.remove(case_dir / temp_path)

drive("attrs_and_props", ["main.rs"])
drive("load_header_path", ["main.rs"])
drive("bom_issue", ["headless_bom.cs"])
drive("regression_blank_line", ["main.rs"])
drive("regression_no_blank_lines", ["repro.py"])
drive("disk_file_creation_year", ["main.rs"], True)
