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


## Supported manga websites directory (below Download All)

- Add a **SUPPORTED WEBSITES** section immediately below the **DOWNLOAD ALL** button at the bottom of the Manga Downloader page.
- Present registered manga sources as a responsive two-column grid of compact rectangular tiles, inspired by the user's supplied platform-grid screenshot. Use a restrained thin outline, dark-theme-compatible contrast within the Manga Downloader's isolated palette, a small site icon/logo, and the site's display name. Keep corners moderately subtle (not pill-shaped or excessively rounded).
- The grid is a live directory of source adapters actually registered and enabled in the app. Do not show sites that have no implemented and validated adapter; do not use fake logos or imply unsupported sites work.
- Initial release: show **CrotPedia** as the sole supported website tile, because it is the only agreed and configured source.
- Tapping a supported site tile selects that source as the active provider and may show its supported host/domain. It must not silently navigate away from the Manga Downloader screen.
- URL analysis remains host-validated against the selected/registered adapter. A CrotPedia URL is accepted only by the CrotPedia adapter.
- When additional providers are implemented and tested later, register them in the source-adapter registry and let the grid render them from that registry; avoid hardcoding a separate tile for every site in the Activity layout.
- If only one source is available, keep the grid layout visually balanced without inventing placeholder providers or “coming soon” tiles.


## Supported websites grid — visual correction from reference

This section supersedes the earlier oversized/centered single-card mockup:

- Match the supplied reference's compact two-column platform tiles: two columns across the content width, short rectangular tiles, thin outline, modest internal padding, and a small horizontal gap. Do not render large cards, tall tiles, oversized icons, or a full-width provider card.
- Each tile uses the website's own recognizable icon/logo (prefer the site's official favicon or logo asset), positioned on the left, with the website name beside it in compact uppercase or title styling. Do not substitute a generic book icon when an official site icon can be obtained.
- Keep icon and label vertically centered and aligned consistently, matching the reference's platform buttons.
- Use a thin subtle blue-grey outline and the Manga Downloader's dark surface/foreground palette as appropriate to the final isolated theme. Corners should be only slightly rounded, not pill-shaped.
- Place the **SUPPORTED WEBSITES** heading and compact grid immediately below **DOWNLOAD ALL**. Preserve the reference's restrained scale and spacing.
- The grid is data-driven from validated registered adapters. Initially it contains only CrotPedia, using the actual CrotPedia site icon/logo. Do not add placeholder sites just to fill the second column; keep the single tile compact and aligned to the grid.


## Page composition correction — preserve the agreed original design

This is a narrow additive change, not a redesign. The latest generated full-screen mockup changed too much of the agreed Manga Downloader page. Do not use that mockup as the overall page layout reference.

Keep the previously agreed Manga Downloader screen exactly as specified:
- Dedicated screen with only a Back control and title at the top; no bottom navigation and no hamburger/sidebar.
- Keep the existing URL input, Analyze button, analyzed manga cover/title, and compact chapter list. Each chapter keeps its own right-aligned **DOWNLOAD** button; page-count/metadata remains on a secondary line below the chapter title.
- Keep the isolated Manga Downloader visual theme and existing spacing, typography, component sizing, and chapter presentation. Do not add unrelated history panels, feature cards, greeting panels, alternate navigation, or a second download-actions section.
- Keep the floating download trigger and its anchored Downloads pop-up behavior exactly as previously approved. This floating pop-up remains a separate download-manager overlay; it is not replaced by the inline queue section.

### Required content order at the bottom of the page

After the chapter list, render these elements in this exact order:

1. **DOWNLOAD ALL** — one full-width action below the chapter list. It enqueues all chapters in the displayed order.
2. **DOWNLOADS QUEUE** — a compact inline queue section immediately below Download All. This is the only newly requested addition to the original page. Show the queued/active chapter entries in small horizontal cards similar to the compact queue rows from the recent concept: optional small cover thumbnail, chapter title, source label (CrotPedia), state (Queued / Downloading / Completed / Failed), and a slim real progress bar plus percentage for the active item. Keep queued items visually compact; do not make large feature cards. The queue must reflect the same FIFO WorkManager queue as the floating Downloads pop-up; it is not a second queue and must not duplicate or simulate jobs. If empty, show a restrained empty-state line such as "No chapters in queue yet".
3. **SUPPORTED WEBSITES** — immediately below Downloads Queue. Use the previously specified compact two-column grid of short outlined rectangular tiles, with the official website icon/logo at the left and site name at the right. Initially show only CrotPedia. Do not use a large provider card or generic book icon when the actual site favicon/logo is available.

No other page sections should be inserted between Download All, Downloads Queue, and Supported Websites. The section order above supersedes the earlier instruction that placed Supported Websites directly below Download All.


## Queue behavior and visual reference refinement (user-approved, 2026-11-07)

Use the user's latest dark-screen reference only for the **queue card composition and supported-site tile treatment**. Do not copy its dark background, bottom navigation, or overall page structure. The Manga Downloader keeps its previously approved warm off-white / paper background and the original light design screenshot.

### Global queue behavior across different manga

- The queue is global to the Manga Downloader, not scoped to the currently analyzed manga/series.
- If a chapter from Manga A is currently downloading and the user analyzes Manga B and taps Download on one of its chapters, append that chapter to the existing FIFO queue. Do not interrupt, replace, or restart the active Manga A chapter.
- The currently active chapter continues until its PDF is completely generated, optimized, verified, and saved (or fails/cancels). Only then may the next queued chapter start.
- Example: Manga A — Chapter 1 is downloading; user requests Manga B — Chapter 3. The queue shows Manga A Chapter 1 as **Downloading** and Manga B Chapter 3 as **Queued**. When A Chapter 1 finishes, B Chapter 3 becomes active automatically.
- Download All appends all chapters of the currently analyzed manga to the end of the same global queue, preserving the chapter order displayed. Individual Download appends only that chapter.
- Prevent duplicate active/queued jobs for the same normalized chapter URL unless the previous job has completed/failed/cancelled and the user explicitly retries it.
- Persist each queue entry's series title, chapter title, source/provider ID, chapter URL, stable queue ID, enqueue order, state, progress, and output URI so queue survives navigation, Activity recreation, and process death.
- Strictly one active chapter at a time across all manga. No parallel chapter downloads. A failed or cancelled item must not silently block later queue items; show its final state and continue according to retry policy.

### Inline Downloads Queue visual

- Follow the latest user reference for the queue row composition: compact horizontal cards, small square cover thumbnail at left, manga title and chapter/source subtitle in the middle, and state/progress at right or below.
- For the active row, show a slim progress bar and percentage plus a clear **Downloading** state. A pause/resume control may be shown only if it is actually wired to the worker.
- For waiting rows, show **Queued** without a fake progress percentage. Completed rows may show **Completed** with a check icon; failed rows show a concise failure state.
- Keep cards compact and aligned with the original page's margins. Use the original Manga Downloader warm off-white background, dark text, subtle neutral borders, and restrained shadows. The dark background from the latest reference is not part of this design.
- The inline queue and floating Downloads pop-up are two views of the exact same persisted global queue state. Changes and progress must stay synchronized.
- When there are no pending/active jobs, show a compact empty state; completed history should not be mixed into the active queue unless a separate history behavior is explicitly designed.

### Supported websites visual

- Use the latest reference's compact tile composition (actual site logo/icon at left, site name at right, thin outlined horizontal rectangle), but keep the original warm off-white page palette.
- Keep **SUPPORTED WEBSITES** directly below the inline queue, with only validated registered providers. Initial tile: CrotPedia.
