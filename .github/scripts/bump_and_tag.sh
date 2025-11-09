#!/usr/bin/env bash
set -euo pipefail

# Read current version from build.gradle
line=$(grep -E "^\s*version\s*[:=]?\s*['\"][^'\"]+['\"]" build.gradle | head -n1 || true)
if [ -z "${line}" ]; then
  current_version="0.0.0"
else
  current_version=$(echo "${line}" | sed -E "s/.*['\"]([^'\"]+)['\"].*/\1/")
fi

# Remove any suffix like -SNAPSHOT or -RC
base=${current_version%%-*}

IFS='.' read -r -a parts <<< "${base}"
major=${parts[0]:-0}
minor=${parts[1]:-0}
patch=${parts[2]:-0}

if ! [[ ${major} =~ ^[0-9]+$ ]]; then major=0; fi
if ! [[ ${minor} =~ ^[0-9]+$ ]]; then minor=0; fi
if ! [[ ${patch} =~ ^[0-9]+$ ]]; then patch=0; fi

new_patch=$((patch + 1))
new_version="${major}.${minor}.${new_patch}"

# Replace the first occurrence of version in build.gradle
# Prefer Python if available for robust regex replacement
if command -v python3 >/dev/null 2>&1; then
  python3 - <<PY
import re
text = open('build.gradle', 'r', encoding='utf-8').read()
new = re.sub(r"(version\s*[:=]?\s*['\"]) [^'\"]+ (['\"])", r"\1" + "${new_version}" + r"\2", text, count=1)
open('build.gradle', 'w', encoding='utf-8').write(new)
PY
else
  # Fallback to sed (GNU sed compatible)
  sed -E -i "s/(version\s*[:=]?\s*['\"]).*(['\"])$/\1${new_version}\2/" build.gradle || true
fi

# Configure git and push commit + tag
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git add build.gradle >/dev/null 2>&1 || true
git commit -m "chore(release): bump version to ${new_version}" >/dev/null 2>&1 || true
git tag -a "v${new_version}" -m "Release ${new_version}" >/dev/null 2>&1 || true
# Suppress push output (so Actions won't capture unrelated lines)
git push origin HEAD >/dev/null 2>&1 || true
git push origin "v${new_version}" >/dev/null 2>&1 || true

# Only print the new version on stdout (useful for CI to capture)
printf "%s\n" "${new_version}"
