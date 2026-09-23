from pathlib import Path
import os
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, stderr=subprocess.DEVNULL)


secrets = []
local = ROOT / "local.properties"
if local.exists():
    for line in local.read_text(encoding="utf-8-sig").splitlines():
        match = re.match(r"\s*GEMINI_API_KEY\s*=\s*(.+)", line)
        if match and match[1].strip() != "your_api_key_here":
            secrets.append(match[1].strip().encode())
if os.getenv("GEMINI_API_KEY"):
    secrets.append(os.environ["GEMINI_API_KEY"].encode())
patterns = [re.compile(rb"AIza[0-9A-Za-z_-]{35}"), re.compile(rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----")]


def has_secret(data):
    return any(value in data for value in secrets) or any(pattern.search(data) for pattern in patterns)


failures = []
paths = git("ls-files", "--cached", "--others", "--exclude-standard", "-z").decode().split("\0")
for name in filter(None, paths):
    path = ROOT / name
    if path.is_file() and has_secret(path.read_bytes()):
        failures.append("Secret detected in working file: " + name)
tracked = git("ls-files", "-z").decode().split("\0")
if "local.properties" in tracked or "local.properties.txt" in tracked:
    failures.append("Local credential file is tracked or staged")
if subprocess.run(["git", "check-ignore", "-q", "local.properties"], cwd=ROOT).returncode:
    failures.append("local.properties is not ignored")
for line in git("rev-list", "--objects", "--all").splitlines():
    object_id = line.split(b" ", 1)[0].decode()
    kind = git("cat-file", "-t", object_id).strip()
    if kind in (b"blob", b"commit", b"tag") and has_secret(git("cat-file", "-p", object_id)):
        failures.append("Secret detected in Git object " + object_id)
for line in git("log", "--all", "--format=", "--name-only").decode().splitlines():
    if Path(line).name in ("local.properties", "local.properties.txt"):
        failures.append("Credential filename found in Git history")
if has_secret(git("diff", "HEAD")) or has_secret(git("diff", "--cached")):
    failures.append("Secret detected in Git diff")
if failures:
    print("SECRET AUDIT FAILED")
    print("\n".join(sorted(set(failures))))
    sys.exit(1)
print("SECRET AUDIT PASSED: working files, index, diffs and all reachable Git history checked; local.properties ignored and untracked. No secret values printed.")
