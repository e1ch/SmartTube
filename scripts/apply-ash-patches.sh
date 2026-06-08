#!/usr/bin/env bash

set -euo pipefail

patch_repo="${PATCH_REPO:-$(pwd)}"
target_dir="${TARGET_DIR:-${1:-}}"
upstream_remote="${UPSTREAM_REMOTE:-upstream}"
upstream_url="${UPSTREAM_URL:-https://github.com/yuliskov/SmartTube.git}"
upstream_ref="${UPSTREAM_REF:-master}"
patch_ref="${PATCH_REF:-HEAD}"
patch_base_ref="${PATCH_BASE_REF:-}"

if [ -z "$target_dir" ]; then
  echo "TARGET_DIR or first argument is required." >&2
  exit 1
fi

patch_repo="$(cd "$patch_repo" && pwd)"
target_dir="$(cd "$target_dir" && pwd)"

if ! git -C "$patch_repo" remote get-url "$upstream_remote" >/dev/null 2>&1; then
  git -C "$patch_repo" remote add "$upstream_remote" "$upstream_url"
else
  git -C "$patch_repo" remote set-url "$upstream_remote" "$upstream_url"
fi

if [ -n "$patch_base_ref" ]; then
  merge_base="$patch_base_ref"
else
  git -C "$patch_repo" fetch "$upstream_remote" "$upstream_ref" --tags
  merge_base="$(git -C "$patch_repo" merge-base "$patch_ref" "$upstream_remote/$upstream_ref")"
fi
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

apply_patch_file() {
  local destination_dir="$1"
  local patch_file="$2"

  if git -C "$destination_dir" apply --check --whitespace=nowarn "$patch_file" >/dev/null 2>&1; then
    git -C "$destination_dir" apply --whitespace=nowarn "$patch_file"
  else
    git -C "$destination_dir" apply --3way --whitespace=nowarn "$patch_file"
  fi
}

apply_patch_series() {
  local source_dir="$1"
  local destination_dir="$2"
  local upstream="$3"
  local base_ref="${4:-}"
  local patch_file="$5"
  local series_ref="${6:-HEAD}"
  shift 6

  if [ ! -d "$source_dir/.git" ] && ! git -C "$source_dir" rev-parse --git-dir >/dev/null 2>&1; then
    echo "Skipping patch source without git metadata: $source_dir"
    return
  fi

  if [ ! -d "$destination_dir/.git" ] && ! git -C "$destination_dir" rev-parse --git-dir >/dev/null 2>&1; then
    echo "Skipping patch destination without git metadata: $destination_dir"
    return
  fi

  if ! git -C "$source_dir" remote get-url upstream >/dev/null 2>&1; then
    git -C "$source_dir" remote add upstream "$upstream"
  else
    git -C "$source_dir" remote set-url upstream "$upstream"
  fi

  git -C "$source_dir" fetch upstream master --tags >/dev/null 2>&1

  if [ -z "$base_ref" ]; then
    base_ref="$(git -C "$source_dir" merge-base HEAD upstream/master)"
  fi

  git -C "$source_dir" diff --binary "$base_ref..$series_ref" -- . "$@" > "$patch_file"

  if [ -s "$patch_file" ]; then
    apply_patch_file "$destination_dir" "$patch_file"
  else
    echo "No source patches generated for $source_dir."
  fi
}

fork_patch="$tmp_dir/ash-fork.patch"
apply_patch_series \
  "$patch_repo" \
  "$target_dir" \
  "$upstream_url" \
  "$merge_base" \
  "$fork_patch" \
  "$patch_ref" \
  ':(exclude).github' \
  ':(exclude)docs' \
  ':(exclude)scripts' \
  ':(exclude)patches' \
  ':(exclude)MediaServiceCore' \
  ':(exclude)SharedModules'

apply_patch_series \
  "$patch_repo/SharedModules" \
  "$target_dir/SharedModules" \
  "https://github.com/yuliskov/SharedModules.git" \
  "" \
  "$tmp_dir/sharedmodules.patch" \
  "HEAD"

apply_patch_series \
  "$patch_repo/MediaServiceCore/SharedModules" \
  "$target_dir/MediaServiceCore/SharedModules" \
  "https://github.com/yuliskov/SharedModules.git" \
  "" \
  "$tmp_dir/media-sharedmodules.patch" \
  "HEAD"

apply_patch_series \
  "$patch_repo/MediaServiceCore" \
  "$target_dir/MediaServiceCore" \
  "https://github.com/yuliskov/MediaServiceCore.git" \
  "" \
  "$tmp_dir/mediaservicecore.patch" \
  "HEAD" \
  ':(exclude)SharedModules'

if [ -d "$patch_repo/patches" ]; then
  series_file="$patch_repo/patches/series.conf"

  if [ -f "$series_file" ]; then
    while IFS= read -r entry || [ -n "$entry" ]; do
      entry="${entry%%#*}"
      entry="$(printf '%s' "$entry" | xargs)"

      if [ -z "$entry" ]; then
        continue
      fi

      patch_file="$patch_repo/patches/$entry"

      if [ ! -f "$patch_file" ]; then
        echo "Patch listed in patches/series.conf does not exist: $entry" >&2
        exit 1
      fi

      echo "Applying overlay patch: patches/$entry"
      apply_patch_file "$target_dir" "$patch_file"
    done < "$series_file"
  else
    while IFS= read -r patch_file; do
      echo "Applying overlay patch: ${patch_file#$patch_repo/}"
      apply_patch_file "$target_dir" "$patch_file"
    done < <(find "$patch_repo/patches" -type f -name '*.patch' | sort)
  fi
fi

echo "Applied Ash patches to $target_dir"
