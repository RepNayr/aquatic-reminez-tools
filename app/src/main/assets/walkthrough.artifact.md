# Walkthrough — Fixes for Bottom Sheet Poster Hiding & Segment Retry

## Overview
We have resolved the issue where the bottom sheet mini player showed only the static poster image instead of the playing video:

1. **Poster Mini Player Hiding (`EpisodeConfigBottomSheet.kt`)**:
   - Diperbaiki pada `Player.Listener`: `ivPlayerPoster` sekarang langsung disembunyikan (`View.GONE`) begitu player mencapai `Player.STATE_READY` (saat video selesai dimuat dan siap diputar), sehingga video langsung tampil di bawah pemutar media tanpa terhalang gambar poster.

2. **Retry Segmen HLS (`VideoDownloadWorker.kt`)**:
   - Menambahkan mekanisme *retry* hingga 3 kali dengan jeda eksponensial per segmen HLS (`.ts`) untuk memastikan unduhan tidak gagal karena gangguan jaringan sementara pada segmen individual.

---

## Verifikasi
- **Unit Tests**: 79 unit tests lulus (`./gradlew app:testDebugUnitTest`).
- **Build**: Assemble debug sukses (`./gradlew app:assembleDebug`).
- **Git Push**: Perubahan di-commit dan di-push ke branch `fix/manga-downloader-hardening`.
