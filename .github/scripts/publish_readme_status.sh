#!/usr/bin/env bash
set -euo pipefail

image=${1:?Usage: publish_readme_status.sh WEBP MAIN_SHA}
source_sha=${2:?Usage: publish_readme_status.sh WEBP MAIN_SHA}
branch=refs/heads/readme-assets
filename=repository-status.webp

test -s "$image"
remote_main=$(git ls-remote --exit-code origin refs/heads/main)
read -r main_tip _ <<< "$remote_main"
if [[ "$source_sha" != "$main_tip" ]]; then
    echo "Skipping publication because main advanced past $source_sha."
    exit 0
fi

base=
parents=()
if git ls-remote --exit-code origin "$branch" > /dev/null; then
    git fetch --no-tags --depth=1 origin "$branch"
    base=$(git rev-parse FETCH_HEAD)
    parents=(-p "$base")
else
    status=$?
    if [[ "$status" != 2 ]]; then
        exit "$status"
    fi
fi

blob=$(git hash-object -w -- "$image")
if [[ -n "$base" && $(git rev-parse --verify "$base:$filename" 2>/dev/null || true) == "$blob" ]]; then
    echo "The published card already matches this render."
    exit 0
fi

# A temporary index keeps the source checkout untouched and preserves any existing branch files.
index_dir=$(mktemp -d)
export GIT_INDEX_FILE="$index_dir/index"
trap 'rm -f -- "$GIT_INDEX_FILE"; rmdir -- "$index_dir"' EXIT
if [[ -n "$base" ]]; then
    git read-tree "$base"
else
    git read-tree --empty
fi
git update-index --add --cacheinfo "100644,$blob,$filename"
tree=$(git write-tree)
commit=$(git commit-tree "$tree" "${parents[@]}" -m "chore(readme): refresh repository status card")
git push origin "$commit:$branch"
