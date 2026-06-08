# Patch Series

This directory contains explicit overlay patches that are applied after the generated Ash fork delta.

`series.conf` is the source of truth for ordering. Do not rely on filesystem sort order for provider patches.

Current flow:

1. Clone original `https://github.com/yuliskov/SmartTube.git`.
2. Checkout the requested upstream ref, defaulting to `ash-base`.
3. Generate and apply the Ash fork delta from this repository.
4. Generate and apply submodule deltas for `SharedModules`, `MediaServiceCore`, and nested `MediaServiceCore/SharedModules`.
5. Apply patches listed in `patches/series.conf`.
6. Build APKs from the patched upstream tree.

Experimental providers must keep Google login, YouTube cookies, and provider cookies scoped separately.
