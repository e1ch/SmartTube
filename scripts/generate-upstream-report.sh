#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

upstream_remote="${1:-upstream}"
upstream_ref="${2:-master}"
output_file="${3:-upstream_report.md}"

if ! git remote get-url "$upstream_remote" >/dev/null 2>&1; then
  echo "Remote '$upstream_remote' not found." >&2
  exit 1
fi

git fetch "$upstream_remote" "$upstream_ref" --tags >/dev/null 2>&1

fork_head="$(git rev-parse --short HEAD)"
upstream_head="$(git rev-parse --short "$upstream_remote/$upstream_ref")"
merge_base="$(git merge-base HEAD "$upstream_remote/$upstream_ref")"
merge_base_short="$(git rev-parse --short "$merge_base")"
fork_version="$(sed -n '1p' VERSION)"
ahead_behind="$(git rev-list --left-right --count "$upstream_remote/$upstream_ref...HEAD")"
behind_count="$(printf '%s' "$ahead_behind" | awk '{print $1}')"
ahead_count="$(printf '%s' "$ahead_behind" | awk '{print $2}')"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

git diff --name-only "$merge_base..HEAD" | sort -u > "$tmp_dir/fork_files.txt"
git diff --name-only "$merge_base..$upstream_remote/$upstream_ref" | sort -u > "$tmp_dir/upstream_files.txt"
comm -12 "$tmp_dir/fork_files.txt" "$tmp_dir/upstream_files.txt" > "$tmp_dir/overlap_files.txt"

{
  echo "# Upstream Watch Report"
  echo
  echo "| Item | Value |"
  echo "|---|---|"
  echo "| Fork HEAD | \`$fork_head\` |"
  echo "| Fork version | \`$fork_version\` |"
  echo "| Upstream HEAD | \`$upstream_head\` |"
  echo "| Merge base | \`$merge_base_short\` |"
  echo "| Divergence | \`$ahead_count ahead / $behind_count behind\` |"
  echo

  echo "## Upstream-only commits"
  if [ "$behind_count" -eq 0 ]; then
    echo
    echo "_No upstream commits pending._"
  else
    echo
    git log --reverse --date=short --pretty='- `%h` %ad %s' "$merge_base..$upstream_remote/$upstream_ref"
  fi
  echo

  echo "## File overlap risk"
  overlap_count="$(wc -l < "$tmp_dir/overlap_files.txt" | tr -d ' ')"
  echo
  echo "Changed by both fork and upstream since the split: \`$overlap_count\` files."
  echo
  if [ "$overlap_count" -eq 0 ]; then
    echo "_No overlapping files._"
  else
    sed 's/^/- `/' "$tmp_dir/overlap_files.txt" | sed 's/$/`/'
  fi
  echo

  echo "## Upstream changed files"
  echo
  if [ "$behind_count" -eq 0 ]; then
    echo "_No upstream file changes pending._"
  else
    sed 's/^/- `/' "$tmp_dir/upstream_files.txt" | sed 's/$/`/'
  fi
} > "$output_file"

echo "Wrote $output_file"
