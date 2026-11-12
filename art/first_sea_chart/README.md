# Grand Line First Sea chart

`Grand_Line_First_Sea.png` is the assembled M-key chart preview. It uses the existing First Sea world coordinates with a uniform north-up projection. The player and recommendation markers are examples for the UI.

## Assets for the M-key screen

- `textures/first_sea_background.png` is the 1440 × 768 ocean, grid, frame, and compass. Keep its aspect ratio in the UI.
- The 14 island sprites are individual 384 × 288 RGBA PNGs. Their basenames match the existing Grand Line island theme keys.
- `cloud_wide.png` and `cloud_tall.png` are transparent edge decorations.
- `first_sea_islands.json` lists the illustrated Grand Line islands, their themes, current world coordinates, and levels. Hollow Islet was omitted from this art pass as requested. Cloudtop Isles combines the two sky references.

In `WorldMapScreen`, keep the island anchors, player heading, recommendation, and any labels or tooltips data-driven. Use the preview as a guide for composition and relative sizes. The source dimensions of the replacement island PNGs are 384 × 288, so use those texture dimensions when blitting. Keep these theme assets in a First Sea subdirectory if they should apply only to that sea. These images illustrate landmarks and are suitable for the chart. Use measured screenshots of the playable Blox Fruits islands for world-generation dimensions.
