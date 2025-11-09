#!/usr/bin/env python3
import os
import re
import subprocess
import sys

def read_current_version():
    """Read current version from build.gradle"""
    try:
        with open('build.gradle', 'r', encoding='utf-8') as f:
            content = f.read()
    except FileNotFoundError:
        return "0.0.0"

    # Find version line using regex
    match = re.search(r"^\s*version\s*[:=]?\s*['\"]([^'\"]+)['\"]", content, re.MULTILINE)
    if match:
        return match.group(1)
    return "0.0.0"

def bump_version(version):
    """Bump patch version"""
    # Remove any suffix like -SNAPSHOT or -RC
    base = version.split('-')[0]

    parts = base.split('.')
    if len(parts) != 3:
        return "0.0.1"

    try:
        major = int(parts[0])
        minor = int(parts[1])
        patch = int(parts[2])
    except ValueError:
        return "0.0.1"

    new_patch = patch + 1
    return f"{major}.{minor}.{new_patch}"

def update_gradle_file(new_version):
    """Update version in build.gradle using regex"""
    try:
        with open('build.gradle', 'r', encoding='utf-8') as f:
            content = f.read()
    except FileNotFoundError:
        print("Error: build.gradle not found", file=sys.stderr)
        sys.exit(1)

    def replace_version(match):
        return match.group(1) + new_version + match.group(2)

    # Replace the first occurrence of version
    new_content = re.sub(r"(version\s*[:=]?\s*['\"])[^'\"]*(['\"])", replace_version, content, count=1)

    with open('build.gradle', 'w', encoding='utf-8') as f:
        f.write(new_content)

def git_operations(new_version):
    """Perform git operations: add, commit, tag, push"""
    # Configure git
    subprocess.run(['git', 'config', 'user.name', 'github-actions[bot]'],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    subprocess.run(['git', 'config', 'user.email', '41898282+github-actions[bot]@users.noreply.github.com'],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    # Add, commit, tag, push
    try:
        subprocess.run(['git', 'add', 'build.gradle'], check=True,
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run(['git', 'commit', '-m', f'chore(release): bump version to {new_version}'],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run(['git', 'tag', '-a', f'v{new_version}', '-m', f'Release {new_version}'],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run(['git', 'push', 'origin', 'HEAD'], check=True,
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        subprocess.run(['git', 'push', 'origin', f'v{new_version}'], check=True,
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except subprocess.CalledProcessError as e:
        print(f"Git operation failed: {e}", file=sys.stderr)
        sys.exit(1)

def main():
    if len(sys.argv) > 1 and sys.argv[1] == '--help':
        print("Usage: python bump_version.py")
        print("This script automatically bumps the patch version in build.gradle, commits and tags the change.")
        sys.exit(0)

    current_version = read_current_version()
    new_version = bump_version(current_version)

    update_gradle_file(new_version)
    git_operations(new_version)

    # Print the new version as output
    print(new_version)

if __name__ == "__main__":
    main()