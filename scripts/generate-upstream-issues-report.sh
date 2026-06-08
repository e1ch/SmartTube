#!/usr/bin/env bash

set -euo pipefail

upstream_repo="${1:-yuliskov/SmartTube}"
output_file="${2:-upstream_issues_report.md}"

require_gh() {
  if ! command -v gh >/dev/null 2>&1; then
    echo "GitHub CLI 'gh' is required to generate the upstream issues report." >&2
    exit 1
  fi
}

issue_table() {
  local title="$1"
  local search="$2"
  local limit="${3:-12}"

  echo "## $title"
  echo
  echo "| Issue | Labels | Updated |"
  echo "|---|---|---|"

  gh issue list \
    --repo "$upstream_repo" \
    --state open \
    --search "$search" \
    --limit "$limit" \
    --json number,title,url,updatedAt,labels \
    --jq '.[] | "| [#\(.number) \(.title)](\(.url)) | \([.labels[].name] | join(", ")) | \(.updatedAt[0:10]) |"' \
    || echo "| _Unable to query issues_ |  |  |"

  echo
}

require_gh

{
  echo "# Original SmartTube Issue Watch"
  echo
  echo "Source repo: \`$upstream_repo\`"
  echo
  echo "Generated: \`$(date -u +%Y-%m-%dT%H:%M:%SZ)\`"
  echo
  echo "This report tracks upstream issues that can affect Ash patch builds, account state, startup order, search, and shared-player patches."
  echo

  issue_table "Account, Login, History, and Profile State" "account OR login OR history OR watched OR profile OR sync OR \"search history\"" 20
  issue_table "Search Regressions" "search" 16
  issue_table "Startup, Settings, and Crash Regressions" "startup OR opening OR crash OR settings OR IllegalStateException OR UnknownHostException" 16
  issue_table "Playback and Player Regressions" "playback OR player OR buffer OR \"Unexpected playback\" OR loading OR HDR10 OR WebView" 20
  issue_table "Recent Open Bugs" "label:bug sort:updated-desc" 20
} > "$output_file"

echo "Wrote $output_file"
