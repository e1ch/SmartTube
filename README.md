# SmartTube (e1ch Fork)

Fork of [SmartTube](https://github.com/yuliskov/SmartTube) with enhanced content discovery, performance optimizations, and localization improvements.

## What's Different

### YouTube Charts Integration
- Official YouTube Charts API (`charts.youtube.com`) for real trending data
- **Top Charts**: Weekly top 100 videos by view count per country
- **Trending Now**: Real-time trending 30 videos (Right Now)
- **Top Songs**: Weekly top 100 songs per country
- Single API call returns full metadata (title, artist, thumbnail, view count, duration)
- Auto-fallback to previous weeks when current week videos are all watched

### Home Feed Enhancement
- **"For You ✦" unified shelf**: All discovery content (Charts + kworb + search) merged into one shuffled shelf
- **Progressive loading**: Phase 1 (TV recommendations, ~3s) shown instantly, Phase 2 streams in as single shelf
- **Multiple sources**: YouTube TV recommendations + YouTube Charts + kworb trending + localized search
- **Pool cache**: Persistent disk cache (30min TTL) for instant cold-start, no duplicate rows on refresh
- **Background prefetch**: Next refresh content pre-loaded during video playback
- **Channel diversity**: Max 2 videos per creator per shelf row
- **Watched video filtering**: Already-watched videos excluded from results
- **Refresh throttle**: Soft (<30s) / Medium (<120s) / Hard (>120s) prevents rate limiting
- **QoS**: Playback-aware search throttling (3→1 concurrent, higher jitter during video playback)

### kworb Trending
- Scrapes real trending video IDs from [kworb.net](https://kworb.net/youtube/trending/)
- Uses `/player` endpoint for metadata (100% match rate, ~2s for 20 videos)
- Random sampling from full list for variety on each refresh
- Fallback when YouTube Charts API unavailable

### Localization
- **47 languages**: Localized search query strings across 5 content pools (Music, Lifestyle, Mixed, Movies/Travel, Anime)
- **Language discovery**: Stop word search (e.g. "的", "는") for diverse fresh content per locale
- **Language/country independent**: `hl` and `gl` parameters set separately
  - Fixes: Traditional Chinese + Japan region no longer shows Simplified Chinese
- **Localized view counts**: `觀看次數：759萬次` (zh), `조회수 759만회` (ko), `759万回視聴` (ja)
- **Localized time ago**: `4天前`, `1週前`, `3個月前`

### Performance
- **SplashActivity prewarm**: TLS connections to `/player` and `/search` pre-established during splash
- **Search**: Semaphore (max 2 concurrent) + 200-400ms jitter to avoid rate limiting
- **kworb via /player**: 100% metadata match rate, ~2s for 20 videos (vs 60s via /search)
- **OkHttp logging**: BODY → BASIC (fixes 256KB logcat buffer overflow on TV devices)
- **Ranking engine**: Heuristic scoring (novelty + channel diversity + topic spread)

### QR Code Sign-In
- Custom vertical layout: QR code on top, user code below, centered
- Soft gray background (`#E8E8E8`) with 16dp rounded corners
- User code: auto-sized to fill QR width, single line
- Static QR (no refresh on code change — `yt.be/activate` doesn't support auto-fill)

### Update System
- Points to `e1ch/SmartTube` GitHub releases
- `update/beta.json` and `update/stable.json` with per-ABI download URLs
- Localized changelog (`changelog_zh`)

## Build

```bash
# Requires JDK 17 (AGP 7.4 incompatible with JDK 24+)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17  # macOS example
./gradlew :smarttubetv:assembleStstableDebug
```

APK output: `smarttubetv/build/outputs/apk/ststable/debug/`

Install to TV:
```bash
adb install -r SmartTube_stable_31.70_armeabi-v7a.apk
```

## Target Device

Tested on Xiaomi Mi TV (armeabi-v7a, Android 7.1.2)

## Credits

- Original [SmartTube](https://github.com/yuliskov/SmartTube) by yuliskov
- [kworb.net](https://kworb.net/) for trending data
- YouTube Charts API for official chart data
