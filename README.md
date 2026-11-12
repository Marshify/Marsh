# Grand Line

A multiplayer RPG mod for **Minecraft Java Edition 1.20.1** on **Fabric**, inspired by the progression,
exploration, combat and fruit-ability loops of action RPGs like Blox Fruits. All content is original.

> **Status: Milestone 2 (stats, HUD & mob rewards) — version 0.2.0.** Players level up by killing hostile mobs,
> spend stat points on Melee / Defense / Sword (Gun and Fruit are stored for later milestones), and see their
> progress on a HUD and a stats screen (**K**). No fruits, abilities or world generation yet — see [ROADMAP.md](ROADMAP.md).

| | |
|---|---|
| Mod ID | `grandline` |
| Base package | `com.grandline` |
| Minecraft | 1.20.1 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.92.12+1.20.1 (latest for 1.20.1) |
| Mappings | Mojang official (`loom.officialMojangMappings()`) |
| Fabric Loom | 1.19.4 (`net.fabricmc.fabric-loom-remap`) |
| Gradle | 9.9.1 (wrapper) |
| Java | **17** bytecode for players; **JDK 25** needed to *build* (Loom 1.19 requires it) |

Versions were checked against `meta.fabricmc.net` and `maven.fabricmc.net` on 2026-11-12.

| HUD + kill reward | Stats screen (K) |
|---|---|
| ![HUD showing level, XP bar, currency and a kill reward](docs/images/m2-hud-kill-reward.png) | ![Stats screen with allocation buttons](docs/images/m2-stats-screen.png) |

## Requirements

- **To build:** JDK 25 — Fabric Loom 1.19 refuses to run on older JVMs. Set `JAVA_HOME` to a JDK 25 install
  (e.g. [Eclipse Temurin](https://adoptium.net/)). The compiled mod targets Java 17.
- **To play:** Minecraft 1.20.1 with Fabric Loader ≥ 0.20.1, Fabric API for 1.20.1, Java 17 or newer.
- Internet access to `maven.fabricmc.net`, `meta.fabricmc.net`, Mojang's
  `piston-meta`/`piston-data`/`libraries.minecraft.net`/`resources.download.minecraft.net`, Maven Central
  and `services.gradle.org` (first build only; everything is cached afterwards).
- No local Gradle install needed — use the wrapper.

## Build

```bash
export JAVA_HOME=/path/to/jdk-25
./gradlew build
```

`build` compiles everything, runs the JUnit unit tests **and** the headless server game tests.

The release jar is written to:

```
build/libs/grandline-0.2.0.jar          # the mod — put this in a server's or client's mods/ folder
build/libs/grandline-0.2.0-sources.jar  # sources
```

Install it alongside **Fabric API 0.92.12+1.20.1** (or newer for 1.20.1) on Fabric Loader ≥ 0.20.1.
The mod is required on **both** client and server (it adds an item and custom packets).

## Run in development

| Command | What it does |
|---|---|
| `./gradlew runClient` | Launches the Minecraft client with the mod (run dir: `run/`) |
| `./gradlew runServer` | Launches a dedicated server with the mod (run dir: `run-server/`, so client and server can run together). Accept the EULA in `run-server/eula.txt` on first run. |
| `./gradlew test` | JUnit unit tests (progression maths, codecs) |
| `./gradlew runGameTest` | Headless server game tests; report at `build/reports/gametest/server-gametests.xml` |

IDE: open the folder in IntelliJ IDEA and set the Gradle JVM to JDK 25.

To test multiplayer locally: start `./gradlew runServer`, then
`./gradlew runClient --args="--quickPlayMultiplayer localhost:25565 --username Tester"` (set `online-mode=false`
in `run-server/server.properties` for offline dev accounts).

## Manual test checklist (in game)

1. `./gradlew runClient`, create a creative world with cheats on.
2. Open the **Grand Line** creative tab — the *Grand Line Crystal* should be there with its texture.
   Or: `/give @s grandline:grand_line_crystal`.
3. `/grandline test` — prints the mod version, server type, and `Grand Line Crystal registered: yes`.
4. `/grandline profile` — shows level 1, 0 XP, 0 currency, 0 stat points.
5. `/grandline admin addxp @s 260` → `/grandline profile` shows level 3, 10 XP, 6 stat points.
6. `/grandline admin addcurrency @s 500`, `/grandline admin setlevel @s 10`, `/grandline admin reset @s`.
7. `/grandlineclient profile` — the **client-side** synced copy; it should match the server's values.
8. Save & quit, re-open the world → profile is unchanged. Die and respawn → profile is unchanged.
9. The HUD (top left) shows level, XP bar, currency and unspent stat points.
10. Press **K** → Stats screen. Click **+5** on Defense → hearts increase (5 points = +1 half-heart by default).
11. Kill a zombie (survival or creative) → action bar shows `+20 XP  +10 currency`; the HUD updates.
12. Hold nothing and hit a mob with Melee points, or hold a sword with Sword points → more damage.

### Commands

| Command | Permission |
|---|---|
| `/grandline test` | everyone |
| `/grandline profile` | everyone (own profile) |
| `/grandline profile <player>` | op level 2 |
| `/grandline admin addxp <players> <amount>` | op level 2 |
| `/grandline admin addcurrency <players> <amount>` (negative removes) | op level 2 |
| `/grandline admin setlevel <players> <level>` | op level 2 |
| `/grandline admin reset <players>` | op level 2 |
| `/grandline admin resetstats <players>` (refunds invested points) | op level 2 |
| `/grandline stats` / `/grandline stats <player>` | everyone (own) / op level 2 |
| `/grandline allocate <melee\|defense\|sword\|gun\|fruit> <amount>` | everyone (own points, server-validated) |
| `/grandlineclient profile` (client-side command) | everyone |

## Configuration

`config/grandline.json` is created on first start (server side). Values are validated and clamped on load;
a malformed file is renamed to `grandline.json.broken` and replaced with defaults.

| Key | Default | Meaning |
|---|---|---|
| `maxLevel` | 2800 | Level cap |
| `statPointsPerLevel` | 3 | Unallocated stat points granted per level |
| `baseExperiencePerLevel` | 100 | XP from level 1 → 2 |
| `experienceGrowthPerLevel` | 50 | Extra XP required per level beyond the first |
| `startingCurrency` | 0 | Currency for new profiles |
| `maxCurrency` | 1 000 000 000 000 | Currency cap |
| `requestCooldownTicks` | 4 | Minimum ticks between accepted client requests of one type |
| `maxPointsPerStat` | 2800 | Cap on points in one stat |
| `defenseHealthPerPoint` / `maxBonusHealth` | 0.2 / 200 | Max health (half-hearts) per Defense point, and its cap |
| `meleeDamagePerPoint` | 0.05 | Attack damage per Melee point (empty main hand) |
| `swordDamagePerPoint` | 0.05 | Attack damage per Sword point (holding a sword) |
| `maxBonusDamage` | 100 | Cap for the Melee and Sword bonuses |
| `experiencePerMobHealth` / `currencyPerMobHealth` | 1.0 / 0.5 | Kill reward per point of the hostile mob's max health (zombie = 20 XP, 10 currency) |
| `maxKillExperiencePerMinute` / `maxKillCurrencyPerMinute` | 5000 / 2500 | Anti-farm caps per player per minute (0 = no cap) |

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) — packages, data flow, networking & persistence design
- [ROADMAP.md](ROADMAP.md) — milestones
- [DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md) — what was done, verified and left open
