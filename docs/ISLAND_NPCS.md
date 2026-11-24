# Island NPC rosters (0.74.0)

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

| Stonewall Prison | 5 Stonewall Inmate (190, 9 s, outer enclosure west), 5 High Security Inmate (210, 12 s, east), Iron Jailer (220, 4 min) and Emerald Overseer (230, 10 min) in the central courtyard, Rose Baron (240, 30 min, central tower) | Jail Keeper (inmate quests), Head Jailer (boss quests), Military Detective (Second Sea Lv 700 info), Fruit Removal NPC (50,000), Boat Dealer | 8 + 8 inmates (190, 210); Iron Jailer (220), Emerald Overseer (230), Rose Baron (240) |
| Gladiator's Ring | 5 Arena Vanguard (250, 20 s, west grounds), 5 Gilt Champion (275, 20 s, east grounds); arena free; no boss | Arena Quest Giver, Boat Dealer, Luxury Boat Dealer, Set Home Point | 7 Vanguards (250), 8 Champions (275) |

| Emberfall Isle | 5 Ashguard Trooper (300, 20 s, 50% Aura), 4 Cinder Infiltrator (325, 25 s, Aura, Flash Step), Emberfall Marshal (350, 8 min, magma moves) | Mayor, Ability Teacher, Living Skeleton (Soul Cane), Set Home Point | 7 / 8 / Marshal (300, 325, 350) |
| Lotus Palace | 7 Tideborn Bladesman (375, 30 s, west), 6 Abyssal Guard (400, 20 s, east), Deepcourt Sovereign (425, 8 min, throne) | King Neptune, Water Kung Fu Teacher*; surface: Fisherman, Set Home Point | 8 / 7 / Sovereign (375, 400, 425) |
| Crystal Fountain | 6 Steelcrest Raider (625, 16 s, outer level by the gate), 8 Steelcrest Captain (650, 20 s, Aura, inner ring), Ironvolt (675, 12 min, central court) | City Quest Giver, Boat Dealer, Set Home Point | 8 / 9 / Ironvolt (625, 650, 675) |
| Dawnreach Harbor | none (safe hub) | Fruit Dealer, Fruit Gacha Dealer, Weapon Dealer, Sword Dealer, Experienced Captain (Second Sea, Lv 700), Boat Dealer, Luxury Boat Dealer, Set Home Point, Aura Editor*, Dark Blade Upgrade*, Race Mystic (kept) | none |

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
