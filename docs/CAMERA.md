# Action camera, Shift Lock and no crouching (0.37.0)

The camera is client-side (`client/camera/`), plus a server rule for crouching (`player/rules/SneakRules`).

## Modes

The action camera is on by default and runs in the third-person back view (F5 still cycles views; first person and the front view stay vanilla).

| | Exploration (default) | Shift Lock (Left Shift toggles) |
|---|---|---|
| Camera | Orbits freely through 360° horizontally, ±80° vertically, about 7.5 blocks back and slightly raised | Same, but over the right shoulder |
| Character | Turns quickly to face where it moves (W away from the camera, S towards it) | Always faces the camera's direction; WASD strafes and walks backwards |
| Reticle | A faint centre dot | A small ring with a dot |

The ring and the shoulder view follow **only** the Shift Lock toggle: using a move without Shift Lock keeps the dot and the centred view.

## Aim

**What you aim at always follows the camera.**
- **Before acting:** before any attack, item use, ability or race ability, the character turns so its eyes point at exactly what the reticle is on (the first block or entity along the camera's ray, up to 96 blocks), and that rotation is sent to the server **before** the action's own packet. The reticle, the attack and the server's aim check therefore agree, and server validation is unchanged.
- **Holding the aim:** facing then stays locked to the camera for 1.5 s (and while a move key is held), so multi-hit and charged moves keep their aim.
- **Targets:** the block or entity outlined is the one under the reticle: the pick is the camera's own ray through the screen centre, starting at the character (so nothing behind them is picked). Reach is still measured from the character's eyes as in vanilla (blocks: the game mode's reach; entities: 3 blocks, 6 in creative), so the server accepts every use and attack.

## Following and collision

- **Distance:** it eases out smoothly, but snaps in at once when a block is in the way (vanilla's 8-ray check, so no clipping).
- **Jumps and landings:** a slight height lag.
- **Smoothing:** frame-rate independent and allocation-free. Mouse turning is applied directly, with no smoothing, so it adds no input lag.

## Movement responses

These are subtle and off with **Reduced camera motion**.

| Situation | Response |
|---|---|
| Sprint | +0.6 blocks |
| Dash or fast burst (over 0.75 blocks per tick) | +1.2 blocks and +5% FOV, easing back |
| Swimming | −1.5 blocks |
| On a ship or boat | +3.5 blocks (kept even with reduced motion) |
| Holding a charge | +1.5 blocks, +4% FOV |
| Aiming a gun (holding use) | −2.5 blocks, −12% FOV, shoulder view |
| Saber Warden within 28 blocks | +1.5 blocks |

The camera never rotates by itself, and camera shake keeps its own setting. The FOV starts from your Minecraft FOV setting, which these responses only scale.

## Settings and keys

**Settings → Camera:**
- Action camera (on/off)
- Camera distance (near 5.5, normal 7.5, far 9.5)
- Camera turn speed (on top of Minecraft's mouse sensitivity)
- Reduced camera motion

**Keys** (Controls → Grand Line):
- Shift Lock: Left Shift
- Camera zoom in / out: `=` / `-`

## No crouching

- **Survival and adventure, on foot:** players never crouch. The client never sends it, and the server clears it if anything does.
- **Riding a ship or boat:** Shift still dismounts.
- **Creative and spectator:** Shift still flies down.
- **The Sneak key:** vanilla Sneak is unbound once on first join so Left Shift belongs to Shift Lock without a conflict. Either can be rebound in Controls.
- **Unchanged:** player size, collision and the vanilla forced crawl or crouch in low spaces.

## Not included

- There is no Flash Step ability in the mod yet. Dashes are caught by their speed.
- The camera has no cinematic moves.
