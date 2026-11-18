# Fruit roster and save migration

The active roster has one fruit per approved icon in `art/fruit_icons/`. A fruit keeps its old registry id when it clearly replaces an old fruit, so inventories, eaten fruits, stash entries and mastery carry over unchanged.

## Active fruits (42)

| Fruit | Rarity | Registry id | Item id | Icon | Save migration |
|---|---|---|---|---|---|
| Rocket Fruit | Common | `grandline:rocket` | `grandline:rocket_fruit` | `rocket_fruit.png` | new fruit |
| Spin Fruit | Common | `grandline:spin` | `grandline:spin_fruit` | `spin_fruit.png` | renamed from `gale` (0.54.0, saves migrate) |
| Blade Fruit | Common | `grandline:blade` | `grandline:blade_fruit` | `blade_fruit.png` | new fruit |
| Spring Fruit | Common | `grandline:spring` | `grandline:spring_fruit` | `spring_fruit.png` | new fruit |
| Bomb Fruit | Common | `grandline:bomb` | `grandline:bomb_fruit` | `bomb_fruit.png` | renamed from `blast` (0.54.0, saves migrate) |
| Smoke Fruit | Common | `grandline:smoke` | `grandline:smoke_fruit` | `smoke_fruit.png` | renamed from `mist` (0.54.0, saves migrate) |
| Spike Fruit | Common | `grandline:spike` | `grandline:spike_fruit` | `spike_fruit.png` | new fruit |
| Flame Fruit | Uncommon | `grandline:flame` | `grandline:flame_fruit` | `flame_fruit.png` | renamed from `ember` (saves migrate) |
| Ice Fruit | Uncommon | `grandline:ice` | `grandline:ice_fruit` | `ice_fruit.png` | renamed from `frost` (0.54.0, saves migrate) |
| Sand Fruit | Uncommon | `grandline:sand` | `grandline:sand_fruit` | `sand_fruit.png` | renamed from `dune` (0.54.0, saves migrate) |
| Dark Fruit | Uncommon | `grandline:dark` | `grandline:dark_fruit` | `dark_fruit.png` | renamed from `umbra` (0.54.0, saves migrate) |
| Diamond Fruit | Uncommon | `grandline:diamond` | `grandline:diamond_fruit` | `diamond_fruit.png` | renamed from `quartz` (0.54.0, saves migrate) |
| Eagle Fruit | Uncommon | `grandline:eagle` | `grandline:eagle_fruit` | `eagle_fruit.png` | renamed from `feather` (0.54.0, saves migrate) |
| Light Fruit | Rare | `grandline:light` | `grandline:light_fruit` | `light_fruit.png` | renamed from `nova` (0.54.0, saves migrate) |
| Rubber Fruit | Rare | `grandline:rubber` | `grandline:rubber_fruit` | `rubber_fruit.png` | renamed from `bounce` (0.54.0, saves migrate) |
| Ghost Fruit | Rare | `grandline:ghost` | `grandline:ghost_fruit` | `ghost_fruit.png` | new fruit |
| Magma Fruit | Rare | `grandline:magma` | `grandline:magma_fruit` | `magma_fruit.png` | renamed from `lava` (0.54.0, saves migrate) |
| Quake Fruit | Rare | `grandline:quake` | `grandline:quake_fruit` | `quake_fruit.png` | renamed from `tremor` (0.54.0, saves migrate) |
| Spider Fruit | Rare | `grandline:spider` | `grandline:spider_fruit` | `spider_fruit.png` | new fruit |
| Buddha Fruit | Legendary | `grandline:buddha` | `grandline:buddha_fruit` | `buddha_fruit.png` | new fruit |
| Love Fruit | Legendary | `grandline:love` | `grandline:love_fruit` | `love_fruit.png` | new fruit |
| Creation Fruit | Legendary | `grandline:creation` | `grandline:creation_fruit` | `creation_fruit.png` | new fruit |
| Sound Fruit | Legendary | `grandline:sound` | `grandline:sound_fruit` | `sound_fruit.png` | renamed from `echo` (0.54.0, saves migrate) |
| Phoenix Fruit | Legendary | `grandline:phoenix` | `grandline:phoenix_fruit` | `phoenix_fruit.png` | new fruit |
| Portal Fruit | Legendary | `grandline:portal` | `grandline:portal_fruit` | `portal_fruit.png` | new fruit |
| Lightning Fruit | Legendary | `grandline:lightning` | `grandline:lightning_fruit` | `lightning_fruit.png` | renamed from `volt` (0.54.0, saves migrate) |
| Pain Fruit | Legendary | `grandline:pain` | `grandline:pain_fruit` | `pain_fruit.png` | new fruit |
| Blizzard Fruit | Legendary | `grandline:blizzard` | `grandline:blizzard_fruit` | `blizzard_fruit.png` | new fruit |
| Magnet Fruit | Legendary | `grandline:magnet` | `grandline:magnet_fruit` | `magnet_fruit.png` | new fruit |
| Gas Fruit | Legendary | `grandline:gas` | `grandline:gas_fruit` | `gas_fruit.png` | new fruit |
| Gravity Fruit | Mythical | `grandline:gravity` | `grandline:gravity_fruit` | `gravity_fruit.png` | renamed from `orbit` (0.54.0, saves migrate) |
| Mammoth Fruit | Mythical | `grandline:mammoth` | `grandline:mammoth_fruit` | `mammoth_fruit.png` | new fruit |
| T-Rex Fruit | Mythical | `grandline:t_rex` | `grandline:t_rex_fruit` | `t_rex_fruit.png` | new fruit |
| Dough Fruit | Mythical | `grandline:dough` | `grandline:dough_fruit` | `dough_fruit.png` | new fruit |
| Shadow Fruit | Mythical | `grandline:shadow` | `grandline:shadow_fruit` | `shadow_fruit.png` | renamed from `ink` (0.54.0, saves migrate) |
| Venom Fruit | Mythical | `grandline:venom` | `grandline:venom_fruit` | `venom_fruit.png` | renamed from `toxin` (0.54.0, saves migrate) |
| Spirit Fruit | Mythical | `grandline:spirit` | `grandline:spirit_fruit` | `spirit_fruit.png` | new fruit |
| Tiger Fruit | Mythical | `grandline:tiger` | `grandline:tiger_fruit` | `tiger_fruit.png` | new fruit |
| Yeti Fruit | Mythical | `grandline:yeti` | `grandline:yeti_fruit` | `yeti_fruit.png` | new fruit |
| Kitsune Fruit | Mythical | `grandline:kitsune` | `grandline:kitsune_fruit` | `kitsune_fruit.png` | new fruit |
| Control Fruit | Mythical | `grandline:control` | `grandline:control_fruit` | `control_fruit.png` | new fruit |
| Dragon Fruit | Mythical | `grandline:dragon` | `grandline:dragon_fruit` | `dragon_fruit.png` | new fruit |

## Removed fruits (v0.8.3)

The 24 retired fruits from the pre-0.8 roster were removed: `thorn`, `pebble`, `ash`, `tide`, `coral`, `iron`, `sludge`, `glass`, `rust`, `pollen`, `bloom`, `prism`, `monsoon`, `mirage`, `hornet`, `wolf`, `grizzly`, `tempest`, `solar`, `leviathan`, `aegis`, `hydra`, `titan`, `seraph`.

- Held or chested items of these fruits disappear when the world loads (Minecraft drops unknown items).
- On join, an eaten removed fruit is cleared (the player has no fruit) and removed fruits are dropped from the stash (`FruitService.cleanUpRemovedFruits`).
- Mastery XP stored under their ids is left in the profile but is never shown or used.
