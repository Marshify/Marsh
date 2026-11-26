# Grand Line — Claude Code guide

Minecraft **1.20.1 Fabric** mod (One Piece-style RPG). Mod id `grandline`, package `com.grandline`.
Branch: `claude/grand-line-mod-setup-cl0e4n`. Do not upgrade Minecraft, Java, Fabric, Gradle or Loom unless asked.

## Stack (keep as is)
- Gradle wrapper 9.9.1, Loom `net.fabricmc.fabric-loom-remap` 1.19.4, loader 0.20.1, Fabric API 0.92.12+1.20.1,
  Mojang official mappings. Versions live in `gradle.properties` (`mod_version` is bumped per release).
- Target **Java 17** (`options.release = 17`): no Java 21+ language/API features. Gradle itself must run on **JDK 25+** (Loom 1.19 requirement); the jar still targets 17.
- Optional dev-only runtime: Photon/LDLib. Must stay compatible with Sodium, Iris and BSL shaders.

## Build and test
| Task | Bash | PowerShell (Windows) |
|---|---|---|
| Compile + jar | `./gradlew build -x test -x runGameTest` | `tools/dev/build.ps1` |
| Unit tests | `./gradlew test` | `tools/dev/test.ps1 -Unit` |
| Game tests | `./gradlew runGameTest` → `build/reports/gametest/server-gametests.xml` | `tools/dev/test.ps1` |
| Dev client / server | `./gradlew runClient` / `runServer` | `tools/dev/client.ps1` |
| Logs / crash | `run/logs/latest.log`, `run/crash-reports/` | `tools/dev/logs.ps1 [-Crash]` |
| Review changes | `git status`, `git diff --stat` | `tools/dev/review.ps1` |

Release: bump `mod_version`, build, copy `build/libs/grandline-<ver>.jar` to `dist/` (`build.ps1 -Dist`), add a
`CHANGELOG.md` entry. Report: what changed, tests run (only ones that really ran), jar link
`https://github.com/Marshify/Marsh/raw/claude/grand-line-mod-setup-cl0e4n/dist/grandline-<ver>.jar`.

## Architecture (details: `ARCHITECTURE.md`)
- Server is the only authority; clients send intents, never values. One write path per state
  (`PlayerProfileService`, `AbilityCooldownService`).
- Source sets: `src/main` (common/server + assets), `src/client` (client only; main must not reference it),
  `src/test` (JUnit), `src/gametest` (Fabric GameTest, separate test mod).
- Style: tabs, small focused classes, Javadoc on public types, records/switch expressions fine (Java 17).
- **Reuse before writing new systems**: `AbilityDef` (INSTANT/CHARGED/TOGGLE/HELD), `ChargeService`,
  `FruitStrikes`, `TimedEffects`, `FxEvents`, `KineticBody`, `MovementGuards`, `CombatContext`, `AbilityHelpers`,
  `Traps`, `AttachedCharges`, `HealthCosts`, `DamageZones`, `GroundSpikes`, `ElementalReflex`. Search with Grep first.

## Standards
- **Fruits** (`com.grandline.fruit.<name>`): moves server-side in `<Name>Moves`, client FX in
  `client/fx/<Name>Fx`; spec doc `docs/<FRUIT>.md`; roster `docs/FRUIT_ROSTER.md`. Each rework adds game tests
  and is verified in game. Never change another fruit's moveset unless asked.
- **VFX**: data-driven library, `docs/VFX.md`. Effects are JSON in `assets/grandline/vfx/effects/<category>/`;
  play from server with `combat.fx.Vfx.play(...)`. Import art only through `tools/vfx/import_vfx.py` with a
  confirmed licence. Respect quality presets and particle budgets; must fall back when a shader pack is active.
- **Names**: display renames only in `assets/grandline/names.json` (`docs/NAMES.md`). Never rename registry ids,
  island ids or translation keys to change a display name.
- **NPCs**: per-island docs (`docs/*_NPCS.md`, `ISLAND_NPCS.md`), enemy AI `docs/ENEMY_AI.md`; elemental
  thresholds in `data/grandline/elemental_reflex/`.
- **World generation**: islands are versioned layouts (`docs/STRUCTURES.md`, `ISLAND_POSITIONS.md`); add a new
  layout version instead of editing one existing worlds already use.
- **Sounds**: `tools/sounds/incoming` → `tools/sounds/sync_sounds.py` (`docs/AUDIO.md`).
  The ElevenLabs key comes only from an environment variable; never hardcode or commit credentials.
- **Performance target**: GTX 1660 Ti at 60 fps with Sodium + Iris/BSL. Pool particles, cull behind camera and
  by distance, no per-tick allocations in render loops, no new shader passes without a fallback.
- **Save compatibility**: never remove or rename registry ids, attachment keys or NBT fields; migrate with a
  version bump; old worlds must load.

## Assets
`src/main/resources/assets/grandline/`: `textures/{item,block,entity,gui,fx}` (+ `vfx/` for imported art), `models`, `lang/en_us.json`,
`sounds.json` + `sounds/`, `shaders/`, `vfx/`, `music/`. Source art in `art/`, generator scripts in `tools/`.

## Git
- Commit only on the branch above; **ask before pushing**, before destructive commands (reset, clean,
  force push, deleting branches) and before dependency upgrades. Preserve uncommitted work.
- Never commit secrets, `run/`, `build/`, or test artefacts.

## Token efficiency
- Do not list or read `build/`, `.gradle/`, `run/` (except logs), `dist/`, `logs/*.gz`, `art/`, binary files or
  `build/libs` (hundreds of old jars). Grep for symbols, then read only the needed range.
- Read the task's own doc in `docs/` rather than the whole tree. Filter Gradle output to errors
  (`build.ps1` does this). Keep reports short.
