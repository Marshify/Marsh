# Island NPC rosters (0.71.0)

Islands with a fixed population (`IslandRoster`, enforced by `RosterNpcs`): Millbrook, Ironhold, Verdant Wilds,
Saltmarket Docks, Sunscorch Dunes, Frostpeak Isle (their rebuilt layouts; older layouts keep their old NPCs).
Enemies stay the shared Bandit / Raider / Island Captain types; each island gives them its own skin
(`textures/entity/<skin>.png`, synced per enemy) and names (`names.json`, `island_npcs`). Every enemy has its own
spot and respawn timer, spawns on the island's ground, and has a 12-block leash; NPC areas are safe zones.

| Island | Enemies (level, respawn) | Friendly NPCs | Quests (min level) |
|---|---|---|---|
| Verdant Wilds | 7 Canopy Primate (10-18, 20 s), 4 Ironback (17-25, 20 s, west island), Crownback (28, 60 s, west island north) | Adventurer (temple stairs), Boat Dealer (dock) | 6 Primates, 8 Ironbacks, Crownback (10) |
| Saltmarket Docks | 6 Redcoat Raider (35, 15 s, west clearing), 6 Dock Enforcer (45, 15 s, east clearing), Galley Master (55, 5 min) | Pirate Adventurer, Dark Step Teacher*, Blacksmith*, Sword Dealer of the West, Pirate Recruiter, Set Home Point, Boat Dealer | 8 Redcoats (30), 8 Enforcers (40), Galley Master (55) |
| Sunscorch Dunes | 4 Dune Prowler (60, 15 s, village courtyards), 4 Sandwatch Captain (75, 15 s, ruins between the pyramids); no boss | Desert Adventurer, Boat Dealer, Set Home Point, Guy (Swordsman Hat 15,000, pyramid chamber), Desert Merchant* | 8 Prowlers (60), 6 Captains (75) |
| Frostpeak Isle | 6 Frost Rover (90, 15 s, village), 4 Rime Construct (100, 15 s, frozen arena), Glacier Maw (110, 3 min, northern ridge) | Villager, Boat Dealer, Sword Dealer of the East, Set Home Point, Ability Teacher (ice cave: Flash Step, Aura) | 7 Rovers (90), 8 Constructs (100), Glacier Maw (105) |

| Redbrick Port | 8 Harbor Sentinel (120, 15 s, grass behind the crescent wall, 4 west + 4 east), Bastion Commander (130, 5 min, HQ hall) | Marine Officer, Advanced Weapon Dealer (guns), Parlus (Black Cape 50,000, SW watchtower chamber) | 8 Sentinels (120), Bastion Commander (130) |
| Cloudtop Isles (lower region only) | 4 Cloudstep Raider (150, 15 s, Azure Rise temple), 4 Duskwarden (175, 15 s, Skyreach Citadel); no boss | Mad Scientist*, Boat Dealer, Luxury Boat Dealer, Angler (fishing), Sky Adventurer, Master Sword Dealer, Yoshi (Tomoe Ring 100,000), Set Home Point | 7 Raiders (150), 8 Duskwardens (175) |

\* talk only for now. Not added (no such feature yet): Rich Man, Sick Man. Removed: Saltmarket's Trial Keeper and Fruit
Dealer, the islands' Fishermen. Verdant's Saber Warden is the Crimson Duelist there (unchanged rules; its skin is
still the old one: no crimson_duelist skin was supplied).

Quest rewards: the stage's usual reward, scaled by kills over six; bosses five times it. Kills count only on the
quest's own island. Accessories: `AccessoryShop`, prices in config `accessoryPrices`.

## Raid: the Quake Monarch (Redbrick Port)

`RaidEvents`: every `raidIntervalMinutes` (120) while players are online he rises (Lv 750) on the plaza north of the HQ,
announced server-wide; he leaves after `raidDurationMinutes` (15). Never more than one. Everyone who hurt him gets 10x
Redbrick's quest reward; Bisento owners unlock **Bisento V2** (now the only way to get it). Admin:
`/grandline admin raid start|stop`. Cloudtop's upper region keeps its own population (`IslandRoster.lowerOnly`).
