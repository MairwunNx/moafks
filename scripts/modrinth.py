"""Upload the plugin's jar to Modrinth as a version of the project, marked to run on the server only.

Standard library only: MODRINTH_TOKEN=… python3 -I scripts/modrinth.py --version X.Y.Z --notes release-notes.md
The release workflow runs it after `./gradlew build` and Minotaur's sync of the description. Minotaur can't set a
version's `environment`, which Modrinth refuses a version without while the project is in review, so the jar goes up
through Modrinth's v3 API here. A version already uploaded is skipped.
"""

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PROJECT = "moafks"
API = "https://api.modrinth.com"
AGENT = "MairwunNx/moafks (github.com/MairwunNx)"
LOADERS = ["paper", "purpur"]


def request(method, path, token, body=None, headers=None):
    req = urllib.request.Request(f"{API}{path}", data=body, method=method,
                                 headers={"User-Agent": AGENT, "Authorization": token, **(headers or {})})
    try:
        with urllib.request.urlopen(req, timeout=120) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        sys.exit(f"{method} {path}: {error.code} {error.read().decode(errors='replace')}")


def game_version():
    """The game version Minotaur's block in the build file names, as CurseForge's step reads it."""
    match = re.search(r'gameVersions\.addAll\("([^"]+)"\)', (ROOT / "build.gradle.kts").read_text(encoding="utf-8"))
    return match.group(1) if match else sys.exit("No gameVersions in build.gradle.kts")


def jar_path(version):
    return ROOT / "build/libs" / f"moafks-{version}.jar"


def multipart(data, file_name, file_bytes):
    boundary = uuid.uuid4().hex
    parts = [
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"data\"\r\nContent-Type: application/json\r\n\r\n".encode(),
        json.dumps(data).encode(), b"\r\n",
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{file_name}\"\r\n"
        f"Content-Type: application/java-archive\r\n\r\n".encode(),
        file_bytes, f"\r\n--{boundary}--\r\n".encode(),
    ]
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--version", required=True)
    parser.add_argument("--notes", required=True, help="a Markdown file with the release notes")
    args = parser.parse_args()
    jar = jar_path(args.version)
    if not jar.is_file():
        sys.exit(f"{jar} isn't built")
    game = game_version()
    token = os.environ.get("MODRINTH_TOKEN") or sys.exit("MODRINTH_TOKEN isn't set")
    notes = Path(args.notes).read_text(encoding="utf-8")
    project = request("GET", f"/v2/project/{PROJECT}", token)["id"]
    if args.version in {v["version_number"] for v in request("GET", f"/v2/project/{project}/version", token)}:
        print(f"{args.version}: already on Modrinth")
        return

    data = {
        "name": f"Mo’Afks {args.version}", "version_number": args.version, "changelog": notes,
        "game_versions": [game], "loaders": LOADERS, "version_type": "release", "featured": True,
        "project_id": project, "status": "listed", "environment": "server_only", "dependencies": [],
        "file_parts": ["file"], "primary_file": "file",
    }
    body, content_type = multipart(data, jar.name, jar.read_bytes())
    version = request("POST", "/v3/version", token, body, {"Content-Type": content_type})
    print(f"{args.version}: {version['id']}, {version.get('environment')}")


if __name__ == "__main__":
    main()
