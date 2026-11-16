# Treasure chests (0.39.0)

Three kinds of treasure chest are hidden around every First Sea island: 55 in all. Each one is **per player**: opening
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
  removed when its chunk loads. Islands still on their pre-redesign plan in old worlds get no chests (the spots belong
  to the new layouts).

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
| Cloudtop Isles | Legendary | -69, 96, -35 | west | dark, covered, walls e |
| Cloudtop Isles | Rare | 55, 144, -104 | east | indoors, walls w |
| Cloudtop Isles | Common | 42, 126, -68 | east | walls w |
| Cloudtop Isles | Common | 6, 123, -37 | south | walls nw |
| Crystal Fountain | Legendary | -28, 64, 28 | south | beach camp shelter, guarded by the west camp |
| Crystal Fountain | Rare | 26, 64, 31 | south | beside the east camp on the beach |
| Crystal Fountain | Common | -25, 67, -7 | south | at the foot of a crystal pillar |
| Crystal Fountain | Common | 19, 67, -17 | south | at the foot of a crystal pillar |
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
| Redbrick Port | Legendary | -16, 76, -23 | south | raised 11, walls nw |
| Redbrick Port | Rare | 24, 65, -25 | north | under a shelter by crates |
| Redbrick Port | Common | 35, 66, 6 | east | by crates |
| Redbrick Port | Common | -37, 65, -14 | west | by crates under a shelter |
| Saltmarket Docks | Legendary | -19, 66, -19 | west | under the warehouse awning, by crates |
| Saltmarket Docks | Rare | 19, 66, -19 | east | under the warehouse awning, by crates |
| Saltmarket Docks | Common | 4, 64, 28 | west | by crates, walls e |
| Saltmarket Docks | Common | -3, 66, -24 | south | walls ne |
| Stonewall Prison | Legendary | -35, 91, -51 | east | raised 23, walls w |
| Stonewall Prison | Rare | -44, 69, -9 | east | dark, covered, walls sw |
| Stonewall Prison | Common | 15, 67, 39 | east | by crates, walls w |
| Stonewall Prison | Common | 36, 68, 41 | north | walls s |
| Sunscorch Dunes | Legendary | -23, 71, -51 | east | dark, covered, raised 5, walls w |
| Sunscorch Dunes | Rare | -20, 71, -30 | east | indoors, raised 5, walls w |
| Sunscorch Dunes | Common | -24, 67, 17 | south | by crates, walls n |
| Sunscorch Dunes | Common | 9, 67, 26 | south | by crates, walls n |
| Verdant Wilds | Legendary | -121, 67, -2 | west | by crates, dark, covered, walls e |
| Verdant Wilds | Rare | -109, 67, -18 | west | by crates, dark, covered, walls e |
| Verdant Wilds | Common | 14, 69, 14 | south | by crates, walls n |
| Verdant Wilds | Common | -23, 67, 28 | east | by crates, walls w |

**Known compromises:** Saltmarket Docks, Lotus Palace, Redbrick Port and Crystal Fountain are simpler builds with few
hidden corners, so some of their chests sit at camps, under awnings or at landmarks rather than in hidden rooms. When
an island is rebuilt (Saltmarket Docks is next), its chests are re-picked from the new build.
