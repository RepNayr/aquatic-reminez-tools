# Walkthrough — HentaiCop Speed Acceleration, Moving-Window Speed, & Mini Player Poster

## Overview
We have resolved both issues reported in the latest feedback:

1. **Percepatan Unduhan HLS & Kecepatan Stabil**:
   - **Download Paralel (3x-5x Lebih Cepat)**: Di `VideoDownloadWorker.kt`, proses unduhan segmen HLS (`.m3u8`) sekarang mengunduh segmen secara **paralel dalam batch (3 koneksi simultan via Coroutines `async`)**, menghilangkan jeda latensi antar-segmen.
   - **Kecepatan Stabil (Moving-Window Speed)**: Perhitungan kecepatan unduh diubah dari rata-rata total kumulatif menjadi **kecepatan instan jendela waktu 500ms**, sehingga angka KB/s di UI selalu stabil, responsif, dan tidak terus menurun seiring berjalannya waktu.

2. **Gambar Thumbnail Episode & Poster Player Mini**:
   - **URL Absolut Thumbnail**: Di `HentaiCopParser.kt`, thumbnail episode sekarang menggunakan `absUrl("src")` sehingga URL lengkap seperti `https://hentaicop.com/thumb/d47364ccd91b65ba3d544fe1495b1bc5.jpg/...` selalu didapatkan dan kartu episode menampilkan gambar cuplikan video asli.
   - **Poster Thumbnail Mini Player**: Di `bottom_sheet_episode_config.xml` & `EpisodeConfigBottomSheet.kt`, menambahkan `ivPlayerPoster` yang menampilkan gambar thumbnail/poster episode sebelum video diputar, sehingga layar player tidak lagi hitam kosong (*blank black*). Begitu video diputar, gambar poster otomatis hilang.

---

## Verifikasi
- **Unit Tests**: 79 unit tests lulus (`./gradlew app:testDebugUnitTest`).
- **Build**: Assemble debug sukses (`./gradlew app:assembleDebug`).
- **Git Push**: Perubahan di-push ke branch `fix/manga-downloader-hardening`.
