## Fork Changes

### Current focus

- `For You ✦` unified discovery shelf with Charts, kworb, language discovery, and localized query pools
- Streaming search and `VideoInfo` parsers for lower latency on weak TVs
- Playback startup optimization: splash prewarm, shared connections, deferred suggestion loading
- Discovery settings UI with per-pool toggles and debug commands
- GitHub-only update channel and auto-built APK releases

### Why this fork exists

This fork adds an independent content-discovery layer on top of SmartTube to reduce recommendation filter bubbles, while also targeting better playback startup time on low-end Android TV devices.

### Notes for this release train

- Fork versioning follows upstream base version plus fork suffix, for example `31.45s+AS.49022c`.
- Upstream fixes still need to be merged selectively from the current upstream branch.
