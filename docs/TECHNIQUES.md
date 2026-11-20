# Techniques: Dash, Flash Step, Aura, Instinct (0.56.0)

All four are handled on the server (`technique/`); clients only send key presses and draw. Numbers are in the config
(`dash*`, `flashStep*`, `aura*`, `instinct*`, `elementalImmunity`). Energy costs are a share of max energy (10 = 10%).

| | Key | Unlock | Cost | Cooldown / limit |
|---|---|---|---|---|
| Dash | Left Shift | everyone, level 1 | 10% energy | 0.8 s |
| Flash Step | R | Flash Step Master (Frostpeak Isle), level 100, $100,000 | 10% energy | 15 s, 20 blocks |
| Aura | J (toggle) | Aura Master (Frostpeak Isle), $25,000 | none | — |
| Instinct | E (toggle) | Instinct Sage (upper Cloudtop Isles), level 300, $750,000 | none | dodge charges |

## Controls moved (once, on first start of 0.56.0)

| Control | Was | Now |
|---|---|---|
| Shift Lock | Left Shift | Left Alt |
| Race ability | R | O |
| Call Ship | J | P (vanilla Social Interactions loses P) |
| Vanilla inventory | E | Caps Lock (Grand Line's Items screen stays on I) |
| Sneak | Left Shift | unbound (rebindable; riding still dismounts with Left Shift) |

A key rebound afterwards in Controls is never moved again.

## Dash
Direction from W/A/S/D (diagonals normalized; nothing held = forward). 4 blocks over 4 ticks, stops at walls, holds the
fall in the air. Not while stunned, riding or without energy. Holding the key does not repeat it. The Movement
Instructor (Millbrook Village, the Marines' island) explains it and runs a tutorial (dash forward, back, left, right and
in the air).

## Flash Step
Destination along the crosshair: never through walls, the whole body must fit, only loaded chunks inside the border and
build height; prefers standing on ground just below the aimed point (ledges, lower decks), else hangs in the air. No
valid spot: cancelled, no energy, no cooldown. Fall distance is reset; the camera keeps its angle.

## Aura
Toggle. Stages from Aura XP: 0 / 4,000 / 12,000 / 24,000 / 48,000 / 60,000. Physical damage (punches, swords, guns,
fighting-style, sword and gun moves) +10%; damage taken −0/5/10/15/20/30%. Applied per hit, stats never change.
XP: a landed physical hit with Aura on gives XP equal to its damage (capped at 25, at most one hit every 4 ticks);
islanders, armour stands and same-side players give none. Coating: hands → arms → torso → head → upper legs → whole
body (`AuraLayer`, wide and slim models, first-person arm), the held weapon darkened (`CoatedBuffers`). The coating tint
is a single parameter, ready for coloured Aura later.

**Elemental immunity** (new): players with Smoke, Sand, Ice, Light, Magma, Lightning, Gas or Blizzard cannot be hurt by
other players' plain physical attacks; Aura breaks through, for that hit only. Enemies still hit them normally.

## Instinct
Toggle. Evades combat attacks (an attacker is behind them: melee, swords, guns, moves, bosses), one charge each:
damage, knockback, stuns, fire and harmful effects of that hit are all cancelled. Never evades falls, drowning, fire,
lava, the void, the ocean's weakness or `/kill`-type damage. Moves marked `instinctBreaking` land anyway: Scarlet
Crossfire, Eclipse Cleaver, Seismic Core, Thunderfall, Inferno Nova and the Saber Warden's combo finisher
(`AbilityDef.breaksInstinct()`, `CombatContext.breaking`).
Charges: 2 at first, then 3/4/5/6/7/8 at 50/350/850/1,400/2,100/2,800 Instinct XP (1 XP per evasion). One spent charge
returns every 30 s of play, on or off; toggling and reconnecting never refill.
Detection (32 blocks, this player only): outlines through walls (hostile and enemy players red, bosses gold, allies
green) and labels with name, level and health. Islanders, animals, items and armour stands are never marked.

## Data
`ModAttachments.TECHNIQUES` (persistent, kept through death): ownership, Aura XP, Instinct XP, charges and regeneration
progress. Toggles and cooldowns are per session. `/grandline admin technique <player> grant|reset|auraxp|instinctxp`.
