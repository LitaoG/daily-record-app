# Daily Record adaptive icon source

Status: `current asset handoff reference`
Last reviewed: 2026-08-16

This folder contains the approved neon source and the three-layer handoff from Figma.
Only canonical handoff files are kept here; generated previews and duplicate raster
exports are intentionally not versioned.

- `background.svg` — unrounded purple-to-red gradient layer.
- `foreground.svg` — transparent neon calendar, person, interlocking hearts and lock layer.
- `monochrome.svg` — single-color alpha mask with a black keyhole negative shape.
- `daily-record-icon-1254.png` — the user-approved full artwork source, kept at its original 1254×1254 resolution.
- `google-play-icon-512.png` — 512×512 sRGB Play listing icon, under 1 MB, with no pre-rounded corners.

The former `background.png`, `foreground.png`, `monochrome.png`, 1024px backup and
preview composites were derived exports, not inputs to the Android build or Figma
handoff, so they were removed during the 2026-08-11 resource cleanup. The previous
versions remain recoverable from Git history when an audit needs to compare them.

## Safe-area and fallback decisions

The Figma source is composed on a 108×108dp adaptive-icon canvas. The central artwork stays within the approximately 66×66dp safe area so launchers can apply their own mask and motion. Neither source layer contains a pre-rounded corner or external drop shadow.

Since ADR-022 (2026-11-05) the installed adaptive icon no longer uses this neon artwork. `res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml` and `ic_launcher_monochrome.xml` are hand-authored VectorDrawables: a flat violet background with a soft white sheen, two interlocking frosted rings and a small star, all inside the 66dp safe zone. The former WebP runtime layers were deleted from `drawable-nodpi`. The SVG/PNG files in this folder, including `google-play-icon-512.png`, describe the previous beta.3 icon and stay only as history until the Play listing icon is re-exported from the vector layers.

Figma source file: https://www.figma.com/design/WP2CrYM0XuAFOeojA46jZY
