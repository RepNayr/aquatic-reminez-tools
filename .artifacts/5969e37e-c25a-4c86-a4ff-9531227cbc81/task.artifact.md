# Task List — Manga Downloader Hardening & Result System

- [ ] Create `MangaResultEntity.kt` and `MangaResultDao.kt`
- [ ] Update `MangaDatabase.kt` to version 6 with `MIGRATION_5_6`
- [ ] Enhance `PdfBuilder.kt` with page-count progress reporting and `compressExistingPdf`
- [ ] Update `MangaDownloadWorker.kt` to store PDF as `NOT_COMPLETED` result and handle compression jobs & HTTP 403
- [ ] Update `MangaRepository.kt` and `MangaViewModel.kt` for results & compression
- [ ] Create `item_manga_result.xml` and `MangaResultAdapter.kt`
- [ ] Update `activity_manga.xml` and `view_download_popup.xml` (remove completed tab, add result list below download all)
- [ ] Update `MangaActivity.kt` to wire result UI, compression dialog, save, remove
- [ ] Run unit tests and build project
