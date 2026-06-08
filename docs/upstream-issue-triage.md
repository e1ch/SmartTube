# Original SmartTube Issue Triage

This file records upstream `yuliskov/SmartTube` issues that should guide Ash patch maintenance.

Checked on 2026-06-08 with `gh issue list/view --repo yuliskov/SmartTube`.

## P0: Account Identity and Sync

| Upstream issue | Why it matters to Ash |
|---|---|
| [#5857 Changing account name creates new not working profile](https://github.com/yuliskov/SmartTube/issues/5857) | Confirmed upstream bug. Ash must key local state by stable account id, not display name, before adding provider history/resume records. |
| [#5359 History isn't synced correctly](https://github.com/yuliskov/SmartTube/issues/5359) | Long thread showing Google watch progress can diverge from local resume state; update cadence and account/profile selection are suspect. |
| [#5111 Videos not marked as watched](https://github.com/yuliskov/SmartTube/issues/5111) | Related to watched-state persistence and "mark as watched"; directly affects Ash filtering and recommendation de-duplication. |
| [#5856 Remove from search history does not work](https://github.com/yuliskov/SmartTube/issues/5856) | Search history writes/deletes are not reliable; Ash anonymous discovery must stay separate from user search history. |
| [#4735 Separate settings per each account not working](https://github.com/yuliskov/SmartTube/issues/4735) | Confirms per-account settings isolation is weak. |

Ash implication: do not store provider history/resume/search state under mutable profile names. Require a stable account scope before writing account-backed state; otherwise use anonymous local scope.

## P0: Search Regressions

| Upstream issue | Why it matters to Ash |
|---|---|
| [#5780 Search results not showing up](https://github.com/yuliskov/SmartTube/issues/5780) | Search can fail only when logged in, which is exactly the boundary Ash discovery must avoid. |
| [#5849 Search shows playlists instead of video and channel](https://github.com/yuliskov/SmartTube/issues/5849) | Upstream work-in-progress builds mention search result fixes; Ash should not add more search coupling until this is stable. |
| [#5836 Search Bar not Working](https://github.com/yuliskov/SmartTube/issues/5836) | Recent reports still ask when the fix reaches devices. |
| [#5870 Only playlists on search](https://github.com/yuliskov/SmartTube/issues/5870) and [#5872 Keine Suchergebnisse](https://github.com/yuliskov/SmartTube/issues/5872) | Fresh reports indicate the search regression family is still active for some users. |
| [#5778 Search back returns to Home](https://github.com/yuliskov/SmartTube/issues/5778) | Activity stack/search navigation regression; relevant to third-party source detail/player return flow. |

Ash implication: third-party provider search should not reuse logged-in YouTube search request state. Provider rows should enter the shared player directly and preserve return context.

## P0: Startup and Settings Reliability

| Upstream issue | Why it matters to Ash |
|---|---|
| [#5868 SmartTube resetting UI / don't load settings](https://github.com/yuliskov/SmartTube/issues/5868) | Startup can forget recent settings/session state while account remains logged in. |
| [#5775 Starts replaying last video watched when application is relaunched](https://github.com/yuliskov/SmartTube/issues/5775) | Startup/player restoration order can relaunch stale playback. |
| [#5850 Crashing on opening](https://github.com/yuliskov/SmartTube/issues/5850) | Recent startup crash on older Android, likely tied to video preset/default playback setup. |
| [#5660 IllegalStateException / UnknownHostException](https://github.com/yuliskov/SmartTube/issues/5660) and [#5869 illegal state exception non stop](https://github.com/yuliskov/SmartTube/issues/5869) | Startup/network errors need graceful provider and YouTube separation. |
| [#5740 Cast devices appear only after playback](https://github.com/yuliskov/SmartTube/issues/5740) | Startup does not initialize all services before first playback. |

Ash implication: external providers must not start playback before account scope, provider cookies, and player initialization are ready.

## P1: Playback and Shared Player

| Upstream issue | Why it matters to Ash |
|---|---|
| [#5684 Unexpected playback error null](https://github.com/yuliskov/SmartTube/issues/5684) | Generic player error path is still fragile; external provider playback needs explicit errors instead of `null`. |
| [#5826 HDR10 buffer too small](https://github.com/yuliskov/SmartTube/issues/5826) | Upstream added quality fallback behavior; external sources should inherit shared-player fallback logic. |
| [#5767 invalid to call at released state](https://github.com/yuliskov/SmartTube/issues/5767) | Player lifecycle state transitions can break when changing quality or source. |
| [#5733 Video stops and skips to next video](https://github.com/yuliskov/SmartTube/issues/5733) | Queue/next-video logic is fragile; external next-episode logic must not piggyback on YouTube suggestions. |
| [#5575 Loading indicator never goes away](https://github.com/yuliskov/SmartTube/issues/5575) | Loader visibility can desync from actual playback state. |
| [#5361 Videos reloading issue](https://github.com/yuliskov/SmartTube/issues/5361) | Quality/HDR format selection can trigger reload loops. |

Ash implication: keep external playback inside the shared ExoPlayer path, but isolate provider source resolution and add explicit lifecycle guards before source replacement.

## Maintenance Rule

Before moving Ash from `ash-base` to latest upstream, check:

1. `scripts/generate-upstream-issues-report.sh`
2. `scripts/build-upstream-with-patches.sh` with `UPSTREAM_REF=master`
3. Whether upstream issues above have fixes already present in latest upstream and whether those fixes conflict with Ash patches
