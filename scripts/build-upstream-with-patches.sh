#!/usr/bin/env bash

set -euo pipefail

patch_repo="${PATCH_REPO:-$(pwd)}"
upstream_url="${UPSTREAM_URL:-https://github.com/yuliskov/SmartTube.git}"
upstream_ref="${UPSTREAM_REF:-ash-base}"
patch_ref="${PATCH_REF:-HEAD}"
build_root="${BUILD_ROOT:-${RUNNER_TEMP:-/tmp}/smarttube-upstream-build}"
release_assets_dir="${RELEASE_ASSETS_DIR:-$patch_repo/release_assets}"
skip_build="${SKIP_BUILD:-false}"
runner_temp="${RUNNER_TEMP:-/tmp}"

patch_repo="$(cd "$patch_repo" && pwd)"

resolve_upstream_ref() {
  if [ "$upstream_ref" != "ash-base" ]; then
    printf '%s' "$upstream_ref"
    return
  fi

  if ! git -C "$patch_repo" remote get-url upstream >/dev/null 2>&1; then
    git -C "$patch_repo" remote add upstream "$upstream_url"
  else
    git -C "$patch_repo" remote set-url upstream "$upstream_url"
  fi

  git -C "$patch_repo" fetch upstream master --tags >/dev/null 2>&1
  git -C "$patch_repo" merge-base "$patch_ref" upstream/master
}

resolve_patch_base_ref() {
  if ! git -C "$patch_repo" remote get-url upstream >/dev/null 2>&1; then
    git -C "$patch_repo" remote add upstream "$upstream_url"
  else
    git -C "$patch_repo" remote set-url upstream "$upstream_url"
  fi

  git -C "$patch_repo" fetch upstream master --tags >/dev/null 2>&1
  git -C "$patch_repo" merge-base "$patch_ref" upstream/master
}

resolved_upstream_ref="$(resolve_upstream_ref)"
patch_base_ref="$(resolve_patch_base_ref)"

case "$build_root" in
  /tmp/*|"$patch_repo"/build/*|"$patch_repo"/buildout/*|"$runner_temp"/*) ;;
  *)
    echo "Refusing to remove unsafe BUILD_ROOT: $build_root" >&2
    exit 1
    ;;
esac

rm -rf "$build_root"
git clone "$upstream_url" "$build_root"
git -C "$build_root" checkout "$resolved_upstream_ref"
git -C "$build_root" submodule update --init --recursive

PATCH_REPO="$patch_repo" TARGET_DIR="$build_root" UPSTREAM_URL="$upstream_url" UPSTREAM_REF="master" PATCH_REF="$patch_ref" PATCH_BASE_REF="$patch_base_ref" \
  "$patch_repo/scripts/apply-ash-patches.sh"

if [ "$skip_build" = "true" ]; then
  echo "SKIP_BUILD=true; patched tree is ready at $build_root"
  exit 0
fi

cd "$build_root"

if [ "${HAS_SIGNING_KEY:-false}" = "true" ] && [ -n "${SIGNING_KEY:-}" ]; then
  echo "storePassword=${KEY_STORE_PASSWORD}" > keystore.properties
  echo "keyAlias=${ALIAS}" >> keystore.properties
  echo "keyPassword=${KEY_PASSWORD}" >> keystore.properties
  echo "storeFile=$build_root/key.jks" >> keystore.properties
  echo "$SIGNING_KEY" | base64 --decode > "$build_root/key.jks"
  mkdir -p "$HOME/.android"
  echo "$SIGNING_KEY" | base64 --decode > "$HOME/.android/debug.keystore"
fi

chmod +x gradlew
./gradlew assembleStbetaDebug assembleStstableDebug

version_name="$(sed -n '1p' VERSION)"
version_code="$(grep "versionCode" smarttubetv/build.gradle | head -n 1 | awk '{print $2}')"

mkdir -p "$release_assets_dir"
rm -f "$release_assets_dir"/*.apk

for abi in armeabi-v7a arm64-v8a x86 universal; do
  cp smarttubetv/build/outputs/apk/stbeta/debug/*_"$abi".apk "$release_assets_dir/SmartTube_beta_$abi.apk"
  cp smarttubetv/build/outputs/apk/ststable/debug/*_"$abi".apk "$release_assets_dir/SmartTube_stable_$abi.apk"
done

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "VERSION_NAME=$version_name" >> "$GITHUB_OUTPUT"
  echo "VERSION_CODE=$version_code" >> "$GITHUB_OUTPUT"
  echo "UPSTREAM_RESOLVED_REF=$resolved_upstream_ref" >> "$GITHUB_OUTPUT"
  echo "PATCH_BASE_REF=$patch_base_ref" >> "$GITHUB_OUTPUT"
  echo "PATCHED_TREE=$build_root" >> "$GITHUB_OUTPUT"
  echo "RELEASE_ASSETS_DIR=$release_assets_dir" >> "$GITHUB_OUTPUT"
fi

echo "Built SmartTube Ash from upstream $upstream_url#$resolved_upstream_ref with patch repo $patch_repo"
