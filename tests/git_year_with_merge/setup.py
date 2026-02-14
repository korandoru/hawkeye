#!/usr/bin/env python3
import subprocess
import os
import shutil
import sys
import time
from pathlib import Path

# Setup paths
case_dir = Path(__file__).parent.absolute()
test_dir = case_dir
git_dir = test_dir / ".git"

def run_cmd(cmd, cwd=None, env=None):
    subprocess.run(cmd, cwd=cwd, check=True, env=env)

def git_add(filename):
    run_cmd(["git", "add", filename], cwd=test_dir)

def git_commit(msg, date_day, files=[]):
    for filename in files:
        git_add(filename)

    date_str = f"{date_day}T00:00:00Z"
    env = os.environ.copy()
    env["GIT_COMMITTER_DATE"] = date_str
    env["GIT_AUTHOR_DATE"] = date_str
    run_cmd(["git", "commit", "--allow-empty", "-m", msg], cwd=test_dir, env=env)

def setup():
    clean()

    # Init git repo
    print("Initializing git repo...")
    run_cmd(["git", "init"], cwd=test_dir)
    # Older git versions default to `master` so we explicitly name it `main`
    run_cmd(["git", "branch", "-m", "main"], cwd=test_dir)
    # Configure user for reproducibility
    run_cmd(["git", "config", "user.email", "you@example.com"], cwd=test_dir)
    run_cmd(["git", "config", "user.name", "Your Name"], cwd=test_dir)

    # Create common ancestors
    git_commit("init", "2025-12-15")
    git_commit("base", "2025-12-22")

    run_cmd(["git", "branch", "wip"], cwd=test_dir)

    # Create more commits on `main`
    git_commit("mA2025", "2025-12-30", ["a.rs"])
    git_commit("mB2026", "2026-01-05", ["b.rs"])
    git_commit("dummy", "2026-01-07")
    git_commit("random", "2026-01-08")

    # Create commits on `wip`
    run_cmd(["git", "checkout", "wip"], cwd=test_dir)
    git_commit("w", "2026-01-06")

    # Merge `wip` into `main`
    run_cmd(["git", "checkout", "main"], cwd=test_dir)
    run_cmd(["git", "merge", "wip", "--no-edit"], cwd=test_dir)

def clean():
    print("Removing git repo...")
    if git_dir.exists():
        shutil.rmtree(git_dir)


if __name__ == "__main__":
    if sys.argv[1] == "setup":
        setup()
    elif sys.argv[1] == "clean":
        clean()
    else:
        pass

