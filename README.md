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
- **Progressive loading**: Phase 1 (TV recommendations, ~3s) shown instantly, Phase 2 (Charts + search) streams in
- **Multiple sources**: YouTube TV recommendations + YouTube Charts + kworb trending + localized search
- **Background prefetch**: Next refresh content pre-loaded during video playback
- **Channel diversity**: Max 2 videos per creator per shelf row
- **Watched video filtering**: Already-watched videos excluded from results
- **Refresh throttle**: Soft (<30s) / Medium (<120s) / Hard (>120s) prevents rate limiting

### kworb Trending
- Scrapes real trending video IDs from [kworb.net](https://kworb.net/youtube/trending/)
- Uses `/player` endpoint for metadata (100% match rate, ~2s for 20 videos)
- Random sampling from full list for variety on each refresh
- Fallback when YouTube Charts API unavailable

### Localization
- **48 languages**: Localized search query strings in Android string resources
- **Language/country independent**: `hl` and `gl` parameters set separately
  - Fixes: Traditional Chinese + Japan region no longer shows Simplified Chinese
- **Localized view counts**: `觀看次數：759萬次` (zh), `조회수 759만회` (ko), `759万回視聴` (ja)
- **Localized time ago**: `4天前`, `1週前`, `3個月前`

### Performance
- **SplashActivity prewarm**: TLS + PoToken initialized in background during splash
- **Search**: Semaphore (max 2 concurrent) + 200-400ms jitter to avoid rate limiting
- **OkHttp logging**: BODY → BASIC (fixes 256KB logcat buffer overflow on TV devices)
- **Auth skip for search**: WEB client format always used (fixes signed-in search returning null)

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
# Requires JDK 17
export JAVA_HOME=/path/to/jdk17
./gradlew assembleStbetaDebug
```

APK output: `smarttubetv/build/outputs/apk/stbeta/debug/`

## Target Device

Tested on Xiaomi Mi TV (armeabi-v7a, Android 7.1.2)

## Credits

- Original [SmartTube](https://github.com/yuliskov/SmartTube) by yuliskov
- [kworb.net](https://kworb.net/) for trending data
- YouTube Charts API for official chart data
