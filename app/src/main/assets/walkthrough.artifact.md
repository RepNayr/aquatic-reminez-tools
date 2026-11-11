# Walkthrough — Fixes for Episode Ordering, Fast Preview Pre-resolution, Thumbnails & Download Pipe

## Overview
We have resolved all 3 issues reported in the latest feedback:

1. **Urutan Episode & Label Episode**:
   - Diperbaiki pada `HentaiCopParser.kt`: Sekarang membaca atribut `data-num` dan `<span class="ht-eplist__num">` asli dari website HentaiCop. Episode 1 dan Episode 2 sekarang persis sesuai dengan urutan dan penomoran asli web HentaiCop.
   - Diperbaiki label episode hitam di atas judul (`EPISODE 1`, `EPISODE 2`) pada `item_video_episode.xml` & `VideoEpisodeAdapter.kt`.

2. **Thumbnail Cuplikan & Pemuatan Cepat Mini Player (Pre-resolution)**:
   - **Pemuatan Instan**: Di `VideoViewModel.kt`, ketika tombol "ANALYZE SERIES" ditekan, aplikasi secara otomatis melakukan pra-resolusi (`resolveDirectMediaSource`) untuk semua episode di latar belakang. Saat pengguna membuka Bottom Sheet, URL media dan header sudah siap di memori sehingga mini player dapat diputar secara instan tanpa perlu menunggu loading lama.
   - **Thumbnail Gambar**: Menambahkan header HTTP (`User-Agent` & `Referer: https://hentaicop.com/`) pada permintaan `Coil` di `VideoEpisodeAdapter.kt` sehingga thumbnail episode berhasil dimuat.

3. **Perbaikan Download Failed ("Respons server bukan file video") & Badge Tag**:
   - Di `HentaiCopParser.kt`, ditambahkan warm-up request ke referer halaman episode sebelum membuka `/unduh/...`, sehingga cookie sesi HentaiCop terisi dengan benar di `CrotPediaCookieJar`.
   - Di `VideoDownloadAdapter.kt`, badge tag pada daftar popup diubah dari `MANGA · PDF` menjadi `VIDEO · MP4`.

---

## Verifikasi
- **Unit Tests**: 79 unit tests lulus (`./gradlew app:testDebugUnitTest`).
- **Build**: Assemble debug sukses (`./gradlew app:assembleDebug`).
- **Git Push**: Perubahan telah di-commit dan di-push ke branch `fix/manga-downloader-hardening`.
