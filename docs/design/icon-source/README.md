# Daily Record adaptive icon source

This folder contains the approved neon source and the three-layer handoff from Figma:

- `background.svg` / `background.png` — unrounded purple-to-red gradient.
- `foreground.svg` / `foreground.png` — transparent neon calendar, person, interlocking hearts and lock.
- `monochrome.svg` / `monochrome.png` — single-color alpha mask with a black keyhole negative shape.
- `figma-handoff-preview.png` — review screenshot of the high-fidelity Figma handoff board.
- `adaptive-preview-1024.png` — local composite of the Android background and foreground layers for QA.
- `daily-record-icon-1254.png` — the user-approved full artwork source, kept at its original 1254×1254 resolution.
- `daily-record-icon-1024.png` — 1024×1024 sRGB backup/marketing source.
- `google-play-icon-512.png` — 512×512 sRGB Play listing icon, under 1 MB, with no pre-rounded corners.

## Safe-area and fallback decisions

The Figma source is composed on a 108×108dp adaptive-icon canvas. The central artwork stays within the approximately 66×66dp safe area so launchers can apply their own mask and motion. Neither source layer contains a pre-rounded corner or external drop shadow.

The Android adaptive icon uses optimized WebP layers in `drawable-nodpi` because the neon glow and fine anti-aliased edges are not preserved reliably by a hand-authored VectorDrawable at launcher size. The SVG exports and PNG source copies remain available for Figma editing; WebP is the lightweight runtime fallback and keeps the installed icon faithful to the approved source.

Figma source file: https://www.figma.com/design/WP2CrYM0XuAFOeojA46jZY
