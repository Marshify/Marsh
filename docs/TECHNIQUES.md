# Techniques: Dash, Flash Step, Aura, Instinct (0.56.0)

All four are handled on the server (`technique/`); clients only send key presses and draw. Numbers are in the config
(`dash*`, `flashStep*`, `aura*`, `instinct*`, `elementalImmunity`). Energy costs are a share of max energy (10 = 10%).

| | Key | Unlock | Cost | Cooldown / limit |
|---|---|---|---|---|
| Dash | Left Shift | everyone, level 1 | 10% energy | 0.8 s |
| Flash Step | R | Ability Master (Frostpeak Isle), level 100, $100,000 | 10% energy | 15 s, 20 blocks |
| Aura | J (toggle) | Ability Master (Frostpeak Isle), $25,000 | none | — |
| Instinct | E (toggle) | Ability Master (upper Cloudtop Isles), level 300, $750,000 | none | dodge charges |

## Controls moved (once, on first start of 0.56.0)

| Control | Was | Now |
|---|---|---|
| Shift Lock | Left Shift | Left Alt |
| Race ability | R | O |
| Call Ship | J | P (vanilla Social Interactions loses P) |
| Vanilla inventory | E | Caps Lock (Grand Line's Items screen stays on I) |
| Sneak | Left Shift | unbound (rebindable; riding still dismounts with Left Shift) |

A key rebound afterwards in Controls is never moved again.

## Trainers
- **Movement Instructor** (Millbrook Village, Ironhold Bastion: the two starting islands): explains Dash, free tutorial.
- **Ability Master** (custom skin `textures/entity/ability_master.png`, player model): on Frostpeak Isle, in the ice
  cavern a few steps east of the campsite's campfire (beside the barrels, facing the fire;
  `FrostpeakIsle.ABILITY_MASTER_SPOT`), he teaches Flash Step and Aura (first page: Flash Step / Aura / Leave, each with its own page and Back); on the upper
  Cloudtop Isles, beside that region's quest giver, he teaches Instinct. Internally these are the roles
  `FLASH_STEP_MASTER` and `INSTINCT_SAGE` (`AURA_MASTER` is kept unused so saved role ids never shift).
- Placed by `KeeperNpcs` in new and existing worlds alike (never twice). A keeper whose spot changes (the Ability Master,
  first placed on the village square) is walked over to the new spot, not doubled. On a built terrace (Frostpeak's village) the
  free spot is searched at the anchor's own height.

## HUD
The move panel (bottom right, every power: fighting style, fruit, sword, gun) is drawn at ~75% size (rounded to
half-pixel steps per GUI scale, full size at GUI scale 1), on 25%-opaque backing with no borders, so the held item shows
through. Names, keys (gold) and cooldowns stay opaque with shadows; the mastery bar is thin and softened.

Tiles just right of the hotbar's centre (the chat box covers the left): Dash, Flash Step (hidden until bought), Aura
(stage number while on), Instinct (charges, a bar for the next one). Raised a row while the air bubbles show. Key names
come from Controls. The Stats screen (K) has a Techniques panel with Aura stage/XP/bonus and Instinct charges/XP.

## Dash
Direction from W/A/S/D (diagonals normalized; nothing held = forward). 4 blocks over 4 ticks, stops at walls, holds the
fall in the air. Not while stunned, riding or without energy. Holding the key does not repeat it. The Movement
Instructor (Millbrook Village, the Marines' island) explains it and runs a tutorial (dash forward, back, left, right and
in the air).

## Flash Step
Destination along the crosshair: never through walls, the whole body must fit, only loaded chunks inside the border and
build height; first the farthest spot along the aim where the player can stand (up to ~2.6 blocks below the ray: just over a
ledge's edge, a lower deck), else the farthest point in the air. No
valid spot: cancelled, no energy, no cooldown. Fall distance is reset; the camera keeps its angle.

## Aura
Toggle. Stages from Aura XP: 0 / 4,000 / 12,000 / 24,000 / 48,000 / 60,000. Physical damage (punches, swords, guns,
fighting-style, sword and gun moves) +10%; damage taken −0/5/10/15/20/30%. Applied per hit, stats never change.
XP: a landed physical hit with Aura on gives XP equal to its damage (capped at 25, at most one hit every 4 ticks);
islanders, armour stands and same-side players give none. Coating: hands → arms → torso → head → upper legs → whole
body (`AuraLayer`, wide and slim models, first-person arm), the held weapon darkened (`CoatedBuffers`). The coating tint
is a single parameter, ready for coloured Aura later.

**Elemental Reflex** (replaces the old elemental immunity): see docs/SMOKE.md. Only fruits that register a profile have it (Smoke so far); it is checked before Instinct, so a negated hit never spends a dodge.

## Instinct
Toggle. Evades combat attacks (an attacker is behind them: melee, swords, guns, moves, bosses), one charge each:
damage, knockback, stuns, fire and harmful effects of that hit are all cancelled. Never evades falls, drowning, fire,
lava, the void, the ocean's weakness or `/kill`-type damage. Moves marked `instinctBreaking` land anyway: Scarlet
Crossfire, Eclipse Cleaver, Seismic Core, Thunderfall, Inferno Nova and the Saber Warden's combo finisher
(`AbilityDef.breaksInstinct()`, `CombatContext.breaking`).
Charges: 2 at first, then 3/4/5/6/7/8 at 50/350/850/1,400/2,100/2,800 Instinct XP (1 XP per evasion). One spent charge
returns every 30 s of play, on or off; toggling and reconnecting never refill.
Detection (32 blocks, this player only): outlines through walls (hostile and enemy players red, bosses gold, allies
green) and labels with name and health, drawn through walls (`WorldRenderEvents.LAST`). Islanders, animals, items and armour stands are never marked.

## Data
`ModAttachments.TECHNIQUES` (persistent, kept through death): ownership, Aura XP, Instinct XP, charges and regeneration
progress. Toggles and cooldowns are per session. `/grandline admin technique <player> grant|reset|auraxp|instinctxp`.
