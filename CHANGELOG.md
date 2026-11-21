# changelog

all notable changes to bunko, newest first.

## 1.1.0 — unreleased

updates in place over 1.0.0; your settings carry over untouched.

### new

- **statistics.** more → statistics shows library, chapter and tracker totals in one place, including how many entries the server will actually include in a global update.
- **status banners.** "offline", "downloaded only" and "incognito mode" now sit under the status bar on every screen. offline waits 1.5 s before appearing, so a momentary hiccup doesn't flash at you.
- **more ways to filter and sort.** filters: started, bookmarked. sorts: last read, latest uploaded chapter, latest fetched chapter, last update check, total chapters, downloaded chapters.
- **pinned global search.** all / pinned chips, for when you only care about the sources you pinned.
- **`.apk` and `.jar` extensions.** the file picker accepts both.
- **port auto-detection.** leave the port empty and bunko finds it on save. an explicit port, in the field or in the url, always wins.

### faster

- **cache-first loading.** the library and entry screens paint from a local cache and then catch up with the server. a failed refresh after a cache hit stays quiet.
- **schedule remembers anilist.** release statuses are kept for six hours, so reopening the tab no longer waits on anilist. pull-to-refresh skips the cache.

### polish

- **skeletons instead of spinners** on first loads for the library, entry, source browse, sources list and extensions.
- **continue reading** is a floating card above the bottom navigation. it collapses while you scroll and hides during selection and search.
- **animated bottom navigation icons**, plus smoother transitions and press feedback across lists.
- **truthful connection errors.** if both the primary and the fallback url fail, you see both messages; a missing password reads as "unauthorized" instead of a vague network error.
- **tab counts** only count titles actually in your library, and the last category you used is restored.

## 1.0.0

the first release. server-first suwayomi client: graphql, jetpack compose, dual-origin urls, per-entry filters, dynamic accents, offline chapters, trackers.
