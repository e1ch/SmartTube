# SmartTube Fork Analysis (2026-04-09)

## Snapshot

| Item | Value |
|---|---|
| Fork HEAD | `49022cd5c` (`2026-04-06`) |
| Fork app version | `31.45s+AS.49022c` |
| Upstream HEAD | `77dc7a603` (`2026-04-08`) |
| Upstream visible version line | `31.45s-9-g77dc7a603` |
| Common base | `5e76847ac` (`2026-04-04`) |
| Divergence | `75 commits ahead / 26 commits behind` |

## Important Clarification

- `31.45s+AS.49022c` is the version of this fork, not the current upstream SmartTube version.
- Current upstream `master` is at `31.45s` plus 9 commits.
- The comparison that matters here is:
  - base snapshot: `5e76847ac`
  - our fork: `49022cd5c` / `31.45s+AS.49022c`
  - upstream current: `77dc7a603`

## What Upstream Added After The Fork Point

These are the 26 upstream-only commits since the split. Recommended merge priority is included.

| Priority | Upstream area | Commits | Why it matters |
|---|---|---|---|
| High | Player infinite loading fix | `77dc7a603` | Direct playback reliability fix. |
| High | Android 8 dialog/activity crash fix | `d37d701eb` | Stability on older devices. |
| High | Search provider crash before app init | `11d1985ef` | Prevents launcher/search integration crash. |
| High | VideoContentProvider authority cleanup | `229cf94a8` | Avoids manifest/provider mismatch risks. |
| Medium | Blocked channels stricter match | `fc6ab6ead` | Better filtering accuracy. |
| Medium | Chromecast crash fix | `4dde68762` | Useful if casting is still used by your users. |
| Medium | Browse/queue restore fixes | `4556c2f55` | UI behavior correctness. |
| Medium | App dialog header layout fixes | `5faa47171`, `bb1118671`, `29a354a72` | Cosmetic, but touches shared dialogs. |
| Low | HDR debug info | `89fc089e3` | Diagnostics only. |
| Low | OLED theme work | `c2be86ce6`, `60cf170e3`, `1ddb66ef6` | Nice-to-have theme improvements. |

## What This Fork Added Since The Fork Point

The fork work clusters into six themes.

| Theme | Main commits | Intent |
|---|---|---|
| Discovery / anti-filter-bubble home feed | `ba363396a` through `7426ad5b1` | Build a non-YouTube-driven discovery shelf with Charts, kworb, language discovery, and query pools A-E. |
| Search freshness and locale tuning | `5198607fc` through `932a35029` | Bias results toward recent uploads and local-language content. |
| Performance / home loading | `5f4e2bb68`, `08e7f53ed`, `aa39d449d`, `8d2e63396` | Progressive home, prefetching, shared connections, streaming parser. |
| Playback optimization | `90220026c`, `020042c49`, `dbbfa5e4e`, `8a76d4d86`, `c4ba11cb2` | Reduce weak-device startup latency and speed up `VideoInfo` parsing. |
| Sign-in UX | `a2a8c715c`, `38eec2868`, `1c46302a1`, `d03a1f28d` | Better QR code sign-in flow and readability. |
| Release / CI / update channel | `a0196ca09`, `cade16aa7`, `49022cd5c` | Move updates to GitHub releases and make APK signing consistent. |

## Pool Impact Assessment

### What the code does

- Discovery search uses a dedicated anonymous WEB client, not the normal signed-in TV client.
- The code explicitly says auth must be skipped for WEB search.
- Discovery items are mixed into a separate pool and shown as a custom shelf.

Relevant files:

- `MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/browse/v2/BrowseService2.kt`
- `common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/presenters/settings/GeneralSettingsPresenter.java`
- `common/src/main/java/com/liskovsoft/smartyoutubetv2/common/debug/DebugCommands.java`

### Conclusion

- Background pool generation itself should not directly write signed-in search history, because it uses anonymous WEB requests without the auth interceptor.
- However, if the user opens and watches videos that came from the discovery shelf, those watch events can still influence YouTube recommendations later. That part is normal account behavior and is not isolated by this fork.
- So:
  - fetching the pool: low algorithm contamination risk
  - watching discovered content: normal recommendation impact

## Comparison Table

| Area | Base version at fork point (`5e76847ac`) | Fork current (`31.45s+AS.49022c`, `49022cd5c`) | Upstream current (`77dc7a603`) |
|---|---|---|---|
| Home feed | Original SmartTube TV home rows | Progressive home + custom `For You ✦` discovery shelf + cache + pool rotation | Original SmartTube home behavior plus bug fixes |
| Trending | Stock SmartTube trending | Charts + kworb + locale-aware freshness logic | Stock trending with upstream fixes only |
| Search | Original parser / behavior | Streaming parser, locale strings, recent-upload bias | Search provider crash fix, no fork discovery logic |
| Playback startup | Original | Weak-device optimization, splash prewarm, PoToken/AppInfo/auth warmup, streaming `VideoInfo` parser | Infinite loading fix on some devices |
| Sign-in UI | Original QR screen | Larger, centered, styled QR and user code layout | No equivalent fork-specific QR redesign found |
| User controls | Original settings | Discovery mode selector, pool toggles, debug broadcast commands | No equivalent discovery controls |
| Release/update | Original update behavior | GitHub-only update channel, CI auto-release, local debug keystore consistency | No equivalent fork release pipeline |
| Localization | Upstream strings only | 46+ locale additions for discovery/search strings | Minor translation updates |

## Practical Merge Recommendation

1. Cherry-pick or manually port the 4 high-priority upstream fixes first.
2. Re-test playback, browse restore, search provider, and Android 8 launch path.
3. Merge medium-priority upstream fixes only after the player path is stable.
4. Keep discovery logic isolated from upstream changes where possible; the clean seam is `BrowseService2`, settings, and home presentation.

## Missing Claude Notes

- No dedicated Claude handoff file was found in this repository.
- `/Users/adamchen/.codex/memories` is empty in this environment.
- The reconstruction above is based on commit history, README claims, and current code paths.
