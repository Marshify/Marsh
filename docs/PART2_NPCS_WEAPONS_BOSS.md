# NPCs, weapons and the Verdant Wilds boss (Update Part 2)

All values below are defaults from `config/grandline.json`.

## Innkeeper (home NPC), on every island

- **Where:** a few blocks from the island's main NPC (quest giver, gunsmith or arrival point), on open ground (never on a roof), clear of the other NPCs. If an island has no planned NPCs, the Innkeeper stands near its centre. Each island's spot is chosen once and saved (`data/grandline_keepers.dat`).
- **Existing worlds:** the Innkeeper appears the first time a player comes near each island.
- **Can't be lost:** Innkeepers can't be hurt, don't despawn and can't be pushed. If one is removed anyway (for example with `/kill`), it is put back at its spot the next time a player is near. There is never more than one per island.
- **Talking to it:** "Would you like to make this island your home?" with the options **Set Home** and **Cancel**. Set Home shows "Home successfully set to <Island>!".
- **Your home:** saved per player through death, logout and restarts. You can change it as often as you like.
- **Respawning:**
  1. You come back on the island you died on.
  2. If you died at sea, or anywhere off an island, you come back at your home.
  3. With no home set, you come back at Millbrook Village.

  Setting a home never overrides respawning on the island you died on.

Placed in the test world (First Sea):

| Island | Innkeeper at |
| --- | --- |
| Dawnreach Harbor | -28 68 -9 |
| Verdant Wilds | -55 68 -377 |
| Millbrook Village | 630 66 116 |
| Redbrick Port | -629 65 140 |
| Sunscorch Dunes | 578 68 548 |
| Frostpeak Isle | 610 66 -400 |
| Ironhold Bastion | 1380 66 30 |
| Cloudtop Isles | -1498 67 -536 |
| Stonewall Prison | -1138 67 270 |
| Gladiator's Ring | -629 67 -514 |
| Emberfall Isle | -1379 65 694 |
| Saltmarket Docks | -32 66 542 |
| Lotus Palace | 1201 65 -600 |
| Crystal Fountain | 1201 67 686 |

Other seas work the same way, for example Kingsport Haven at 11998 68 1 and Tidecrest Citadel at 23998 68 1. Exact spots can differ in other worlds and on hand-edited terrain.

## Swordsmith, Millbrook Village

- **Where:** beside the Innkeeper on Millbrook's square, by the well. In the test world that's 632 66 116.
- **Existing worlds:** placed and kept just like the Innkeeper.
- **Sells:**

  | Sword | Price | Config |
  | --- | --- | --- |
  | Katana | $8,000 | `swordPrices[0]` |
  | Cutlass | $2,500 | `swordPrices[1]` |

- **How buying works:**
  - The price is checked and taken on the server before the sword is handed over.
  - If you can't afford it you get "Not enough money for that." and nothing changes.
  - The sword goes into your inventory, or drops at your feet if the inventory is full.

## Saber (buffed)

| | Before | Now | Config |
| --- | --- | --- | --- |
| Damage per hit | 7 | **9** | `saberAttackDamage` |
| Attacks per second | 1.7 | **2.0** | `saberAttackSpeed` |
| Damage per second | 11.9 | **18** | |
| Z Thrust | 10 | **13** | ×`saberMoveDamageMultiplier` (1.3) |
| X Twin Arc | 12 | **15.6** | ×1.3 |
| C Flourish | 14 | **18.2** | ×1.3 |

For comparison: the Katana does 6 per hit at 1.8 per second (10.8 per second), and the Cutlass 5 at 2.0 (10 per second).

The Sword stat and gear bonuses add on top, as for every sword. Mastery is unchanged: moves unlock at 0, 20 and 50 mastery, and kills with the Saber train Saber mastery.

New on hit: a sweep flash, sparks, a steel ring and a bright slash arc.

## Saber Warden (Verdant Wilds temple boss)

- **Where:** inside the Verdant Wilds temple hall, standing in front of the altar dais (the raised slab platform with the chiseled pedestal and lantern), facing the door.
  - In the test world that's -60 76 -422; island-local, it's (0, 76, -22).
  - **Arena:** the hall, 23 blocks wide from wall to wall, from the back wall to the door, and 9 high.
  - **Worlds:** only worlds with the redesigned Verdant Wilds have this temple. Worlds created before that redesign have no Saber Warden.
- **Look:**
  - The full player model (both skin layers, standard 64×64 layout), 1.08× size, holding the Saber.
  - Posed through every attack: blade raised, blade drawn back, a crouch, three cuts, then a winded stance.
  - It uses `textures/entity/saber_boss.png`: the supplied Saber Warden skin (64×64, wide arms, both layers), copied unchanged byte for byte (0.27.1).
- **Stats:**

  | Stat | Value | Config |
  | --- | --- | --- |
  | Health | 400, +50% for each extra player in the hall when the fight starts | `saberBossHealth`, `saberBossHealthPerExtraPlayer` |
  | Level | 20 | |
  | Armour | 6 | |
  | Knockback resistance | 80% | |
  | Speed | 0.3 | |

  A Verdant bandit has about 38 HP.
- **Damage:**
  - No single hit can take more than **35%** of a player's max health (`saberBossMaxHitPercent`).
  - World difficulty scales mob damage as usual. On Easy, an 8-damage hit lands for about 4.5.

| Attack | Damage | Shape | Warning | Recovery | Cooldown |
| --- | --- | --- | --- | --- | --- |
| Sword swing | 6 (`saberBossSwingDamage`) | normal melee | swing | — | 1 s |
| **Saber Slash** | 9 (`saberBossSlashDamage`) | 3.8 blocks, 120° in front | blade raised, red ground arc fills for 0.5 s | 0.6 s | 2.25 s |
| **Flying Saber Slash** | 8 (`saberBossFlyingDamage`) | crescent of sword energy, 18 blocks/s, up to 22 blocks, stopped by walls | blade drawn back with a blue glint for 0.8 s; aim locks 0.2 s before release, so sidestep | 0.5 s | 5.5 s |
| **Heavy Combo** | 5 + 5 + 9 (`saberBossComboDamage`, `saberBossComboFinisherDamage`) | three lunging cuts (left, right, overhead), 3.5 blocks, 110° | crouch, red glint and red ground arc for 0.7 s | **1.4 s winded** (punish window) | 7.5 s |

- **Behaviour:**
  - Chases players inside the hall.
  - Drops any target who leaves the hall.
  - Walks back to the altar between fights.
  - If knocked out of the hall, it walks back in first. If it ends up more than 32 blocks away, it is put back.
  - If it is stuck behind pillars, it switches to the Flying Slash.
  - With nobody in the hall for 5 s, it returns to full health and forgets the fight.
- **Reward:**
  - Exactly one Saber per defeat. It goes into the inventory of the player who dealt the most damage.
  - If that player has died or left, the Saber goes to the next living fighter.
  - If no fighter is alive, the Saber stays at the boss's spot. Only the top fighter can pick it up, and it never despawns.
  - The Saber the boss holds is never dropped. A boss removed by a command gives nothing.
- **Respawning:**
  - **600 s (10 minutes)** after a defeat (`saberBossRespawnSeconds`), counted in world time. The timer survives restarts and unloaded chunks.
  - There's only ever one boss: the island remembers its current boss, and stray copies are removed.
  - The boss only appears when its spot is loaded and has room (otherwise the nearest free block within 3).
  - The boss timer is separate from the enemy respawn timer.

## Regular enemy respawn

- Bandits, raiders and corsairs placed by an island come back **10 s** after they die (`enemyRespawnSeconds`), exactly at the spot where they were placed. They return with the same kind, level, weapon and AI.
- One death queues exactly one respawn. Queued respawns count toward the island's limit of 4 enemies, so islands never fill beyond it.
- The queue is saved (`data/grandline_enemy_respawns.dat`). A respawn whose spot isn't loaded waits until it is.
- Enemies that existed before this update respawn at one of their island's enemy camps.
- Bosses never use this timer.
- Quest kills count as before.
