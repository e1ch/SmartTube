# SmartTube (e1ch Fork)

**An enhanced YouTube client for Android TV** — breaking filter bubbles with real content discovery.

[English](#why-this-fork) | [繁體中文](#為什麼要做這個-fork) | [日本語](#このforkの理由) | [한국어](#이-fork를-만든-이유)

---

## Why This Fork

YouTube TV's recommendation algorithm increasingly traps users in **filter bubbles** — showing the same types of content based on watch history, with no way to discover new topics. In some regions, the home feed has become extremely repetitive, displaying only a narrow slice of what YouTube actually has. There is no transparency in how recommendations are chosen; users have no real control.

This fork adds **independent content discovery** by pulling from multiple real data sources (YouTube Charts, kworb trending, localized search pools) and mixing them into a single "For You ✦" shelf — giving users access to content YouTube's algorithm would never show them.

## 為什麼要做這個 Fork

YouTube TV 的推薦演算法越來越把用戶困在**同溫層**裡 — 根據觀看紀錄只推送同類型內容，完全沒有辦法探索新主題。在部分地區，首頁內容已經變得極度重複，只顯示 YouTube 實際擁有內容的很小一部分。推薦的邏輯完全不透明，用戶無法真正掌控。

這個 Fork 透過從多個真實數據來源（YouTube 排行榜、kworb 即時熱門、多語言搜尋池）拉取內容，混合成一個「為你推薦 ✦」清單，讓用戶能接觸到 YouTube 演算法永遠不會推送給你的內容。

## このForkの理由

YouTube TVのレコメンドアルゴリズムは、視聴履歴に基づいて同じ種類のコンテンツばかりを表示する**フィルターバブル**にユーザーを閉じ込めます。一部の地域では、ホームフィードが極端に偏り、新しいトピックを発見する方法がありません。

このForkは、YouTube Charts、kworb、多言語検索プールなど複数のデータソースから独立したコンテンツ発見機能を追加し、「For You ✦」シェルフとしてまとめて表示します。

## 이 Fork를 만든 이유

YouTube TV의 추천 알고리즘은 시청 기록을 기반으로 같은 유형의 콘텐츠만 보여주는 **필터 버블**에 사용자를 가둡니다. 일부 지역에서는 홈 피드가 극도로 반복적이며 새로운 주제를 발견할 방법이 없습니다.

이 Fork는 YouTube Charts, kworb, 다국어 검색 풀 등 여러 실제 데이터 소스에서 콘텐츠를 가져와 "For You ✦" 선반으로 통합 표시합니다.

---

## "For You ✦" — Content Discovery

The unified discovery shelf pulls from **6 independent sources** and shuffles them into a single row of 110+ videos:

| Source | Content Type | How It Works |
|--------|-------------|--------------|
| **YouTube Charts** | Official top views + trending | `charts.youtube.com` API (WEB_MUSIC_ANALYTICS client), per-country, auto-fallback to previous weeks |
| **kworb Trending** | Real-time mixed trending | Scrapes [kworb.net](https://kworb.net/youtube/trending/) video IDs, fetches metadata via `/player` endpoint |
| **Language Discovery** | Locale-specific fresh content | Searches high-frequency stop words ("的", "는", "の") with THIS_WEEK filter for diverse results |
| **Search Pool A-C** | Music, Lifestyle, Mixed | Localized queries across 47 languages, rotated on each refresh |
| **Search Pool D** | Movies, Travel, Knowledge | Break filter bubble — topics YouTube's algorithm rarely suggests |
| **Search Pool E** | Anime, Animation, Manga | Dedicated pool for anime/animation fans |

### How It Breaks Filter Bubbles

1. **Multiple independent sources** — not relying on YouTube's recommendation engine
2. **Random shuffle** — every refresh shows different mix, no fixed order
3. **Channel diversity** — max 2 videos per creator per shelf
4. **Ranking engine** — heuristic scoring penalizes topic saturation, rewards novelty
5. **Watched video filtering** — already-seen content excluded automatically
6. **Pool rotation** — 5 query pools (A-E) cycle on each refresh for maximum variety

---

## Update Channel — GitHub Only

**All updates are distributed exclusively through GitHub Releases.**

The original SmartTube project experienced security concerns related to potential API key exposure through third-party update channels. To avoid similar risks, this fork uses GitHub as the **sole update channel**:

- Update manifest: `update/beta.json` and `update/stable.json` point to `github.com/e1ch/SmartTube/releases/download/release/`
- Per-ABI APKs uploaded to GitHub Releases (armeabi-v7a, arm64-v8a, x86, universal)
- No third-party update servers, no external manifest URLs
- App checks `raw.githubusercontent.com` for version info, downloads directly from GitHub

If you receive update prompts from any source other than this GitHub repository, **do not install them**.

---

## Search — Streaming Parser

User search uses a custom **JsonReader streaming parser** instead of the default JsonPath library:

| | JsonPath (original) | JsonReader (this fork) |
|---|---|---|
| Parse method | Full tree materialization + path evaluation | Single-pass token stream scan |
| 190KB JSON on TV | ~12 seconds | ~7 seconds |
| Memory | Entire JSON in memory as tree | Token-by-token, minimal allocation |

The streaming parser supports YouTube's current TV client format (`tileRenderer`, 2025+) as well as legacy formats (`compactVideoRenderer`, `videoRenderer`).

**Known limitation**: Parse time ~6s on 32-bit ARM Android 7 is hardware-bound. `JsonReader.skipValue()` must still scan through irrelevant JSON tokens. This is the fastest approach tested (alternatives: JsonPath 12s, string indexOf 22s, org.json + reflection 7.4s with parsing failures).

---

## Localization — 47 Languages

Search queries are localized in Android string resources across **47 languages** with 5 content pools (15 queries per language):

| Pool | Content | Example (zh-TW) | Example (ko) | Example (ja) |
|------|---------|------------------|--------------|--------------|
| A | Music | 新歌 MV 官方 | 최신 뮤직비디오 | 最新 MV 公式 |
| B | Lifestyle | 美食 推薦 vlog | 맛집 브이로그 | グルメ vlog おすすめ |
| C | Mixed | 今天 熱門 話題 | 오늘 인기 화제 | 今日 人気 話題 |
| D | Exploration | 紀錄片 科學 知識 | 다큐 과학 지식 | ドキュメンタリー 科学 |
| E | Anime | 動漫 新番 推薦 | 애니 신작 추천 | アニメ 新作 おすすめ |

**Help improve localization**: Some search queries may not return optimal results in all languages. If you notice issues with search quality in your language, please [open an issue](https://github.com/e1ch/SmartTube/issues) with:
- Your language and region
- The query that returns poor results
- What you expected to see

---

## All Features

### YouTube Charts Integration
- Official YouTube Charts API for real trending data
- Top Charts (weekly top 100 by views), Trending Now (real-time top 30), Top Songs
- Per-country results, auto-fallback to previous weeks when all watched

### Home Feed Enhancement
- **"For You ✦"**: 110+ videos from 6 sources, shuffled, cache-first display at top
- **Progressive loading**: Phase 1 (TV recommendations, ~3s) instant, Phase 2 (discovery) streams in
- **Pool cache**: Persistent disk cache (30min TTL) for instant cold-start
- **Quick re-entry**: Returning from search/player within 5 minutes keeps existing content (no blank flash)
- **QoS**: Playback-aware throttling (3→1 concurrent search during video playback)

### kworb Trending
- Real trending video IDs from [kworb.net](https://kworb.net/youtube/trending/)
- `/player` endpoint for metadata (100% match, ~2s for 20 videos)
- Random sampling for variety on each refresh

### Performance
- **Search**: Streaming JsonReader parser (12s → 7s on TV)
- **SplashActivity prewarm**: TLS connections pre-established during splash
- **OkHttp**: Profiler disabled, logging BASIC, shared connection pool
- **Ranking engine**: Heuristic scoring (novelty boost + channel/topic diversity penalty)

### QR Code Sign-In
- Custom vertical layout: QR code on top, user code below, centered
- Soft gray background with rounded corners

---

## Build

```bash
# Requires JDK 17 (AGP 7.4 incompatible with JDK 24+)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17  # macOS example
./gradlew :smarttubetv:assembleStstableDebug
```

APK output: `smarttubetv/build/outputs/apk/ststable/debug/`

Install to TV:
```bash
adb install -r SmartTube_stable_31.74_armeabi-v7a.apk
```

## Target Device

Tested on Xiaomi Mi TV (armeabi-v7a, Android 7.1.2)

---

## Acknowledgments

This project would not exist without the incredible foundation built by **[SmartTube](https://github.com/yuliskov/SmartTube)** and its creator **yuliskov**. SmartTube's architecture — the InnerTube API integration, Leanback UI framework, ExoPlayer pipeline, and the entire plugin system — is a remarkable piece of engineering that makes projects like this possible.

As someone coming from a **Quality Assurance** background rather than a professional developer, being able to build on such a solid codebase has been invaluable. SmartTube proves that open-source projects can achieve production-quality results, and this fork aims to contribute back by focusing on content discovery and testing quality.

### Additional Credits
- [kworb.net](https://kworb.net/) for trending data
- YouTube Charts API for official chart data
- Claude, Gemini, and ChatGPT for development assistance and code review

---

## License

Same license as the original [SmartTube](https://github.com/yuliskov/SmartTube) project.
