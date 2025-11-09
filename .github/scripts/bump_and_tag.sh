#!/usr/bin/env bash
set -euo pipefail

# Run Python script for version bumping and tagging
python3 .github/scripts/bump_version.py
