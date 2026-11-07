# Manga Downloader — Technical Design

## Scope

Initial provider: CrotPedia only. Output: one PDF per chapter. Scheduling: sequential queue (one chapter at a time). Target PDF size: at most 20 MiB per chapter.

The downloader is intended for content the user is authorized to access and download. It must not bypass login restrictions, paywalls, CAPTCHAs, DRM, or other access controls.

## User flow

1. User pastes a CrotPedia series URL and taps **Analyze**.
2. The source adapter validates the host and extracts the series title and chapter links.
3. The UI displays the detected chapters, with select-all, select-none, and per-chapter selection.
4. User adds selected chapters to the queue.
5. WorkManager processes the queue sequentially. Each chapter is fetched, its page images are ordered, and a PDF is generated.
6. The PDF is optimized until it is <= 20 MiB. If it cannot meet the limit without unacceptable image degradation, the job is marked failed with a clear reason rather than silently dropping pages.
7. The result is saved through Android's Storage Access Framework (user-selected folder) and can be opened/shared.

## Proposed package layout

```
data/manga/
  model/MangaSeries.kt
  model/MangaChapter.kt
  model/MangaPage.kt
  source/CrotPediaSource.kt
  repository/MangaRepository.kt

domain/manga/
  PdfBuilder.kt
  PdfOptimizer.kt
  worker/MangaQueueWorker.kt
  worker/MangaWorkRequest.kt

presentation/manga/
  MangaActivity.kt
  MangaViewModel.kt
  MangaChapterAdapter.kt
  MangaQueueAdapter.kt

res/layout/
  activity_manga.xml
  item_manga_chapter.xml
  item_manga_queue.xml
```

## Responsibilities

- `CrotPediaSource`: Jsoup + the app's existing OkHttp client. It accepts only HTTPS URLs on the configured CrotPedia host, parses series metadata and chapter links, and parses chapter page image URLs. Relative URLs are resolved against the page URL. Requests use normal public page access and conservative timeouts.
- `MangaRepository`: source-facing API for analyze-series and load-chapter-pages. Keeps HTML parsing out of Activities.
- `MangaViewModel`: owns screen state (Idle, Analyzing, Ready, Enqueued, Downloading, Completed, Failed) and observes WorkManager progress.
- `MangaQueueWorker`: processes chapter requests in order. A single unique work chain / one worker owns the queue; no parallel chapter downloads. Persist queue metadata so the screen can be reconstructed after process death.
- `PdfBuilder`: creates a page-sized PDF from downloaded images. Use Android `PdfDocument` initially to avoid another dependency.
- `PdfOptimizer`: re-encodes images at controlled quality/resolution and rebuilds the PDF until it meets 20 MiB. Preserve page order and never omit a page to hit the size target.
- Storage: use `ACTION_OPEN_DOCUMENT_TREE` / `DocumentFile` or `ACTION_CREATE_DOCUMENT` so Android 10+ scoped storage is respected. Do not request broad storage permission.

## Queue semantics

- FIFO: selected chapters are queued in the order shown by the source (oldest-to-newest or newest-to-oldest, clearly labelled in UI).
- Exactly one chapter is processed at a time.
- A chapter is marked complete only after its PDF has been written and verified.
- Retry transient network errors a limited number of times; parsing errors and inaccessible chapters fail without infinite retry.
- Cancel affects the active work and remaining queue. Completed PDFs remain intact.
- Show chapter-level progress: downloading page N of M, building PDF, optimizing PDF, saving.

## Existing app integration points

- Register `media_manga_downloader` in `ToolData.ALL_TOOLS`.
- Mark it implemented only after the complete flow is wired and tested in `ToolImplementationHelper`.
- Add its drawable mapping to `ToolIconRegistry` only after the corresponding drawable asset exists; otherwise use the app's established fallback icon behavior.
- Route the tool from `SearchActivity` to `MangaActivity`.
- Add the Activity to `AndroidManifest.xml`.
- Reuse existing Jsoup, OkHttp, Lifecycle, RecyclerView, and WorkManager dependencies. No new dependency is required for the first implementation.

## CrotPedia parser validation gate

The parser must not ship with guessed CSS selectors. Before enabling downloads, validate selectors against a current, user-accessible sample series page and chapter page, and add parser tests with saved HTML fixtures. The public web fetch was not able to retrieve the previously supplied CrotPedia chapter URL during this design pass, so selectors and image ordering remain unverified.

## Acceptance criteria

- Reject non-CrotPedia URLs and malformed URLs with a useful message.
- Analyze a valid series and show a non-empty chapter list.
- Parse every page image for a chapter in the correct order.
- Queue multiple chapters and process them strictly sequentially.
- Survive Activity recreation and process interruption using persisted WorkManager work.
- Save a readable PDF for each successful chapter.
- Verify every output is <= 20 MiB, or report that the limit could not be met without dropping pages.
- Keep existing Image Compressor routing and behavior unchanged.
