# First Sea Swords (0.55.0)

Fifteen swords, each with a normal-attack combo and two moves (Z, X). All swords scale with the **Sword** stat, train
their own mastery from kills, sit in the Sword slot of the loadout hotbar and show in the Items tab. Owned swords are
kept for good (see EquipmentOwnership); the Swordsmith sells every sword except the Saber, which drops from the Saber
Warden.

Energy is a share of the player's maximum energy (15 = 15%). Cooldowns are in seconds.

| Sword | Rarity | Combo | Z (mastery, cooldown, energy) | X (mastery, cooldown, energy) |
|---|---|---|---|---|
| Katana | Common | 3 cuts + sweeping slash | Silent Step (1, 7, 15) | Crescent Edge (20, 10, 25) |
| Cutlass | Common | 3 alternating slashes + thrust | Swift Advance (1, 7, 15) | Wind Cutter (20, 10, 25) |
| Dual Katana | Common | 4 dual strikes + crossing slash | Cyclone Edge (1, 7, 15) | Spiral Tempest (30, 13, 25) |
| Iron Mace | Uncommon | 2 heavy swings + ground slam | Fracturing Impact (1, 7, 15) | Crushing Sequence (30, 11, 25) |
| Reefsaw | Uncommon | 3 heavy cuts + ground strike | Serrated Fury (10, 8, 15) | Crimson Grinder (60, 12, 25) |
| Triple Katana | Uncommon | 4 strikes + forward dash | Azure Barrage (20, 7.5, 15) | Azure Pursuit (60, 12, 25) |
| Boiler Pipe | Rare | 3 blunt strikes + sweep | Ignition Strike (1, 7.5, 15) | Ember Serpent (50, 10.5, 25) |
| Stonewall Blade | Rare | 4 heavy alternating strikes | Azure Rupture (35, 9, 15) | Rising Vortex (80, 15, 25) |
| Twinfang Glaive | Rare | alternating spins + spinning finisher | Revolving Edge (20, 7.5, 15) | Triple Vortex (100, 10, 25) |
| Wraith Cane | Rare | 4 quick strikes | Spirit Lance (40, 7.5, 25) | Phantom Sequence (120, 13.5, 30) |
| Trident | Rare | 2 sweeps, thrust + lunge | Tidal Grapple (50, 6, 30) | Abyssal Surge (125, 11, 25) |
| Bisento | Legendary | 3 sweeps + ground slam | Atmospheric Cleave (50, 9, 20) | Seismic Core (150, 13.5, 30) |
| Stormstaff | Legendary | 3 staff strikes + wide sweep | Storm Detonation (80, 7.5, 15) | Thunderfall (180, 10.5, 25) |
| Saber | Legendary | 3 swings + powerful thrust | Crimson Onslaught (50, 7.5, 15) | Scarlet Crossfire (125, 14, 30) |
| Eclipse Blade | Mythical | 3 swings + forward dash | Emerald Blade Storm (1, 8.5, 20) | Eclipse Cleaver (50, 13, 40) |

Trident's Abyssal Surge had no energy cost in the spec; it uses 25.

## Upgraded forms

The Bisento and the Eclipse Blade upgrade once their own mastery reaches `swordUpgradeMastery` (config, default 300):

* **Bisento**: Atmospheric Cleave releases three pressure waves; Seismic Core forms an energy sphere at the tip, throws
  it at the target, drags nearby enemies in, swells and detonates.
* **Eclipse Blade**: both moves become hold-to-charge. Emerald Blade Storm held before release dashes forward with the
  barrage; Eclipse Cleaver held flies faster and further the longer it charged. Un-upgraded, both fire on press.

## Normal attacks

Every swing (hit or miss) advances the combo; the server paces swings by the sword's attack speed and resets the combo
after 1.5 s without a swing. Each swing draws its blade trail in the sword's colours; the last swing is the finisher,
which hits its own area on top of the normal melee hit.

## Names

The Reefsaw, Boiler Pipe, Stonewall Blade, Twinfang Glaive, Wraith Cane, Stormstaff and Eclipse Blade use Grand Line's
own names; their textures are the supplied icons (`art/sword_icons/`, original file names).

## Code

* `gear/Gear.java` registers the items (ids `katana`, `cutlass`, `saber`, `trident` are unchanged, so ownership and
  mastery carry over).
* `gear/sword/`: `Swords` (combo, palette, rarity), `SwordMovesA`/`SwordMovesB` (the 30 moves), `SwordCombos`,
  `SwordShot` (travelling hit volumes), `Stuns`, `SwordUpgrades`, `SwordKit` (hit shapes, beats, layered sounds).
* Client: `fx/SwordFx`, `fx/SwordShapes`, `fx/SwordPalette` draw every beat; `render/sword/HeldSwords` and three mixins
  put one blade in each hand for the Dual Katana and a third blade in the mouth for the Triple Katana.
* Sounds are layered vanilla sounds (no new audio files).
* `/grandline admin cast <player> <slot> [charge]` fires a move of the held sword at once (testing, recording).
