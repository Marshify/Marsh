# Fruit roster and save migration

The active roster has one fruit per approved icon in `art/fruit_icons/`. A fruit keeps its old registry id when it clearly replaces an old fruit, so inventories, eaten fruits, stash entries and mastery carry over unchanged.

## Active fruits (42)

| Fruit | Rarity | Registry id | Item id | Icon | Save migration |
|---|---|---|---|---|---|
| Rocket Fruit | Common | `grandline:rocket` | `grandline:rocket_fruit` | `rocket_fruit.png` | new fruit |
| Spin Fruit | Common | `grandline:gale` | `grandline:gale_fruit` | `spin_fruit.png` | carried over from old `gale` |
| Blade Fruit | Common | `grandline:blade` | `grandline:blade_fruit` | `blade_fruit.png` | new fruit |
| Spring Fruit | Common | `grandline:spring` | `grandline:spring_fruit` | `spring_fruit.png` | new fruit |
| Bomb Fruit | Common | `grandline:blast` | `grandline:blast_fruit` | `bomb_fruit.png` | carried over from old `blast` |
| Smoke Fruit | Common | `grandline:mist` | `grandline:mist_fruit` | `smoke_fruit.png` | carried over from old `mist` |
| Spike Fruit | Common | `grandline:spike` | `grandline:spike_fruit` | `spike_fruit.png` | new fruit |
| Flame Fruit | Uncommon | `grandline:ember` | `grandline:ember_fruit` | `flame_fruit.png` | carried over from old `ember` |
| Ice Fruit | Uncommon | `grandline:frost` | `grandline:frost_fruit` | `ice_fruit.png` | carried over from old `frost` |
| Sand Fruit | Uncommon | `grandline:dune` | `grandline:dune_fruit` | `sand_fruit.png` | carried over from old `dune` |
| Dark Fruit | Uncommon | `grandline:umbra` | `grandline:umbra_fruit` | `dark_fruit.png` | carried over from old `umbra` |
| Diamond Fruit | Uncommon | `grandline:quartz` | `grandline:quartz_fruit` | `diamond_fruit.png` | carried over from old `quartz` |
| Eagle Fruit | Uncommon | `grandline:feather` | `grandline:feather_fruit` | `eagle_fruit.png` | carried over from old `feather` |
| Light Fruit | Rare | `grandline:nova` | `grandline:nova_fruit` | `light_fruit.png` | carried over from old `nova` |
| Rubber Fruit | Rare | `grandline:bounce` | `grandline:bounce_fruit` | `rubber_fruit.png` | carried over from old `bounce` |
| Ghost Fruit | Rare | `grandline:ghost` | `grandline:ghost_fruit` | `ghost_fruit.png` | new fruit |
| Magma Fruit | Rare | `grandline:lava` | `grandline:lava_fruit` | `magma_fruit.png` | carried over from old `lava` |
| Quake Fruit | Rare | `grandline:tremor` | `grandline:tremor_fruit` | `quake_fruit.png` | carried over from old `tremor` |
| Spider Fruit | Rare | `grandline:spider` | `grandline:spider_fruit` | `spider_fruit.png` | new fruit |
| Buddha Fruit | Legendary | `grandline:buddha` | `grandline:buddha_fruit` | `buddha_fruit.png` | new fruit |
| Love Fruit | Legendary | `grandline:love` | `grandline:love_fruit` | `love_fruit.png` | new fruit |
| Creation Fruit | Legendary | `grandline:creation` | `grandline:creation_fruit` | `creation_fruit.png` | new fruit |
| Sound Fruit | Legendary | `grandline:echo` | `grandline:echo_fruit` | `sound_fruit.png` | carried over from old `echo` |
| Phoenix Fruit | Legendary | `grandline:phoenix` | `grandline:phoenix_fruit` | `phoenix_fruit.png` | new fruit |
| Portal Fruit | Legendary | `grandline:portal` | `grandline:portal_fruit` | `portal_fruit.png` | new fruit |
| Lightning Fruit | Legendary | `grandline:volt` | `grandline:volt_fruit` | `lightning_fruit.png` | carried over from old `volt` |
| Pain Fruit | Legendary | `grandline:pain` | `grandline:pain_fruit` | `pain_fruit.png` | new fruit |
| Blizzard Fruit | Legendary | `grandline:blizzard` | `grandline:blizzard_fruit` | `blizzard_fruit.png` | new fruit |
| Magnet Fruit | Legendary | `grandline:magnet` | `grandline:magnet_fruit` | `magnet_fruit.png` | new fruit |
| Gas Fruit | Legendary | `grandline:gas` | `grandline:gas_fruit` | `gas_fruit.png` | new fruit |
| Gravity Fruit | Mythical | `grandline:orbit` | `grandline:orbit_fruit` | `gravity_fruit.png` | carried over from old `orbit` |
| Mammoth Fruit | Mythical | `grandline:mammoth` | `grandline:mammoth_fruit` | `mammoth_fruit.png` | new fruit |
| T-Rex Fruit | Mythical | `grandline:t_rex` | `grandline:t_rex_fruit` | `t_rex_fruit.png` | new fruit |
| Dough Fruit | Mythical | `grandline:dough` | `grandline:dough_fruit` | `dough_fruit.png` | new fruit |
| Shadow Fruit | Mythical | `grandline:ink` | `grandline:ink_fruit` | `shadow_fruit.png` | carried over from old `ink` |
| Venom Fruit | Mythical | `grandline:toxin` | `grandline:toxin_fruit` | `venom_fruit.png` | carried over from old `toxin` |
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
