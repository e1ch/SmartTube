# Account Sync and Startup Order

Ash must keep content discovery, Google login, cookie handling, and playback startup in a deterministic order. This prevents anonymous requests from polluting account state and prevents history/resume data from being saved under the wrong profile.

## Startup Order

1. Load local settings and feature flags.
2. Load provider cookie jars, but do not attach them to YouTube requests.
3. Initialize Google auth/session state.
4. Resolve the active Google profile id.
5. Load account-scoped stores: subscriptions, history, resume positions, playlists, and provider playback state.
6. Start anonymous discovery prewarm for Ash trending pools.
7. Start external provider discovery only after cookie scope is known.
8. Open the home screen.
9. Start playback only after the selected item has a resolved source and account scope.

## Google Account Sync Contract

Every account-backed write must include the active Google profile id:

- watch history
- resume position
- playlists
- subscriptions/cache snapshots
- "not interested" or filtering state when it is account-specific

If the profile id is unknown, write only to an anonymous local scope and migrate only after an explicit account match. Do not guess based on the last logged-in account.

## Search Contract

Ash discovery searches stay anonymous:

- use the `plainHttpClient` path
- do not attach OAuth interceptors
- do not attach YouTube cookies
- do not write to YouTube search history

User-initiated YouTube search may use the normal authenticated SmartTube path when the user is logged in. Third-party provider search uses only provider-scoped cookies.

## Playback Contract

Before launching playback, the app must know:

- source type: YouTube or external provider
- account scope: Google profile id or anonymous
- cookie scope: provider id if required
- media URL or resolver token
- queue context for next/previous episode

The player writes progress to the account-scoped store after this context is available. For 8movie and similar sources, next-episode playback should replace the media source inside the same player session rather than relaunching the page or activity.
