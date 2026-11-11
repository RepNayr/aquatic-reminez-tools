# Walkthrough — Critical Fix: HentaiCop Video Preview & Full-Size Download Pipeline

## Overview
We have successfully fixed the two critical issues in the Private Video Downloader module:
1. **Mini Video Player**: Fixed ExoPlayer preview playback inside `EpisodeConfigBottomSheet` by introducing `ResolvedVideoSource` and configuring `DefaultHttpDataSource.Factory` with required `User-Agent` and `Referer` headers.
2. **Incomplete/Truncated Downloads**: Fixed premature `COMPLETED` marking of incomplete or 0.2 MB HTML/error responses by implementing strict `Content-Type` inspection, expected file size tolerance checks, and `MediaMetadataRetriever` media integrity verification in `VideoDownloadWorker`.

---

## Technical Fixes Made
1. **Exact Source Matching (`VideoRepository`)**: Removed silent fallback logic; now enforces exact server ID and quality label matching.
2. **Robust Redirect & Go URL Resolution (`HentaiCopParser`)**: Introduced `ResolvedVideoSource` data class to bundle direct media URL, referer, user-agent, expected size, and quality. Handled relative redirect/go URLs robustly using `URI(responseUrl).resolve(goUrl)`.
3. **ExoPlayer Preview (`EpisodeConfigBottomSheet`)**: Configured ExoPlayer with `DefaultHttpDataSource.Factory` passing User-Agent and Referer headers so streaming requests succeed without returning 403 Forbidden.
4. **Strict Download Validation (`VideoDownloadWorker`)**: Inspected HTTP response `Content-Type` to reject `text/html` or JSON error pages, validated file size against expected size, and verified media integrity using `MediaMetadataRetriever` before marking downloads completed.

---

## Verification Results
- **Unit Tests**: All 79 unit tests passed successfully (`./gradlew app:testDebugUnitTest`).
- **Build**: `./gradlew app:assembleDebug` completed successfully (`BUILD SUCCESSFUL`).
- **Git Push**: Committed and pushed changes to `origin/fix/manga-downloader-hardening`.
