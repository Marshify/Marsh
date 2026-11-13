# Grand Line Gear · Batch 01

This pack contains 12 transparent 32 × 32 RGBA PNG item textures for Grand Line on Minecraft Java 1.20.1. Open `preview.png` to review the sprites. In `item_textures.json`, each entry pairs a Blox Fruits reference name with its PNG.

## Import into Grand Line

1. Match each reference name to the corresponding registered Grand Line item.
2. Copy the PNG to `src/main/resources/assets/grandline/textures/item/`. Name it after the registered item path or update the model to point at the filename you choose.
3. In the item's model JSON, use the texture resource location `grandline:item/<texture_filename_without_png>` for the icon's `layer0`. For a flat item model, follow the mod's existing `minecraft:item/generated` examples. Sword sprites can also be used with `minecraft:item/handheld` when the desired held pose calls for it.

The accessory PNGs are inventory sprites. Worn capes, coats, hats and rings require separate models or render layers. The Tomoe Ring design is a gold finger band for Grand Line's ring slot. Pink Coat is included as a legacy reference.
