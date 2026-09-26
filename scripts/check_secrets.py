#!/usr/bin/env python3
"""Focused source/history check. Never prints secret matches or file contents."""
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
PATTERN = re.compile(rb"AIza[0-9A-Za-z_-]{30,}|(?:sk-proj-|ghp_)[0-9A-Za-z_-]{20,}")

def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT)

paths = git("ls-files", "--cached", "--others", "--exclude-standard", "-z").split(b"\0")
found = []
for name in paths:
    if not name:
        continue
    file = ROOT / name.decode()
    if file.is_file() and file.suffix not in {".png", ".webp", ".jar"}:
        if PATTERN.search(file.read_bytes()):
            found.append(name.decode())
tracked_local = [p.decode() for p in git("ls-files", "-z").split(b"\0") if p and Path(p.decode()).name == "local.properties"]
history = git("log", "--all", "-p", "--format=")
history_count = len(PATTERN.findall(history))
local_history = git("log", "--all", "--format=%H", "--", "local.properties", "**/local.properties").splitlines()
print("Source files with potential secrets:", len(found))
for name in found:
    print("Review:", name)
print("Potential secret matches in history:", history_count)
print("Tracked local.properties files:", len(tracked_local))
print("Commits touching local.properties:", len(local_history))
sys.exit(1 if found or history_count or tracked_local or local_history else 0)
