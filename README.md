# Grand Line

A multiplayer RPG mod for **Minecraft Java Edition 26.3** on **Fabric**, inspired by the progression,
exploration, combat and fruit-ability loops of action RPGs like Blox Fruits. All content is original.

> **Status: Milestone 1 (foundation).** The project builds, loads on client and dedicated server, registers a
> test item, stores a persistent per-player profile, and has a validated networking layer. There is no fruit,
> combat or world-generation gameplay yet — see [ROADMAP.md](ROADMAP.md).

| | |
|---|---|
| Mod ID | `grandline` |
| Base package | `com.grandline` |
| Minecraft | 26.3 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.166.2+26.3 |
| Fabric Loom | 1.19.4 (`net.fabricmc.fabric-loom`, non-remapping) |
| Gradle | 9.9.1 (wrapper) |
| Java | 25 (Temurin 25.0.5 used for development) |

Versions were checked against `meta.fabricmc.net`, `maven.fabricmc.net` and the official
[Fabric for Minecraft 26.3](https://fabricmc.net/2026/09/15/263.html) post on 2026-11-12.

## Requirements

- **JDK 25** — Gradle itself must run on Java 25 for Minecraft 26.x. Set `JAVA_HOME` to a JDK 25 install.
  Any distribution works (e.g. [Eclipse Temurin](https://adoptium.net/)).
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
build/libs/grandline-0.1.0.jar          # the mod — put this in a server's or client's mods/ folder
build/libs/grandline-0.1.0-sources.jar  # sources
```

Install it alongside **Fabric API 0.166.2+26.3** (or newer for 26.3) on Fabric Loader ≥ 0.20.1.
The mod is required on **both** client and server (it adds an item and custom packets).

## Run in development

| Command | What it does |
|---|---|
| `./gradlew runClient` | Launches the Minecraft client with the mod (run dir: `run/`) |
| `./gradlew runServer` | Launches a dedicated server with the mod (run dir: `run/`). Accept the EULA in `run/eula.txt` on first run. |
| `./gradlew test` | JUnit unit tests (progression maths, codecs) |
| `./gradlew runGameTest` | Headless server game tests; report at `build/reports/gametest/server-gametests.xml` |
| `./gradlew runClientGameTest` | Boots a real client, creates a single-player world **and** a dedicated server, and runs the end-to-end suite. Needs a display — on a headless Linux box use `xvfb-run -a ./gradlew runClientGameTest`. Screenshots land in `build/run/clientGameTest/screenshots/`. |

IDE: open the folder in IntelliJ IDEA 2025.3+ (required for mixins on 26.x) and set the Gradle JVM to JDK 25.

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

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) — packages, data flow, networking & persistence design
- [ROADMAP.md](ROADMAP.md) — milestones
- [DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md) — what was done, verified and left open
