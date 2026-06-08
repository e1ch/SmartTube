# Upstream Patch Build CI/CD

Ash builds are produced from a clean upstream SmartTube checkout plus patches. The CI/CD pipeline must not treat `e1ch/SmartTube` as the canonical source tree.

## Workflow

1. GitHub Actions checks out this repository as the patch/control repository.
2. `scripts/build-upstream-with-patches.sh` clones `https://github.com/yuliskov/SmartTube.git` into a temporary build directory.
3. `scripts/apply-ash-patches.sh` generates the fork source patch series from this repository relative to upstream and applies it to the clean upstream checkout.
4. Overlay patches listed in `patches/series.conf` are applied after the fork patch series. This is where experimental providers such as 8movie live.
5. Fork deltas for `SharedModules`, `MediaServiceCore`, and nested `MediaServiceCore/SharedModules` are applied inside the upstream-initialized submodule checkouts.
6. Gradle builds Beta and Stable APKs from the patched upstream tree.
7. The release workflow uploads stable filenames to the `release` GitHub Release, preserving the existing update manifest URLs.

## Patch Inputs

| Input | Default | Purpose |
|---|---|---|
| `UPSTREAM_URL` | `https://github.com/yuliskov/SmartTube.git` | Original SmartTube source. |
| `UPSTREAM_REF` | `ash-base` | Upstream branch/tag/ref to patch. `ash-base` resolves to the fork/upstream merge-base commit. |
| `PATCH_REF` | `HEAD` | Fork commit used to generate the Ash source patch series. |
| `patches/series.conf` | included | Ordered manifest of explicit overlay patches applied after the generated fork patch series. |

## Experimental 8movie Patch

`patches/experimental/8movie-shared-player.patch` adds the first third-party media source path:

- `8movie` homepage items are appended as an experimental Home row.
- External items carry provider/content/episode ids on `Video`.
- Playback resolves the provider page into a direct mp4/m3u8 URL.
- Provider headers are scoped to external playback and are not reused for YouTube.
- The existing `PlaybackPresenter -> PlaybackView -> ExoPlayer` path is used.
- Next episode playback asks the provider for the next episode instead of using YouTube suggestions.

8movie is Cloudflare-protected in some environments. When blocked, the provider reports a provider/cookie error and does not borrow Google OAuth or YouTube cookies.

## Local Validation

Patch-only check against the known-good Ash base:

```bash
SKIP_BUILD=true BUILD_ROOT=/tmp/smarttube-upstream-build ./scripts/build-upstream-with-patches.sh
```

Patch check against latest upstream:

```bash
SKIP_BUILD=true UPSTREAM_REF=master BUILD_ROOT=/tmp/smarttube-upstream-latest ./scripts/build-upstream-with-patches.sh
```

If the latest-upstream check conflicts, keep the release workflow on `ash-base`, resolve the conflicted source/submodule patches, and only then switch scheduled builds to a newer upstream ref.

Pull requests also run `Patch Validation`. The `ash-base` lane is required to pass; the `latest-upstream` lane is intentionally allowed to fail so it can expose upcoming upstream conflicts without blocking maintenance patches.

## Upstream Issue Watch

Original SmartTube issues are part of the merge risk model. Generate an issue report with:

```bash
scripts/generate-upstream-issues-report.sh yuliskov/SmartTube upstream_issues_report.md
```

Use [`docs/upstream-issue-triage.md`](../upstream-issue-triage.md) to decide whether an upstream change is safe to adopt or should remain isolated behind an Ash patch.

Full local build requires Android SDK and JDK 17:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
./scripts/build-upstream-with-patches.sh
```
