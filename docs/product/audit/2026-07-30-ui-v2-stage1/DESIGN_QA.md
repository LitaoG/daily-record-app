# Design QA — UI v2 Stage 1

> Status: historical design comparison evidence. This appendix records the comparison performed at the time and is not a current implementation target.

## Comparison target

- Source visual truth:
  - `docs/product/design/quiet-private-journal-v2/calendar-purple-red-v2.png`
  - `docs/product/design/quiet-private-journal-v2/statistics-purple-red-v2.png`
- Rendered implementation:
  - `docs/product/audit/2026-07-30-ui-v2-stage1/01-calendar-hand.png`
  - `docs/product/audit/2026-07-30-ui-v2-stage1/02-calendar-sex.png`
  - `docs/product/audit/2026-07-30-ui-v2-stage1/03-statistics-hand.png`
  - `docs/product/audit/2026-07-30-ui-v2-stage1/04-statistics-sex.png`
  - `docs/product/audit/2026-07-30-ui-v2-stage1/05-calendar-sex-font200.png`
  - `docs/product/audit/2026-07-30-ui-v2-stage1/06-statistics-sex-font200.png`
- Combined comparison evidence:
  - `docs/product/audit/2026-07-30-ui-v2-stage1/07-calendar-comparison.png`
  - `docs/product/audit/2026-07-30-ui-v2-stage1/08-statistics-comparison.png`

## Viewport and normalization

- Android Emulator: Pixel 4, API 34.
- Runtime capture: 1080×2280 physical pixels at 440 dpi, approximately 393×829 dp including system bars.
- Font scale: 1.0 and 2.0.
- Source images: 1705×923 and 1704×923 pixels, each containing two generated module states rather than an exact Android device raster.
- For the combined input, each runtime capture was proportionally resized to one source half-width and placed below the untouched source image. Aspect-ratio and real account-bar differences were kept visible instead of being distorted into a false pixel match.
- State: empty July 2026 calendar and empty current-week statistics, in both hand-brew and sex module selections.

## Scope boundary

This QA judges only Stage 1: palette, shared typography/spacing, strict 50/50 module selector, light period tabs, and bottom navigation. Calendar-cell geometry, record-page hierarchy, metric hierarchy, yearly heatmap, and trend visuals belong to Stages 2–4 and are not claimed as implemented here.

## Findings

No actionable P0, P1, or P2 differences remain within the Stage 1 scope.

- Fonts and typography: Android sans-serif hierarchy remains legible; module labels, four period labels, and two navigation labels remain visible at 200% text. Metric numerals now use an explicit monospace style.
- Spacing and layout rhythm: the runtime uses a consistent 20dp horizontal page margin. The selector is one layer, full width inside that margin, with exact equal halves. Period and bottom navigation selection use a restrained 2dp underline instead of nested filled pills.
- Colors and visual tokens: the implementation uses the brighter `#85569A` / `#603670` purple and clear wine `#A54658` / `#7A3040` red directions. White-on-primary and semantic text pairs meet WCAG AA in the token test.
- Image and icon fidelity: no raster product imagery is involved. The existing airplane and interlocking-ring Compose icons are retained and always paired with visible text.
- Copy and content: module names and period labels match the product truth. The runtime account bar contains real local-mode actions that are intentionally absent from the generated direction image.

## Focused-region evidence

Separate crops were not needed because both comparison sheets preserve the selector, period tabs, and bottom navigation at readable width. Geometry is additionally confirmed by semantics XML and `RecordModuleIntegrationTest`, including exact half bounds and 200% text touch sizes.

## Comparison history

1. Initial runtime comparison showed the bottom navigation with a full rectangular border. It was a minor P3 polish drift from the source.
2. The shared bottom bar was changed to draw only its top divider, and the normal/200% screenshots and combined comparison sheets were recaptured from the rebuilt APK.
3. A screenshot-pixel test initially sampled the pressed ripple after switching to the red module. The test was corrected to render each selected state directly; both static module colors then matched their exact token values. This was a test-timing artifact, not a product color change.

## Expected differences

- The real app shows the local account/diagnostic bar and Android system bars; the generated source uses a compact illustrative status.
- The current calendar cells and current week distribution card remain from the pre-v2 body implementation. They are intentionally deferred to Stage 2 and Stage 4.
- The source illustration is not an exact 390×844 raster and contains demo data, so it is not used to validate dates, statistics, or pixel coordinates.

## Implementation checklist

- [x] Approved neutral, purple, red, success, warning, and danger tokens.
- [x] Shared state colors for unset, explicit 0, 1, 2, 3+, focus, and disabled.
- [x] Strict single-layer 50/50 module selector.
- [x] Lightweight period tabs and bottom navigation.
- [x] 20dp page margins and 48dp minimum interactions.
- [x] Normal and 200% font screenshots.
- [x] Combined source/implementation comparison.
- [x] No remaining P0/P1/P2 Stage 1 findings.

## Follow-up polish

- Stage 2 will replace the current calendar-cell body without changing this shared shell.
- Stage 4 will replace the current statistics hierarchy and add the real-date annual heatmap.

final result: passed
