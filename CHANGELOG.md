# Grand Line patch notes

## v0.14.0 — Loadout Hotbar
**New**
- **Loadout hotbar:** a new hotbar in the Grand Line style, with numbered slots and a highlighted selection. Same keys, scrolling and items as before.
- **Loadout panel:** Style, Fruit, Sword and Gun slots beside the hotbar, showing which one powers your moves.
- **Press R to cycle your loadout:**
  - **Style / Fruit** switch your move keys.
  - **Sword / Gun** jump straight to that weapon in your hotbar.
- You can switch back to the vanilla hotbar in Settings → Loadout Hotbar.

**Changes**
- The Side button has been removed from the Menu. Pick a Side still opens when you join.

## v0.13.0 — Build Tools & Creative Map Teleport
**New**
- **Creative map teleport:** in creative mode, click an island on the sea chart to teleport there.
- **Custom structures:** island builds made with Structure Blocks can now be placed into the Grand Line sea, with markers for docks, NPCs, quest areas and travel landings.
- **Travel plates:** step on a special pressure plate to travel to a landing elsewhere. Only the player on the plate goes.

**Fixes**
- Removed the light streaks drawn over the sunken Lotus Palace on the sea chart.

## v0.12.0 — New UI
**New**
- The whole interface uses the Marsh UI look: yellow-titled windows, chunky buttons, coloured stat rows and new health, energy and XP bars.
- **Menu (H):** opens big buttons right under your bars: Stats, Items, Settings, Map, Side and Close.
- **Items (I):**
  - **Backpack:** every Grand Line item you carry. Click an accessory or ring to equip it.
  - **Build:** your fighting style, gun, fruit and sword, plus your accessory and ring slots and your Build Stats.
  - **Stash:** your stored fruits.
- **Settings:**
  - Turn other players' move effects on or off.
  - Camera shake on nearby impacts.
  - Show or hide the move list, the quest tracker and the Menu button.
  - **Move HUD:** drag your HUD panels anywhere. **Reset HUD** puts them back.
- **Pick a Side!** Choose Pirates or Marines every time you join (or from the Menu):
  - **Pirates** have red names and can fight anyone, even other pirates.
  - **Marines** have blue names and can't hurt each other.

**Changes**
- The Stats screen has the new look, and the Gun stat now correctly shows that it boosts gun moves.
- Accessory and ring slots moved from the Stats screen to Items → Build.

## v0.11.0 — Flame X Polish
**New**
- **Blaze Burst (Flame X)** now plays out in three beats:
  - **Windup:** embers gather around your hand as you pull your shoulder back.
  - **Release:** a blocky flaming fist with a tapering flame trail snaps forward.
  - **Impact:** a bright spark pop, expanding fire rings and a burst of embers. The target stays visible.
- A punch animation with windup, snap and follow-through, seen by you in third person and by other players. In first person your hand punches on release.
- Optional support for the **Photon** effects mod (Fabric 1.20.1, version 1.1.17). With Photon installed, custom effects can replace the built-in ones.

**Changes**
- Blaze Burst now has a short 0.3 second windup before the fist launches. Damage, range, cooldown and mastery are unchanged.

## v0.10.1 — Chart Hover Fix
**Fixes**
- The surf around each island on the sea chart now rises and grows with the island when you hover over it, instead of staying behind.

## v0.10.0 — Swords, Guns & Accessories
**New**
- **4 swords:** Katana, Cutlass, Saber and Trident. Hold one and Z / X / C use its moves (Rising Draw, Crescent Arc, Hundred Cuts, and more).
- **4 guns:** Slingshot, Flintlock, Musket and Cannon. Right-click to fire (no ammo, each has its own reload), and Z / X / C for gun moves like Long Shot and Broadside Barrage.
- Swords and guns level up their own mastery with kills, which unlocks more moves.
- **4 accessories.** Right-click to equip:
  - **Black Cape:** +5% damage (Melee, Sword, Gun, Fruit), +2 hearts, +10 max energy.
  - **Pink Coat:** +10% Gun damage, +4 hearts.
  - **Swordsman Hat:** +10% Sword damage (normal hits and sword moves).
  - **Tomoe Ring:** +10% Fruit damage (goes in a ring slot).
- **Build screen (K):** a new Gear panel shows your accessory and two ring slots, plus your total bonuses. Click a slot to take the item off.

**Notes**
- Fruit moves always get Fruit bonuses, even while you're holding a sword or gun.
- Swapping gear never refills your health or energy.
- Accessories don't show on your character yet. That needs worn models.

## v0.9.1 — Refined Sea Chart
**New**
- The sea chart (M) has a brand-new pirate look: a torn parchment frame, gold doubloons in the corners, surf around every island and smoke rising from Emberfall's volcano.
- Lotus Palace now appears sunken beneath the waves.
- Islands gently rise and grow when you hover over them.

**Changes**
- New, more detailed pictures for all 14 First Sea islands.
- A wooden "First Sea" title plate and a repositioned close button, to match the new frame.

## v0.9.0 — The First Sea, Rebuilt
**New**
- All 14 First Sea islands have been rebuilt as hand-designed islands with their own shapes, landmarks, roads and docks:
  - **Dawnreach Harbor:** fountain plaza, hilltop hall with a clock tower, market stalls, a lighthouse and two piers.
  - **Verdant Wilds:** a four-tier jungle temple with a grand stairway, an inner chamber and a summit shrine.
  - **Millbrook Village:** windmills, crop fields, cottages, a barn and a well.
  - **Redbrick Port:** brick terraces climbing to a stone keep.
  - **Sunscorch Dunes:** a stepped pyramid with a burial chamber, and a palm oasis.
  - **Frostpeak Isle:** a snowy peak, a frozen waterfall, a lodge and igloos.
  - **Ironhold Bastion:** walls, towers, a gatehouse and a keep.
  - **Cloudtop Isles:** four floating islands joined by rope bridges.
  - **Stonewall Prison:** cell blocks and a watchtower.
  - **Gladiator's Ring:** a full colosseum with tiered seating.
  - **Emberfall Isle:** a volcano with a lava crater and magma streams.
  - **Saltmarket Docks:** a trade hall, warehouses and cranes.
  - **Lotus Palace:** a four-tier pagoda over a lotus pond.
  - **Crystal Fountain:** tiered quartz fountains and crystal spires.
- Buildings have doors and furnished interiors.
- Quest givers, shops and fishermen stand in their proper spots. Enemies spawn in camps, and quest bosses wait in each island's arena.
- Every island has a dock with a boat waiting, and buried treasure with a fruit inside.

**Notes**
- Only new worlds, or parts of the sea you haven't explored yet, get the new islands. Islands you've already visited stay as they were.
- Second and Third Sea islands are next.

## v0.8.4 — Hollow Islet Removed
**Changes**
- Hollow Islet has been removed. It no longer generates in new worlds and no longer appears on the sea chart or in travel.
- The First Sea now has 14 islands.

**Notes**
- In existing worlds, Hollow Islet is removed from the chart and travel automatically. Any blocks and NPCs already generated there stay in the world.

## v0.8.3 — Retired Fruits Removed
**Changes**
- The 24 retired fruits from the old lineup are gone for good: Thorn, Pebble, Ash, Tide, Coral, Iron, Sludge, Glass, Rust, Pollen, Bloom, Prism, Monsoon, Mirage, Hornet, Wolf, Grizzly, Tempest, Solar, Leviathan, Aegis, Hydra, Titan and Seraph.
- The fruit lineup is now exactly the 42 fruits with the new art.

**Notes**
- If you had eaten a retired fruit, you'll have no fruit when you next join. Eat a new one!
- Retired fruits are removed from your stash. Any copies in your inventory or chests disappear.

## v0.8.2 — Classic Sea Chart
**Changes**
- The sea chart (M) has a brand-new look: every First Sea island is drawn as a little block-built island, with a sharper ocean background and blocky clouds.
- A compass rose sits in the top-right corner.
- Hollow Islet shows as a small islet. Hover over it for its name and level.
- The "Recommended" badge has been removed to match the new art.

## v0.8.1 — Movement on F
**Changes**
- The fifth move (flight, dashes, blinks and jumps) now uses **F** instead of R.
- Existing players are switched to F automatically. You can still rebind it in Controls, under Grand Line → Move 5 (movement).
- Fruit tooltips and the fruit dealer now show F for the movement move.

**Notes**
- F is also vanilla's "swap item with offhand" key, so pressing F does both. To avoid that, rebind either one in Controls.

## v0.8.0 — The New Fruit Roster
**New**
- A brand-new lineup of **42 fruits**, each with its own pixel-art icon and five original moves.
- 24 fruits are brand new: Rocket, Blade, Spring, Spike, Ghost, Buddha, Love, Creation, Spider, Phoenix, Portal, Pain, Blizzard, Magnet, Gas, Mammoth, T-Rex, Dough, Spirit, Tiger, Yeti, Kitsune, Control and Dragon.
- **Flight fruits:** Eagle, Light, Phoenix, Blizzard, Spirit and Dragon can fly with R. Press R again to land.
- New elemental effects: webs slow, hearts weaken, souls lift, foxfire burns, magnets pull, control lifts and dragons scorch.

**Changes**
- 18 old fruits became new ones, and you keep them and their mastery: Ember → Flame, Frost → Ice, Dune → Sand, Lava → Magma, Volt → Lightning, Blast → Bomb, Umbra → Dark, Orbit → Gravity, Echo → Sound, Quartz → Diamond, Tremor → Quake, Toxin → Venom, Feather → Eagle, Nova → Light, Bounce → Rubber, Gale → Spin, Mist → Smoke, Ink → Shadow.
- Flame's moves have new names: Cinder Shots, Blaze Burst, Pyre Pillar, Inferno Nova and Ember Wings.
- Fruit rarities have been rebalanced for the new lineup.

**Notes**
- The other 24 old fruits are now retired. If you own one or have eaten one, it still works and keeps its mastery, but it no longer drops or sells.

## v0.7.2 — New Fruit Art (Part 1)
**New**
- 15 fruits have brand-new 32×32 pixel art: Flame, Frost, Dune, Lava, Volt, Blast, Umbra, Orbit, Echo, Quartz, Tremor, Toxin, Feather, Nova and Bounce.
- The new art shows everywhere the fruit appears: inventory, the fruit dealer, the gacha, your stash and the eat/store dialog.

**Notes**
- Item IDs are unchanged, so fruits you're holding, eaten fruits and mastery all carry over.
- The other 27 fruits keep their old icons until they're matched with their new art.

## v0.7.1 — The New Sea Chart
**New**
- The sea chart (M) has new art: an illustrated ocean, a hand-drawn picture for every First Sea island and drifting clouds along the edges.
- Your marker shows your own face in a gold ring, with a glowing cone that points the way you're facing.
- Hover over an island to see its name and level, in green, yellow or red depending on your level.
- A red ✕ in the corner closes the chart.

**Changes**
- Islands and your marker are placed using the same map coordinates, so the marker matches where you really are, on an island or out at sea.
- Island names no longer clutter the chart. They show on hover instead.

**Known issues**
- Hollow Islet still uses its old icon.
- The Second and Third Sea charts use the new background with their old island icons.

## v0.7.0 — Flame Fruit Rework
**New**
- Ember is now the **Flame Fruit**, with a brand-new moveset aimed with your crosshair:
  - **Z – Fire Bullets** (Mastery 1): five fireballs that set targets on fire.
  - **X – Burning Blast** (Mastery 25): a fireball that explodes with knockback.
  - **C – Fire Column** (Mastery 50): a pillar of flame launches enemies into the air.
  - **V – Flame Destroyer** (Mastery 100): tap for a quick blast, or hold to charge a huge one.
  - **R – Fire Flight** (Mastery 10): fly around on fire. Press R again to land.

**Changes**
- Mastery now starts at level 1 instead of 0. Existing mastery shows one level higher.
- Moves can now be charged by holding the key.

**Notes**
- Your Ember fruit and its mastery carry over.
- The new Flame Fruit icon is coming soon.
