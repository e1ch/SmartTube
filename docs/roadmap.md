# Ash Patch-Based Roadmap

This repository should operate as a patch/control repository for original SmartTube builds.

## Confirmed Issues

| Priority | Issue | Status | Resolution Path |
|---|---|---|---|
| P0 | Release builds must use original `yuliskov/SmartTube` plus Ash patches, not the fork tree as canonical source. | In progress | `scripts/build-upstream-with-patches.sh` clones upstream, applies generated fork/submodule deltas, then applies `patches/series.conf`. |
| P0 | Patch ordering must be version controlled. | Fixed in this branch | `patches/series.conf` is now the ordered overlay patch manifest. |
| P0 | CI must prove patches still apply. | Fixed in this branch | `Patch Validation` checks `ash-base` as required and latest upstream as allowed-failure signal. |
| P1 | Latest upstream currently conflicts with Ash deltas. | Open | Keep releases on `ash-base`; resolve conflicts intentionally before switching scheduled builds to `UPSTREAM_REF=master`. Current conflict set: `README.md`, locale `strings.xml` files, `smarttubetv/build.gradle`, and `SignInFragment.java`. |
| P1 | GitHub Issues are disabled for `e1ch/SmartTube`. | External setting | Enable Issues in repository settings before using issue templates or automated issue filing. |
| P1 | Third-party sources need shared-player provider architecture. | Started | 8movie overlay is the first provider; future providers must use the same `ExternalVideoProvider` contract. |
| P1 | Google account sync, provider cookies, and anonymous discovery must not share state. | Design documented | Convert `docs/account-sync-startup-order.md` into runtime checks after the provider path stabilizes. |
| P2 | Upstream watch only reports drift. | Open | Add automatic PR/issue creation after GitHub Issues are enabled and conflict behavior is agreed. |
| P2 | Original SmartTube issues were not being tracked by Ash CI. | Fixed in this branch | `scripts/generate-upstream-issues-report.sh` and `docs/upstream-issue-triage.md` now track upstream account/search/startup/playback issues. |

## Next Milestones

1. Merge this branch after `Patch Validation / ash-base` passes.
2. Enable GitHub Issues in repository settings.
3. Run the release workflow manually with `upstream_ref=ash-base`.
4. Resolve latest-upstream patch conflicts in a separate branch.
5. Add runtime provider feature flags before enabling third-party sources by default.
6. Use `docs/upstream-issue-triage.md` before changing login, search, startup, or player code.
