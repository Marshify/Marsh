# Grand Line — Architecture

## Guiding rules

1. **The server is the only authority.** Clients send *intents* ("I pressed ability slot 2", "please resend my
   profile"). They never send values (damage, XP, currency, cooldown state, inventory changes).
2. **One write path per piece of state.** Player profiles are only changed through `PlayerProfileService`;
   cooldowns only through `AbilityCooldownService`.
3. **Client code is physically separated.** Loom's split source sets put client-only classes in
   `src/client/java`; the common code in `src/main/java` cannot reference them, so a dedicated server never loads
   client classes.
4. **Pure logic is kept free of Minecraft state** (e.g. `ProgressionRules`) so it can be unit tested.

## Source sets

| Source set | Path | Contents | Ships in jar |
|---|---|---|---|
| `main` | `src/main/java`, `src/main/resources` | Common + server logic, assets | yes |
| `client` | `src/client/java` | Client entrypoint, client networking, client cache, client commands | yes |
| `test` | `src/test/java` | JUnit unit tests (run with `fabric-loader-junit`) | no |
| `gametest` | `src/gametest/java` | Server game tests + client end-to-end game test (separate test mod `grandline-test`) | no |

## Packages (`com.grandline`)

| Package | Responsibility | Milestone 1 contents |
|---|---|---|
| *(root)* | `GrandLineMod` — common entrypoint; wires subsystems, owns `MOD_ID`, logger, `id()` | done |
| `registry` | Registries: items, creative tabs (later blocks, entities, sounds) | `ModItems` (Grand Line Crystal), `ModCreativeTabs` |
| `player` | Player profile model, progression rules, profile service, lifecycle events | `PlayerProfile`, `ProgressionRules`, `PlayerProfileService`, `PlayerProfileEvents` |
| `data` | Persistent data definitions (attachments; later world `SavedData`) | `ModAttachments` |
| `network` | Packet registration, request validation | `GrandLineNetworking`, `ServerRequestGuard`, `payload/*` |
| `combat` | Damage, abilities, weapons, fighting styles | `combat.ability`: cooldown state/service, activation validation pipeline (no abilities yet) |
| `command` | Brigadier command trees | `GrandLineCommands` — **added** (not in the original plan) so commands don't live in the entrypoint or in gameplay packages |
| `config` | Mod configuration | `GrandLineConfig` (JSON, validated) |
| `fruit` | Fruit definitions, rarity, consumption, mastery | reserved (`package-info` only) |
| `world` | Islands, structures, oceans, worldgen | reserved |
| `quest` | Quests, NPC interactions, rewards | reserved |
| `entity` | Mobs, bosses, NPCs | reserved |
| `client` (client source set) | Screens, HUD, rendering, client networking | `GrandLineClient`, `ClientProfileCache`, `ClientNetworking`, `/grandlineclient` |

## Player profile & persistence

```
PlayerProfile (immutable record)
  dataVersion, level, experience, currency, unallocatedStatPoints
```

- Stored as a **Fabric Data Attachment** (`grandline:player_profile`) on the `ServerPlayer`, registered with a
  `Codec` (persistent) and `copyOnDeath()` (survives respawn).
- Persistent attachments are written into the vanilla per-player data file under the world's player-data
  directory, under the `fabric:attachments` NBT key. Vanilla saves this on autosave, logout and shutdown, writing
  a temp file and keeping a backup of the previous file, so a crash during save does not corrupt progression.
  (This is verified by the client game test, which reads the raw `.dat` file from a dedicated server.)
- **Safe loading:** every codec field is optional with a default (old/partial saves load), and decoded values are
  passed through `sanitized()` (no negative currency, level ≥ 1, …). `dataVersion` is reserved for migrations.
- Profiles are created lazily with defaults on first access (and eagerly on join).

`ProgressionRules` (pure maths): XP from level *L* to *L+1* = `base + growth × (L−1)`; each level grants
`statPointsPerLevel`; level is capped at `maxLevel`; all arithmetic saturates instead of overflowing.

## Networking

All payloads are `CustomPacketPayload` records registered in `GrandLineNetworking.registerCommon()`.

| Payload | Direction | Purpose |
|---|---|---|
| `ProfileSyncPayload` | S→C | Authoritative profile snapshot for the receiving player |
| `CooldownSyncPayload` | S→C | Active ability cooldowns (remaining/total ticks), capped at 256 entries |
| `RequestProfileSyncPayload` | C→S | "Resend my profile & cooldowns" (no data) |
| `AbilityActivationRequestPayload` | C→S | "I pressed ability slot *n*" (intent only) |

**Why a client cannot change authoritative data**

1. Clientbound payloads are registered only with `PayloadTypeRegistry.clientboundPlay()`. The server has no
   decoder or handler for them, so a modified client cannot push a profile to the server.
2. Serverbound payloads carry no authoritative values, only intents.
3. Every serverbound handler first passes `ServerRequestGuard.check()` (player must be connected and not
   removed; per-player, per-type rate limit from config), then validates its own content (e.g. slot range,
   alive, not spectator). Rejections are counted and logged at debug level.
4. Handlers run on the server thread (Fabric's default for play payloads), so there are no data races.

**Sync triggers:** player join (play-ready), respawn, every profile change via `PlayerProfileService.update`,
every cooldown change, and on client request (rate limited). The client stores what it receives in
`ClientProfileCache`, which is display-only and cleared on disconnect.

### Ability activation (infrastructure only)

`AbilityActivationHandler.handle()` implements guard → slot validation → player-state validation. **No abilities
exist yet**, so every valid request ends in `NO_ABILITY_BOUND`. The fruit/combat milestones will add ability
lookup, server-side cooldown checks (`AbilityCooldownService`) and execution at that point.

## Commands

`/grandline` (server, Brigadier) — see README for the full table. Admin subcommands require permission level 2
(`Commands.LEVEL_GAMEMASTERS`) and are logged. `/grandlineclient profile` is a client-side command that prints the
client's synced copy, to verify sync.

## Testing layers

| Layer | Tool | Covers |
|---|---|---|
| Unit | JUnit 5 + `fabric-loader-junit` (`./gradlew test`) | Progression maths, overflow, codec round-trip, corrupted-data sanitizing, network codec |
| Server game tests | Fabric GameTest (`./gradlew runGameTest`) | Item registration, default profiles, profile save/load via vanilla player NBT, cooldowns not persisted, request validation/rate limiting cannot modify profile |
| Client end-to-end | Fabric Client GameTest (`./gradlew runClientGameTest`) | Real client: item visible, commands as player (incl. permission denial), sync to client, raw malicious packets rejected, world close/reopen persistence, dedicated server join, on-disk player file contents, reconnect persistence |
