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
perl -0777 -pe "s/(version\s*[:=]?\s*['\"]) [^'\"]+ (['\"]) /\$1${new_version}\$2/sex" -i build.gradle || true
# Fallback safer replace if perl expression above didn't match
perl -0777 -pe "s/(version\s*[:=]?\s*['\"]) [^'\"]+ (['\"]) /\$1${new_version}\$2/;" -i build.gradle || true

# Configure git and push commit + tag
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git add build.gradle
git commit -m "chore(release): bump version to ${new_version}" || true
git tag -a "v${new_version}" -m "Release ${new_version}" || true
git push origin HEAD || true
git push origin "v${new_version}" || true

echo "${new_version}"
