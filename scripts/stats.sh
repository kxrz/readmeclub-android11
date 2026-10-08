#!/bin/sh
# Download counts per release, from GitHub's public API (the repo must be public).
#   readmeclub.apk + readmeclub-x.y.z.apk : installs and updates of that version
#   manifest.json                         : app launches while that version was "latest"
#                                            (every launch checks it for updates)
set -eu
curl -fsS "https://api.github.com/repos/kxrz/readmeclub-android11/releases?per_page=100" |
  python3 -c '
import json, sys
for r in json.load(sys.stdin):
    counts = {a["name"]: a["download_count"] for a in r["assets"]}
    apk = sum(n for name, n in counts.items() if name.endswith(".apk"))
    print(f"{r[\"tag_name\"]:<14} downloads {apk:>6}   launches {counts.get(\"manifest.json\", 0):>7}   {r[\"published_at\"][:10]}")
'
