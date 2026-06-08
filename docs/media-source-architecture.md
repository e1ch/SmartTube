# Third-Party Media Source Architecture

Ash discovery can index third-party video sources, but playback must still enter one shared player path. Do not create one-off embedded players per site.

## Source Adapter Contract

Each external source adapter should return a normalized item:

| Field | Meaning |
|---|---|
| `sourceId` | Stable provider id, for example `8movie`. |
| `contentId` | Provider-local movie or series id. |
| `episodeId` | Optional provider-local episode id. |
| `title` | Display title. |
| `thumbnailUrl` | Optional image shown in shelves/search. |
| `pageUrl` | Canonical provider page. |
| `playbackUrl` | Direct playable media URL or a resolver token. |
| `headers` | Optional request headers required by the media host. |
| `requiresCookie` | True when the provider requires a user-supplied cookie. |

Adapters may scrape or call public endpoints, but they must not leak Google OAuth tokens or YouTube cookies to external providers.

## 8movie Experimental Source

Use the same behavior proven in the browser userscript:

- inspect the live `https://8movie.com/play/...` page before relying on old selectors
- resolve episode media from page-local data
- switch episodes by replacing the active media source
- update the URL/state without full app navigation
- keep a visible remaining-episodes playlist for fast switching

The prior web flow used `#v` as the player container, CKPlayer source reconstruction, and same-page source swaps. Treat those as starting evidence, not a permanent API.

## Shared Player Requirement

All external media items must route through SmartTube's existing playback activity/player glue. The player should receive a normalized media item and headers, then choose ExoPlayer/SmartTube playback behavior normally.

Required playback behavior:

- one playback queue regardless of source
- resume position saved per `sourceId + contentId + episodeId`
- next episode advances inside the player
- account-specific history is written only after account identity is ready
- provider cookies stay scoped to that provider

## Cookie and Login Boundaries

- Google login controls YouTube account state only.
- External provider cookies are separate user-provided credentials.
- Anonymous third-party discovery must run without Google OAuth headers.
- When a provider requires cookies, fail with a clear "cookie required" state instead of silently falling back to unauthenticated scraping.

## Adding More Experimental Sources

Add providers behind a feature flag and keep them disabled by default until they pass:

1. source discovery works without blocking app startup
2. playback enters the shared player
3. cookies are scoped to the provider
4. episode switching does not relaunch the app
5. account history/resume records use the active Google profile id when available
