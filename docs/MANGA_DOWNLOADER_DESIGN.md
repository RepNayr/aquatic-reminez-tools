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


## Private access and isolated visual theme

Manga Downloader is a private feature, not a normal catalog tool:

- Do not add it to `ToolData.ALL_TOOLS`, category lists, global tool search, or the home quick-menu.
- Add a discreet hidden entry gesture in Settings (proposed: long-press the existing app-version/about row). Do not add a visible category card or ordinary navigation item.
- The entry opens a dedicated Manga PIN gate. On first use, the user creates a separate six-digit Manga PIN; subsequent access requires that PIN.
- Store a salted PIN hash (never the raw PIN) using Android Keystore-backed encrypted preferences. Apply attempt throttling/temporary lockout. Do not reuse the global app-lock PIN unless the user explicitly chooses that later.
- `MangaActivity` must enforce authorization itself, not merely trust the Settings navigation path. Deep links, explicit intents, Activity recreation, and returning from background must not bypass the gate. Re-prompt after leaving the feature or after a short inactivity timeout.
- The feature uses its own resource palette/theme and layouts; do not change the app-wide theme or settings.
- Visual direction from the supplied reference: warm off-white/paper background, dark charcoal Analyze button, simple URL input with clear/paste affordances, large cover preview, title, thin divider rules, clean chapter rows with right-aligned metadata, and compact bottom navigation. Use accessible contrast, touch targets, loading/empty/error states, and responsive scrolling rather than copying the screenshot pixel-for-pixel.

## Supplied parser test URLs

Series page:
`https://crotpedia.net/baca/series/imouto-no-tomodachi-ga-ichiban-kimochi/`

Chapter page:
`https://crotpedia.net/baca/musume-no-tomodachi-to-nakadashi-sex-shimakurimashita-chapter-1-bahasa-indonesia/`

Use these as initial parser fixtures/validation targets. Confirm current page structure and image URLs before finalizing CSS selectors; do not infer selectors from URL slugs.


## Revised hidden entry point and Manga Downloader screen UX

### Hidden entry point

- Place the concealed entry only inside **Settings → Security & Data Management**, in a new glassmorphism grid at the very bottom of that page.
- The grid must visually blend with ordinary settings shortcut tiles. Do not label it "hidden tools", "Manga", "Downloader", or otherwise reveal its purpose in visible text or accessibility announcements.
- Use a neutral, plausible decoy label/icon consistent with the surrounding settings shortcuts. The actual entry is activated by a deliberate gesture on that tile (for example, long-press); only then show the separate Manga PIN gate.
- Do not add an entry in the main Settings page, category catalog, global search, home shortcuts, or drawer.
- PIN gate is still mandatory; obscurity is not treated as security. The dedicated feature Activity must enforce authorization itself.

### Manga Downloader page layout

- No app bottom navigation, no hamburger/sidebar drawer. Use a simple top app bar with only a Back control and a concise page title.
- Keep the isolated warm paper/off-white visual theme from the supplied reference; the app's global theme remains untouched.
- URL field and prominent dark **ANALYZE** action appear first.
- After analysis, show cover preview and manga title.
- Render each chapter as a compact row. The chapter title is primary; page count / metadata sits on a secondary line directly underneath it.
- Put a per-chapter **DOWNLOAD** action at the far right of each row. Remove the previous "24 halaman / 32 halaman" right-side metadata treatment.
- Replace **ADD TO QUEUE** with **DOWNLOAD ALL**. Download All enqueues all available chapters in the displayed order; individual Download enqueues only that chapter. Both use the same sequential FIFO queue.
- Use restrained corner radii: softly rounded rectangles, neither pill-shaped/overly circular nor sharp corners. Target roughly 8–12dp for cards/fields/buttons and 6–8dp for small controls, adjusted to match the app's density and screenshot.

### Download progress presentation (modal overlay + floating toggle)

The download manager is a dedicated modal pop-up, not an always-visible inline card.

- When expanded, place a large white panel above the page, with a strong soft shadow, a thin/dark outline if needed for contrast, and moderately rounded corners. Dim the entire page behind it with a translucent dark scrim so focus moves to the panel.
- Panel header: cloud/download icon, **Downloads**, active-download count, and an **X** at the far right. This X closes/collapses the panel only; it does not cancel any jobs.
- Each download is a compact bordered card. First row: black category/quality chip (for example `MANGA · PDF` or `CHAPTER · PDF`), truncated chapter/file title with ellipsis, and a small **X** that cancels that specific job. Do not use TikTok labels in the Manga Downloader.
- Under the first row, show a thin horizontal progress bar: downloaded portion black, remaining portion light grey. Width must reflect real progress.
- Under the bar, left side shows downloaded size / total size and current transfer speed; right side shows percentage.
- The floating trigger sits near the bottom-left edge of the screen. It is a circular button with a relatively bold outline, cloud-download icon, subtle shadow/elevation, and a small badge at the upper-right showing the number of active downloads. When expanded, the modal panel must be anchored immediately above and visually associated with this trigger (not centered independently on the screen). Tapping the same floating trigger toggles the panel open/closed.
- The trigger remains available while jobs are active. Tapping the trigger once opens the anchored panel; tapping it again hides the panel. The panel-header X also hides it. Neither action cancels downloads. Reopening restores the panel with the latest live progress.
- Support multiple queued rows, but clearly mark one active chapter and the rest as queued. Process strictly sequentially; do not imply parallel transfer.
- When no job is active, hide the active-count badge or show zero consistently, and let the user reopen recent/completed results only if the UI explicitly supports that state.
- Progress values, byte counts, speed, and active count must come from actual WorkManager/network progress, never simulated values.
- Each cancel action must cancel only the corresponding queued/active chapter. Completed PDFs remain intact.

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


## Final download pop-up positioning reference (2026-11-07)

The latest user-supplied original screenshot is the authoritative reference for the download manager's placement. Refine the earlier “anchored immediately above” requirement as follows:

- The expanded Downloads panel must sit very close to the floating cloud-download trigger at the bottom-left. Avoid the previous mockup's excessive vertical gap.
- The panel's lower-left corner should visually point toward / connect to the trigger using a small speech-bubble tail or equivalent anchored shape. The trigger may slightly overlap the panel's lower-left shadow/edge, matching the reference.
- Treat the panel and trigger as one anchored floating component. Position the panel from the trigger's measured screen coordinates (or a shared parent/overlay anchor), not from screen center and not with a large fixed bottom offset.
- Preserve safe-area and navigation-bar insets; on short screens, constrain panel height and allow its item list to scroll while keeping the trigger reachable.
- Tapping the trigger toggles the panel. Tapping the header X closes only the panel. Neither action pauses or cancels downloads. The per-item X cancels only that item's job.
- When closed, the trigger remains visible with its active-job badge. When reopened, show current live progress.
- Match the reference's compact proportions: white/off-white panel, strong dark outline/shadow, thin dividers, compact bordered download rows, black progress fill on light-grey track, and no unnecessary empty spacing between panel and trigger.
- This final positioning reference supersedes any earlier mockup that places the panel centrally or noticeably far above the trigger.
