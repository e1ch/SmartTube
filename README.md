# SmartTube Ash (e1ch Fork)

**An enhanced YouTube client for Android TV** — breaking filter bubbles with real content discovery powered by 5 external trending sources.

[English](#why-this-fork) | [繁體中文](#為什麼要做這個-fork) | [日本語](#このforkの理由) | [한국어](#이-fork를-만든-이유)

---

## Why This Fork

YouTube TV's recommendation algorithm increasingly traps users in **filter bubbles** — showing the same types of content based on watch history, with no way to discover new topics. In some regions, the home feed has become extremely repetitive, displaying only a narrow slice of what YouTube actually has. There is no transparency in how recommendations are chosen; users have no real control.

This fork adds **independent content discovery** by pulling from multiple real data sources (Google Trends, GDELT, TikTok, Reddit, Wikimedia, YouTube Charts, kworb, localized search pools) and mixing them into a single "For You ✦" shelf — giving users access to content YouTube's algorithm would never show them.

## 為什麼要做這個 Fork

YouTube TV 的推薦演算法越來越把用戶困在**同溫層**裡 — 根據觀看紀錄只推送同類型內容，完全沒有辦法探索新主題。在部分地區，首頁內容已經變得極度重複，只顯示 YouTube 實際擁有內容的很小一部分。推薦的邏輯完全不透明，用戶無法真正掌控。

這個 Fork 透過從多個真實數據來源（Google Trends、GDELT、TikTok、Reddit、維基百科、YouTube 排行榜、kworb 即時熱門、多語言搜尋池）拉取內容，混合成一個「為你推薦 ✦」清單，讓用戶能接觸到 YouTube 演算法永遠不會推送給你的內容。

## このForkの理由

YouTube TVのレコメンドアルゴリズムは、視聴履歴に基づいて同じ種類のコンテンツばかりを表示する**フィルターバブル**にユーザーを閉じ込めます。一部の地域では、ホームフィードが極端に偏り、新しいトピックを発見する方法がありません。

このForkは、Google Trends、GDELT、TikTok、Reddit、Wikimedia、YouTube Charts、kworb、多言語検索プールなど複数のデータソースから独立したコンテンツ発見機能を追加し、「For You ✦」シェルフとしてまとめて表示します。

## 이 Fork를 만든 이유

YouTube TV의 추천 알고리즘은 시청 기록을 기반으로 같은 유형의 콘텐츠만 보여주는 **필터 버블**에 사용자를 가둡니다. 일부 지역에서는 홈 피드가 극도로 반복적이며 새로운 주제를 발견할 방법이 없습니다.

이 Fork는 Google Trends, GDELT, TikTok, Reddit, Wikimedia, YouTube Charts, kworb, 다국어 검색 풀 등 여러 실제 데이터 소스에서 콘텐츠를 가져와 "For You ✦" 선반으로 통합 표시합니다.

---

## How It Works — Architecture

### Data Flow

```
App Launch (SplashActivity)
  │
  ├── Phase 0: Load trending keyword cache (SharedPreferences, instant)
  ├── Phase 1: Prewarm connections (Auth, TLS, PoToken)
  └── Phase 2: Background refresh trending keywords (non-blocking)
        ├── Google Trends RSS → keywords
        ├── GDELT JSON API → keywords
        ├── TikTok Creative Center → hashtags
        ├── Reddit r/popular → topics
        └── Wikimedia Most Read → article titles

Home Tab Load (BrowsePresenter → BrowseService2.streamHomeExtra)
  │
  ├── Phase 1: YouTube TV API (fast, ~3s) — default recommendations
  ├── Phase 2: YouTube Charts + kworb trending (external data)
  ├── Phase 3: Static search pools A-E (localized queries from strings.xml)
  ├── Phase 4: Dynamic trending keywords → YouTube search queries
  └── Phase 5: Merge, dedupe, rank, shuffle → "For You ✦" shelf
```

### Anonymous Search — Algorithm Safety

> **Your YouTube account is NOT affected.** All discovery searches use a completely separate, unauthenticated HTTP pipeline.

| Aspect | How It's Protected |
|--------|-------------------|
| **HTTP Client** | `plainHttpClient` — built WITHOUT OAuth interceptors, no auth headers sent |
| **API Format** | Uses `clientName: "WEB"` (not `"TVHTML5"`) — YouTube treats these as anonymous web searches |
| **Search History** | Discovery queries are **never recorded** in your YouTube search history |
| **Watch History** | Your watch history is unaffected — you choose what to click |
| **Recommendations** | YouTube's recommendation algorithm **does not see** these searches |
| **External Sources** | Google Trends, GDELT, TikTok, Reddit, Wikimedia — all fetched directly, never through YouTube's authenticated API |
| **Local Filtering** | Watched video exclusion happens **on-device only** — never sent to YouTube |

**Technical proof**: The `plainHttpClient` in [`BrowseService2.kt`](MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/browse/v2/BrowseService2.kt) (line 27-35) is constructed without `RetrofitOkHttpHelper.interceptors`, which contain the OAuth token injector. All trending fetchers in the [`trending/`](MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/trending/) package receive this same anonymous client.

### Risk Disclosure

- **No algorithm contamination**: Discovery searches cannot affect your YouTube recommendations because they never carry your authentication credentials.
- **No search history leakage**: YouTube cannot associate these queries with your account.
- **Network traffic**: The app makes additional HTTP requests to Google Trends, GDELT, TikTok Creative Center, Reddit, and Wikimedia APIs. These are public, unauthenticated requests. Your IP address is visible to these services (same as visiting their websites in a browser).
- **Content accuracy**: Trending keywords are converted to YouTube search queries. Results depend on YouTube's search algorithm and may not always match the original trending topic exactly.

---

## Trending Sources — The "Trending Pool"

### Overview

The trending pool fetches real-time keywords from 5 external sources, converts them to YouTube search queries, and injects results into the home screen. All sources are fetched anonymously.

| # | Source | Data Type | Update Frequency | Coverage | API/Method |
|---|--------|-----------|-----------------|----------|------------|
| 1 | **Google Trends** | Search trends | ~10 min (fetched every 15 min) | 100+ countries | RSS feed: `trends.google.com/trending/rss?geo={CC}` |
| 2 | **GDELT** | Global news events | 15 min | 109 languages | JSON API: `api.gdeltproject.org/api/v2/doc/doc` |
| 3 | **Wikimedia** | Most-read articles | Daily | 300+ languages | REST API: `wikimedia.org/api/rest_v1/metrics/pageviews/top` |
| 4 | **TikTok** | Trending hashtags | ~2 hours | Regional | Web scrape: `ads.tiktok.com/business/creativecenter` |
| 5 | **Reddit** | Popular topics | ~2 hours | Global (English-heavy) | Public JSON: `reddit.com/r/popular/hot.json` |

### Verification Layer (existing)

| Source | Purpose | Method |
|--------|---------|--------|
| **YouTube Charts** | Official YouTube trending data | `charts.youtube.com` API (WEB_MUSIC_ANALYTICS client) |
| **kworb** | Real-time YouTube trending video IDs | Scrapes [kworb.net/youtube/trending/](https://kworb.net/youtube/trending/) |

### Source Files

All trending fetcher code is in:
```
MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/trending/
├── TrendingFetcher.java          # Interface for all fetchers
├── TrendingKeyword.java          # Data class for a single keyword
├── TrendingKeywordManager.java   # Cache, TTL, source orchestration
├── GoogleTrendsFetcher.java      # Google Trends RSS parser
├── GdeltFetcher.java             # GDELT DOC 2.0 JSON parser
├── WikimediaMostReadFetcher.java # Wikimedia pageviews API
├── TikTokTrendsFetcher.java      # TikTok Creative Center scraper
└── RedditTrendsFetcher.java      # Reddit r/popular JSON parser
```

---

## "For You ✦" — Content Discovery

The unified discovery shelf pulls from **11 independent sources** and shuffles them into a single row of 110+ videos:

| Source | Content Type | How It Works |
|--------|-------------|--------------|
| **Google Trends** | Real-time search trends | RSS feed, converted to YouTube search queries |
| **GDELT** | Global breaking news | JSON API, article titles as search queries |
| **TikTok** | Viral hashtags | Creative Center trending hashtags → YouTube search |
| **Reddit** | Popular discussion topics | r/popular hot posts → YouTube search |
| **Wikimedia** | Most-read articles | Yesterday's top articles per language → YouTube search |
| **YouTube Charts** | Official top views + trending | `charts.youtube.com` API, per-country |
| **kworb Trending** | Real-time mixed trending | Scrapes kworb.net video IDs, fetches metadata via `/player` |
| **Language Discovery** | Locale-specific fresh content | Searches high-frequency stop words with THIS_WEEK filter |
| **Search Pool A-C** | Music, Lifestyle, Mixed | Localized queries across 47 languages |
| **Search Pool D** | Movies, Travel, Knowledge | Break filter bubble — topics algorithm rarely suggests |
| **Search Pool E** | Anime, Animation, Manga | Dedicated pool for anime/animation fans |

### How It Breaks Filter Bubbles

1. **Multiple independent sources** — not relying on YouTube's recommendation engine
2. **Random shuffle** — every refresh shows different mix, no fixed order
3. **Channel diversity** — max 2 videos per creator per shelf
4. **Ranking engine** — heuristic scoring penalizes topic saturation, rewards novelty
5. **Watched video filtering** — already-seen content excluded automatically
6. **Pool rotation** — 5 query pools (A-E) cycle on each refresh for maximum variety
7. **Real-time trending** — Google Trends + GDELT inject current events every 15 minutes

---

## Content Pools & Search Keywords

### Static Pools (localized in 47 languages)

Search queries are defined in Android string resources. Each pool has 3 queries per language (15 queries total across 5 pools).

**File locations:**
- English (default): [`common/src/main/res/values/strings.xml`](common/src/main/res/values/strings.xml) (lines 756-817)
- Chinese (Traditional): [`common/src/main/res/values-zh-rTW/strings.xml`](common/src/main/res/values-zh-rTW/strings.xml)
- All 47 locales: `common/src/main/res/values-{locale}/strings.xml`

**String resource keys:**
```
query_home_a1, query_home_a1_title  — Pool A, query 1
query_home_a2, query_home_a2_title  — Pool A, query 2
query_home_a3, query_home_a3_title  — Pool A, query 3
query_home_b1..b3                   — Pool B
query_home_c1..c3                   — Pool C
query_home_d1..d3                   — Pool D
query_home_e1..e3                   — Pool E
query_trending_1..3                 — Trending tab queries
```

| Pool | Content | Example (en) | Example (zh-TW) | Example (ko) |
|------|---------|--------------|------------------|--------------|
| A | Music | official music video new release | 新歌 MV 官方 | 최신 뮤직비디오 |
| B | Lifestyle | tech review unboxing | 美食 推薦 vlog | 맛집 브이로그 |
| C | Mixed | gaming highlights | 今天 熱門 話題 | 오늘 인기 화제 |
| D | Exploration | explained science history | 紀錄片 科學 知識 | 다큐 과학 지식 |
| E | Anime | anime new season trailer | 動漫 新番 推薦 | 애니 신작 추천 |

### Dynamic Trending Keywords

Trending keywords are fetched at runtime and cached in SharedPreferences:
- **Cache file**: `trending_cache` SharedPreferences (key: `trending_json`)
- **Cache TTL**: 30 minutes
- **Manager**: [`TrendingKeywordManager.java`](MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/trending/TrendingKeywordManager.java)

### Settings

Users can control discovery via **Settings > General > Discovery**:

| Setting | Options | Default |
|---------|---------|---------|
| Discovery Mode | Unified / Rotating / All Expanded | Unified |
| Content Pools | A (Music), B (Lifestyle), C (Mixed), D (Exploration), E (Anime), Language | All enabled |
| Trending Sources | Google Trends, GDELT, Wikimedia, TikTok, Reddit | All enabled |
| Playback Optimization | Auto / Force Optimize / Full Quality | Auto |

---

## Update Channel

### SmartTube Ash (this fork)

All updates are distributed exclusively through **GitHub Releases**.

- Update manifest: [`update/ash.json`](update/ash.json)
- Per-ABI APKs: `github.com/e1ch/SmartTube/releases/download/release/SmartTube_beta_{abi}.apk`
- App checks `raw.githubusercontent.com` for version info, downloads directly from GitHub

### Migrating from Legacy Beta

If you are currently running a legacy beta version (31.7x), you must **manually install** the Ash version once. After installation, future updates will be automatic via `ash.json`.

1. Download the APK for your device architecture from [Releases](https://github.com/e1ch/SmartTube/releases)
2. Install via `adb install -r` or sideload on your TV
3. The app will automatically check `ash.json` for future updates

### Version Format

```
{upstream_version}-AS.{commit_hash}
Example: 31.46-AS.7216816
```

- `31.46` = based on upstream SmartTube version 31.46
- `AS` = Ash identifier
- `7216816` = 7-character git commit hash of the fork

Version tracking: [`ash-base-version.properties`](ash-base-version.properties)

---

## Search — Streaming Parser

User search uses a custom **JsonReader streaming parser** instead of the default JsonPath library:

| | JsonPath (original) | JsonReader (this fork) |
|---|---|---|
| Parse method | Full tree materialization + path evaluation | Single-pass token stream scan |
| 190KB JSON on TV | ~12 seconds | ~7 seconds |
| Memory | Entire JSON in memory as tree | Token-by-token, minimal allocation |

---

## Localization — 47 Languages

Search queries are localized in Android string resources across **47 languages** with 5 content pools (15 queries per language).

**Help improve localization**: If you notice search quality issues in your language, please [open an issue](https://github.com/e1ch/SmartTube/issues) with your language, the query, and what you expected.

---

## Performance

- **Search**: Streaming JsonReader parser (12s → 7s on TV)
- **SplashActivity prewarm**: Auth + TLS connections pre-established during splash
- **Trending cache**: Loaded from SharedPreferences on cold start (instant), refreshed in background
- **QoS**: Playback-aware throttling (3→1 concurrent search during video playback)
- **Ranking engine**: Heuristic scoring (novelty boost + channel/topic diversity penalty)
- **Weak device optimization**: Auto-detected ARM32/Android ≤7.1/RAM ≤2GB → 720p30 cap, reduced buffers

---

## Build

```bash
# Requires JDK 17 (AGP 7.4 incompatible with JDK 24+)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17  # macOS example
./gradlew :smarttubetv:assembleStbetaDebug
```

APK output: `smarttubetv/build/outputs/apk/stbeta/debug/`

Install to TV:
```bash
adb install -r SmartTube_beta_31.46-AS.7216816_armeabi-v7a.apk
```

## Target Device

Tested on Xiaomi Mi TV (armeabi-v7a, Android 7.1.2)

---

## Acknowledgments

This project would not exist without the incredible foundation built by **[SmartTube](https://github.com/yuliskov/SmartTube)** and its creator **yuliskov**. SmartTube's architecture — the InnerTube API integration, Leanback UI framework, ExoPlayer pipeline, and the entire plugin system — is a remarkable piece of engineering that makes projects like this possible.

### Data Source Credits
- [Google Trends](https://trends.google.com/) — Real-time search trends (RSS)
- [GDELT Project](https://www.gdeltproject.org/) — Global news event monitoring (API)
- [Wikimedia](https://wikimedia.org/) — Most-read articles across 300+ languages (API)
- [TikTok Creative Center](https://ads.tiktok.com/business/creativecenter/) — Trending hashtags
- [Reddit](https://www.reddit.com/) — Popular discussion topics (public JSON)
- [kworb.net](https://kworb.net/) — YouTube trending video data
- [YouTube Charts](https://charts.youtube.com/) — Official chart data
- Claude, Gemini, and ChatGPT for development assistance and code review

---

## License

Same license as the original [SmartTube](https://github.com/yuliskov/SmartTube) project.
