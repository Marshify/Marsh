# First Sea progression (0.30.0)

One table, `world/FirstSeaProgression.java`, now drives every First Sea level: island levels (and so existing worlds' saved levels), enemy and boss levels, enemy kinds, quest rewards, the level shown in NPC dialogue, quest navigation and the "new quests" cards. No island was renamed, moved or rebuilt by this change.

## Stages

| Stage | Island | Levels | Enemy | 1st camp (basic) | 2nd camp (stronger) | Quest boss | Quest XP | Quest $ | Boss quest XP / $ | Quests to clear* |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Millbrook Village / Ironhold Bastion (Marines) | 1–10 | Bandit | 1–5 | 4–8 | 9 | 480 | 140 | 2,400 / 700 | ~5 |
| 2 | Verdant Wilds | 10–30 | Bandit | 10–18 | 17–25 | 28 | 1,680 | 440 | 8,400 / 2,200 | ~12 |
| 3 | Saltmarket Docks | 30–60 | Bandit | 30–42 | 40–52 | 57 | 3,680 | 940 | 18,400 / 4,700 | ~18 |
| 4 | Sunscorch Dunes | 60–90 | Bandit | 60–72 | 70–82 | 87 | 6,080 | 1,540 | 30,400 / 7,700 | ~18 |
| 5 | Frostpeak Isle | 90–120 | Bandit | 90–102 | 100–112 | 117 | 8,480 | 2,140 | 42,400 / 10,700 | ~18 |
| 6 | Redbrick Port | 120–150 | Bandit | 120–132 | 130–142 | 147 | 10,880 | 2,740 | 54,400 / 13,700 | ~18 |
| 7 | Cloudtop Isles (lower) | 150–190 | Bandit | 150–166 | 164–180 | 186 | 13,680 | 3,440 | 68,400 / 17,200 | ~24 |
| 8 | Stonewall Prison | 190–250 | Bandit | 190–214 | 211–235 | 244 | 17,680 | 4,440 | 88,400 / 22,200 | ~37 |
| 9 | Gladiator's Ring | 250–300 | Raider | 250–270 | 268–288 | 295 | 22,080 | 5,540 | 110,400 / 27,700 | ~31 |
| 10 | Emberfall Isle | 300–375 | Raider | 300–330 | 326–356 | 368 | 27,040 | 6,780 | 135,200 / 33,900 | ~46 |
| 11 | Lotus Palace | 375–450 | Raider | 375–405 | 401–431 | 443 | 33,040 | 8,280 | 165,200 / 41,400 | ~46 |
| 12 | Cloudtop Isles (upper) | 450–625 | Raider | 450–520 | 511–581 | 608 | 43,040 | 10,780 | 215,200 / 53,900 | ~109 |
| 13 | Crystal Fountain | 625–700 | Raider | 625–655 | 651–681 | 693 | 53,040 | 13,280 | 265,200 / 66,400 | ~46 |

\* Regular quests alone, from the stage's first level to its last; kill rewards and boss quests shorten it.

- Dawnreach Harbor is the central hub: Lv 1, no quests, no enemies (unchanged).
- Each stage's first level is where it unlocks; at a shared boundary the next island wins (Lv 10 = Verdant Wilds).
- Lv 700 completes the First Sea; leveling continues (the Sea 2 gate is unchanged at 700).
- Temple boss: the Saber Warden (Verdant Wilds) is now Lv 28 (was 20); its health, attacks, VFX, drops, spot and respawn are unchanged.
- Enemy kinds follow the stage (Bandit below Lv 250, Raider from 250), as before; the quest's target kind is the kind its camps hold.

## Rules

- Enemy levels: a region's first camp holds the basic enemies (the lower 40% of its range), its other camps the stronger ones (35-75%). Quest bosses: near the top of the range. Health and damage follow level by the existing enemy formula.
- Quest XP = 1.6 x the experience a player at the middle of the range needs for one level (the existing leveling rule, 100 + 50 per level). One quest moves a player one to two levels at any stage. Money = 40 + 20 x that middle level. Boss quests pay five times both. Kill rewards are unchanged.
- Boss quests start at Verdant Wilds (Lv 10).

## Upper Cloudtop Isles

Cloudtop Isles keeps one island (same id and coordinates) with two regions. The lower region has the landing's quest giver, the east islet camp and a boss spot there. The upper region (Lv 450-625) has the west islet camp, a new quest giver at the summit shrine, and the shrine boss arena. The upper quest giver is kept in place like the innkeepers, so existing worlds get one too. Region-tagged spots (`Anchor.upper()`) carry the region to enemies, quest givers, boss arenas and navigation. The Cloudtop rebuild will keep this split.

## Navigation and cards

- The compass follows the ladder in the First Sea: Lv 1 Pirates go to Millbrook Village, Marines to Ironhold Bastion, Lv 10 to Verdant Wilds, ... Lv 450 to the upper Cloudtop quest giver, Lv 625 to Crystal Fountain. An active quest still leads to its island. Other seas unchanged.
- Cards at Lv 10, 30, 60, 90, 120, 150, 190, 250, 300, 375, 450 ("Cloudtop Isles (Upper)") and 625, once each.

## Existing saves

- Saved island levels update on load (logged: Verdant 10, Saltmarket 30, Sunscorch 60, Gladiator's Ring 250, Lotus Palace 375; the others already matched).
- Player level, XP, money, mastery, items, quests, race, side and home are untouched.
- Enemies outside their island's new range take a level in it when they load; enemies of a kind their island no longer has (Gladiator's Ring moving to Lv 250 switches Bandits to Raiders) are removed and the island refills.
- An active quest asking for such a kind now asks for the island's current kind, keeping its progress and rewards.
- Players already past a milestone are marked as told on join (no flood of cards).

## Tests

- `GrandLineProgressionGameTests`: the 13 stages and their ranges (contiguous, both starters 1-10, upper Cloudtop separate); island levels match the table; every island name exists and Dawnreach stays a Lv 1 hub; boundary targets at Lv 1 (each side), 9, 10, 29, 30, 60, 150, 189, 190, 250, 300, 375, 449, 450, 624, 625, 700, 1200; enemy levels in range and the second camp stronger; Millbrook's first camp stays Lv 1-5; bosses near the top; rewards and boss levels rise along the ladder; quest pacing at both ends of every stage.
- Updated: the navigation and unlock-card tests (Verdant Wilds is now Lv 10).
- Existing world loaded: levels migrated, no errors. Full suite: 107 / 107 pass.

## 0.62.0: Ironhold Bastion and Redbrick Port swapped stages

Ironhold Bastion is now the Marines' starting island (stage 1, Lv 1–10) and Redbrick Port took Ironhold's old stage 6
(Lv 120–150). Only the ladder entries changed: neither island moved or was rebuilt, and both keep their Bandits.
Existing worlds follow on load: the saved island levels are updated, and enemies saved with the old levels are
re-levelled when they load.
