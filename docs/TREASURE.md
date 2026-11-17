# Treasure chests (0.39.0)

Three kinds of treasure chest are hidden around every First Sea island: 56 in all. Each one is **per player**: opening
it pays you Beli and starts *your* refill timer for that chest; other players still find it full.

| Chest | Pays (x the island's quest reward, +/-15%) | Refills after |
|---|---|---|
| Common | 1x | 15 minutes |
| Rare | 3x | 45 minutes |
| Legendary | 8x | 3 hours |

The island's quest reward is `40 + 20 x the island's quest level` (Dawnreach Harbor, the hub, pays like the first
island), so a chest stays worth finding all the way up the ladder. All numbers are in the config:
`treasureRewardMultipliers`, `treasureRewardSpread`, `treasureRefillMinutes`.

## How it works

- **Blocks:** `grandline:treasure_chest_common`, `_rare`, `_legendary`. Each is a 3D model (Blockbench-compatible
  Java block model) with its own pixel-art texture, made by `tools/treasure/make_chests.py` from the reference render:
  - **Common:** weathered dark wood, an arched lid, bronze bands, rivets, a big lock.
  - **Rare:** red-brown wood, polished silver bands with scrollwork, blue gems, an ornate lock.
  - **Legendary:** crimson wood, thick gold bands and scrollwork, a golden skull lock, red gems, golden motes and a
    faint glow.
  They cannot be broken, hold no items and open no menu, so adventure mode's container lock does not apply.
- **Opening** (`TreasureService`, server-side): pays the Beli, records the refill time on the player (saved, kept
  through death), and plays the opening (sound, gold coins, a glow for rare and legendary) for that player only. A
  chest that is still refilling says how long is left.
- **Placement** (`TreasurePlacer`): every chest has a fixed spot (`TreasureSpots`). When an island's chunk loads, its
  chests are put in place, in new worlds and existing ones alike. A chest is only placed where the spot is open (air or
  plants on a solid floor), so it never cuts into a build. If a spot is moved in a later version, the old chest is
  removed when its chunk loads. Islands an older world still has in their pre-rebuild layout keep that layout's own
  spots (`TreasureSpots.legacy`), or get none.

## Choosing the spots

1. `/grandline treasure scan` (op) generates every island and writes `treasure_scan/<island>.json`: the surface, the
   NPC and gameplay anchors, and every block a chest could stand on, tagged with what is around it (walls, roof,
   enclosure, crates, doors, sky light), plus every other block a player can stand on.
2. `tools/treasure/pick_spots.py` keeps only spots a player can **walk to** from the island's NPCs and docks (stairs,
   slabs and open doors count; no flying, no sealed rooms), away from NPCs, doors, paths and beaches, with a wall
   behind the chest and room in front. It ranks them:
   - Common: outdoors at ground level, tucked into corners and backyards or beside crates.
   - Rare: indoors, or raised up against a wall.
   - Legendary: dark covered nooks, high ledges, enclosed rooms, far from the arrival point.
3. Every pick was then looked at in game (a close-up and a wide shot each) and the weak ones moved by hand:
   `tools/treasure/query_spots.py` lists the candidates on one island. The final list is `tools/treasure/picks.json`;
   `tools/treasure/write_spots.py` writes it into `TreasureSpots.java`.
4. `/grandline treasure place` puts every chest in place at once, and `/grandline treasure clean` removes any stray
   chest on the planned islands (both for testing).

Tests (`GrandLineTreasureGameTests`) check the per-player payout and refill, the tier rewards, and that every First Sea
island has well-spread chests (at most one legendary, 12+ blocks apart, on the island).

## Every chest (island-local coordinates)

| Island | Chest | Position | Faces | Where |
|---|---|---|---|---|
| Cloudtop Isles | Legendary | 96, 240, -199 | south | the temple's summit platform (0.46.0; older layouts keep their own spots) |
| Cloudtop Isles | Rare | 58, 205, -208 | east | by the leaning ruined arch |
| Cloudtop Isles | Common | -52, 201, -134 | west | edge of the Ancient Tree combat field |
| Cloudtop Isles | Common | 24, 99, 2 | south | Mistfall, on the grass |
| Cloudtop Isles | Common | -30, 101, -36 | north | Bloomhold, between the towers |
| Crystal Fountain | Legendary | 206, 67, -28 | west | scrapyard, by the foundry engine (0.47.0; older worlds keep the old spots) |
| Crystal Fountain | Rare | 22, 77, 22 | north | the landmark courtyard |
| Crystal Fountain | Common | -67, 73, -58 | north | north-west fountain plaza |
| Crystal Fountain | Common | 30, 66, 190 | south | harbour plaza |
| Dawnreach Harbor | Rare | 24, 68, -12 | south | inside a market hall, by crates |
| Dawnreach Harbor | Common | 22, 68, 10 | north | by crates in a market hall |
| Dawnreach Harbor | Common | -62, 68, 32 | west | by crates near the west harbour |
| Emberfall Isle | Legendary | -4, 105, -33 | west | raised 39, walls es |
| Emberfall Isle | Rare | -33, 79, -77 | east | by crates, indoors, raised 13, walls sw |
| Emberfall Isle | Common | -25, 67, 59 | west | by crates, walls e |
| Emberfall Isle | Common | -6, 65, 96 | south | by crates, walls n |
| Frostpeak Isle | Legendary | 32, 67, 4 | south | dark, covered, walls ne |
| Frostpeak Isle | Rare | -17, 66, 7 | north | by crates, indoors, walls s |
| Frostpeak Isle | Common | 24, 66, 24 | south | walls n |
| Frostpeak Isle | Common | 2, 63, 23 | north | walls s |
| Gladiator's Ring | Legendary | -39, 88, -25 | east | raised 21, walls w |
| Gladiator's Ring | Rare | 40, 66, -45 | east | by crates, indoors, walls w |
| Gladiator's Ring | Common | 23, 67, 55 | east | by crates, walls w |
| Gladiator's Ring | Common | -62, 68, -22 | west | by crates, walls e |
| Ironhold Bastion | Legendary | 19, 66, -24 | north | back corner of the bastion |
| Ironhold Bastion | Rare | -7, 67, -8 | east | indoors in the keep |
| Ironhold Bastion | Common | -13, 67, 21 | north | walls s |
| Ironhold Bastion | Common | -23, 65, -26 | north | rocky back corner |
| Lotus Palace | Legendary | 1, 70, -26 | east | raised garden behind the palace |
| Lotus Palace | Rare | -33, 64, 24 | south | by crates at the west camp, guarded |
| Lotus Palace | Common | -1, 64, 15 | south | walls n |
| Lotus Palace | Common | 13, 64, 23 | north | sheltered corner by the shore |
| Millbrook Village | Legendary | 54, 67, 18 | west | at the bandit boss camp, guarded |
| Millbrook Village | Rare | 17, 67, -20 | east | by crates, indoors, walls w |
| Millbrook Village | Common | -18, 67, 28 | south | by crates, walls n |
| Millbrook Village | Common | 6, 67, 26 | south | by crates, walls n |
| Redbrick Port | Legendary | 3, 79, 2 | west | the headquarters' crown room (0.50.0; older worlds keep the old spots) |
| Redbrick Port | Rare | 35, 83, -22 | west | the north-east watchtower's chamber |
| Redbrick Port | Common | -22, 66, 25 | west | the south-west quay, behind the field cannons |
| Redbrick Port | Common | 13, 66, -52 | east | the green behind the wall, by the east guard post |
| Saltmarket Docks | Legendary | -100, 114, 47 | east | on the lighthouse balcony, at the top of the ladder |
| Saltmarket Docks | Rare | 46, 69, -107 | north | inside the boss ring at the far camp, by crates |
| Saltmarket Docks | Common | -33, 65, 57 | north | by cargo on the harbour promenade |
| Saltmarket Docks | Common | -45, 68, -29 | north | back alley against the landward wall, by crates |
| Saltmarket Docks | Common | 59, 66, -137 | west | among the rocks on the east headland |
| Stonewall Prison | Legendary | 4, 102, 0 | west | the central tower roof (0.48.0; older worlds keep their spots) |
| Stonewall Prison | Rare | 0, 76, -23 | south | the inner enclosure ledge |
| Stonewall Prison | Common | 30, 67, -8 | west | east grass courtyard |
| Stonewall Prison | Common | -49, 64, -2 | east | the waterfront landing |
| Sunscorch Dunes | Legendary | -23, 71, -51 | east | dark, covered, raised 5, walls w |
| Sunscorch Dunes | Rare | -20, 71, -30 | east | indoors, raised 5, walls w |
| Sunscorch Dunes | Common | -24, 67, 17 | south | by crates, walls n |
| Sunscorch Dunes | Common | 9, 67, 26 | south | by crates, walls n |
| Verdant Wilds | Legendary | -121, 67, -2 | west | by crates, dark, covered, walls e |
| Verdant Wilds | Rare | -109, 67, -18 | west | by crates, dark, covered, walls e |
| Verdant Wilds | Common | 14, 69, 14 | south | by crates, walls n |
| Verdant Wilds | Common | -23, 67, 28 | east | by crates, walls w |

**Known compromises:** Lotus Palace, Redbrick Port and Crystal Fountain are simpler builds with few
hidden corners, so some of their chests sit at camps, under awnings or at landmarks rather than in hidden rooms.
When an island is rebuilt its chests are re-picked from the new build (Saltmarket Docks was, in 0.40.0: all outdoor).

**When an island is rebuilt:** re-pick its chests from the new build with `/grandline treasure scan` and the tools
above. The owner's preference is **outdoor spots only** (behind buildings, by crates, ledges, rocks, caves), well
spread out; no chests inside buildings.
