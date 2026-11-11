# Walkthrough — HentaiCop Video Downloader & Preview Hardening

## Overview
We have successfully resolved the root cause of the download failures and player/thumbnail issues:

1. **Penyebab Utama Download Failed & Solusi Unescaping `go` Link**:
   - Tautan `go` pada HTML HentaiCop ter-escape sebagai `href="...&#038;go=..."`. Tanpa Unescape `&#038;` menjadi `&`, OkHttp mengirim parameter query sebagai `q=720&#038;go=...` sehingga server HentaiCop mengabaikan parameter `go` dan mengembalikan halaman HTML unduh alih-alih `302 Found` ke lokasi media video.
   - Diperbaiki di `HentaiCopParser.kt` dengan melakukan unescape `replace("&#038;", "&")` pada `goUrl` dan URL lokasi redirect.

2. **Dukungan HLS `.m3u8` Segment Downloader**:
   - HentaiCop menyajikan video dalam format HLS playlist (`.m3u8`, contoh: `https://9385b44eef82e4b1ce998.site/hls/hc/_hepi/.../index-v1-a1.m3u8?md5=...`).
   - Diperbaiki di `VideoDownloadWorker.kt`: Menambahkan `downloadHlsPlaylistMedia` untuk mengunduh segmen TS/fMP4 secara berurutan dan menggabungkannya menjadi file video MP4 yang utuh.

3. **Urutan & Nomor Episode Asli HentaiCop**:
   - `HentaiCopParser.kt` membaca `data-num` dan `<span class="ht-eplist__num">` asli dari HTML HentaiCop sehingga urutan `EPISODE 1` dan `EPISODE 2` di aplikasi persis sama dengan halaman web aslinya.

4. **Thumbnail Kartu Episode & Pre-resolution Cepat**:
   - `VideoEpisodeAdapter.kt` menambahkan header HTTP (`User-Agent` & `Referer: https://hentaicop.com/`) pada `Coil` dan otomatis *fallback* ke `series.coverUrl` jika thumbnail episode kosong/gagal.
   - `VideoViewModel.kt` melakukan pra-resolusi tautan media video secara otomatis saat `ANALYZE SERIES` selesai sehingga Bottom Sheet memuat player mini secara instan.

---

## Verifikasi
- **Unit Tests**: 79 unit tests lulus (`./gradlew app:testDebugUnitTest`).
- **Build**: Assemble debug sukses (`./gradlew app:assembleDebug`).
- **Git Push**: Perubahan di-push ke branch `fix/manga-downloader-hardening`.
