# Grand Line — Development Log

## 2026-11-12 — Milestone 1: Foundation

### Environment findings
- The container had only JDK 21 and Gradle 8.14; Minecraft 26.x requires Java 25 for the Gradle JVM.
  Installed **Eclipse Temurin 25.0.5+8** (SHA-256 verified) to `/opt/jdk/jdk-25.0.5+8`.
- The default cloud network policy blocked `meta.fabricmc.net`, `maven.fabricmc.net` and Mojang's servers.
  A dedicated cloud environment (`marsh`) was created with exactly the Fabric, Mojang and JDK download hosts.
- The project uses the Gradle wrapper (9.9.1); no system Gradle is needed.

### Dependency versions (verified 2026-11-12)
| Component | Version | Source |
|---|---|---|
| Minecraft | 26.3 (stable) | meta.fabricmc.net/v2/versions/game |
| Fabric Loader | 0.20.1 (latest stable) | meta.fabricmc.net/v2/versions/loader |
| Fabric API | 0.166.2+26.3 (latest for 26.3) | maven.fabricmc.net |
| Fabric Loom | 1.19.4 (latest stable; plugin `net.fabricmc.fabric-loom`) | maven.fabricmc.net |
| Gradle | 9.9.1 (current) | services.gradle.org |
| Java | 25 (Temurin 25.0.5+8) | api.adoptium.net |

Notes from the official Fabric 26.1/26.3 posts that shaped the build:
- 26.x is unobfuscated → use the non-remapping `net.fabricmc.fabric-loom` plugin, plain
  `implementation` (not `modImplementation`), no `mappings`, `jar` instead of `remapJar`.
- Fabric API now uses Mojang names (`Identifier`, `PayloadTypeRegistry.serverboundPlay()/clientboundPlay()`,
  `CreativeModeTabEvents`, …). Every API used was checked with `javap` against the actual 26.3 jars.
- The 26.3 post recommends "Loom 1.17 / Gradle 9.6.0 at the time of writing"; newer stable Loom 1.19.4 with
  Gradle 9.9.1 was used and builds cleanly.

### Completed
- Gradle/Loom project with split `main`/`client` source sets, plus `test` (JUnit) and `gametest` source sets.
- `GrandLineMod` entrypoint; `GrandLineClient` client entrypoint.
- Package structure: `registry`, `player`, `fruit`*, `combat`, `world`*, `quest`*, `entity`*, `network`,
  `config`, `data`, `command`, `client` (*reserved, no code yet). `command` was added to the planned layout.
- **Grand Line Crystal** item (`grandline:grand_line_crystal`, rare, stack 16, fire resistant) with original
  16×16 texture, item model, English name, and a **Grand Line** creative tab.
- **Persistent player profile** (level, experience, currency, unallocated stat points, data version) as a Fabric
  data attachment, copied on death, with defaulting + sanitizing codecs.
- `ProgressionRules`: XP curve, multi-level-ups, stat-point grants, level cap, saturating arithmetic.
- **Networking**: 2 clientbound (profile sync, cooldown sync) and 2 serverbound (sync request, ability
  activation intent) payloads; `ServerRequestGuard` (state check + per-type rate limit + rejection counter);
  server-authoritative cooldown tracking; ability activation validation pipeline (no abilities yet).
- **Commands**: `/grandline test`, `/grandline profile [player]`, `/grandline admin addxp|addcurrency|setlevel|reset`,
  client-side `/grandlineclient profile`.
- `config/grandline.json` with validation, atomic writes and broken-file backup.
- Docs: README, ARCHITECTURE, ROADMAP, this log.

### Verification performed
| Check | How | Result |
|---|---|---|
| Gradle build | `./gradlew build` | ✅ BUILD SUCCESSFUL, no compiler warnings in mod code |
| Unit tests | `./gradlew test` | ✅ 11/11 (ProgressionRulesTest 7, PlayerProfileCodecTest 4) |
| Mod loads on a server | `./gradlew runGameTest` (headless server) | ✅ "Grand Line 0.1.0 initialized" |
| Server game tests | `./gradlew runGameTest` | ✅ 5/5 Grand Line tests (+ vanilla `always_pass`) |
| Release jar contents | inspected `build/libs/grandline-0.1.0.jar` | ✅ mod + client classes + assets; no test code |
| Client end-to-end | `SDL_VIDEO_FORCE_EGL=1 ./gradlew runClientGameTest` on Xvfb + Mesa llvmpipe | ✅ all 16 checks (integrated + dedicated server) |

Server game tests: item registered; new players get default profile; profile survives vanilla player
save → load (and is present under `fabric:attachments` in player NBT); cooldowns are *not* persisted; invalid and
spammed ability requests are rejected and never change the profile.

Client end-to-end checks: client sees the item; `/grandline test` and `/grandline profile` run as a player;
non-operators are denied admin commands; admin changes sync to the client cache; raw invalid ability packets
from the client are rejected and do not change the profile; profile survives closing and reopening the
single-player world (full integrated-server restart); on a **dedicated server**: profile changes sync, the raw
player `.dat` file on disk contains the profile, and it reloads after disconnect/reconnect.

### Problems hit and fixed
- Minecraft 26.3 creates its window through SDL3. On Xvfb, SDL's GLX visual matching fails ("Couldn't find
  matching GLX visual") and the game blocks on an error dialog. Fix for headless runs: `SDL_VIDEO_FORCE_EGL=1`.
- `AttachmentRegistry.builder()` is deprecated in this Fabric API → switched to `AttachmentRegistry.create(id, builder -> …)`.
- A game test wrongly assumed mock players start without a profile; they go through the real join flow, which
  creates the default profile. Fixed the test, not the mod (the mod behavior is intended).
- `fabric-loader-junit` writes `logs/` in the project root → added to `.gitignore`.

### Outstanding issues / caveats
- `GameTestHelper.makeMockServerPlayerInLevel()` is marked for removal by Mojang; it is still the only helper
  giving a connected `ServerPlayer`. Used only in test code with `@SuppressWarnings("removal")`.
- Ability activation requests are validated but always end in `NO_ABILITY_BOUND` — there are no abilities yet.
- Cooldowns are intentionally not persisted across relogs (to be decided in the combat milestone).
- Translation keys are used for command output; vanilla clients without the mod would see raw keys (the mod is
  required on clients anyway).
- Offline sandbox: Mojang auth/services lookups fail in logs (`discovery.minecraftservices.com`); harmless.
- Not tested: a full dedicated-server *process restart* (covered instead by integrated-server restart + dedicated
  logout/login + reading the on-disk file); a real multi-player session with two separate human clients, and a production install via the
  Fabric installer/launcher (the dev runtime was used).

### Next milestone
Milestone 2 — stats allocation, derived attributes, HUD and XP/currency sources (see ROADMAP.md).
