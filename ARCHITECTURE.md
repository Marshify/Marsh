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
| `gametest` | `src/gametest/java` | Server game tests (separate test mod `grandline-test`) | no |

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
- Persistent attachments are written into the vanilla per-player data file (`world/playerdata/<uuid>.dat`),
  under the `fabric:attachments` NBT key. (Fabric API 0.92.x for 1.20.1 includes the data attachment API.) Vanilla saves this on autosave, logout and shutdown, writing
  a temp file and keeping a backup of the previous file, so a crash during save does not corrupt progression.
  (Verified by decoding the raw `.dat` file after a real dedicated-server shutdown; see DEVELOPMENT_LOG.md.)
- **Safe loading:** every codec field is optional with a default (old/partial saves load), and decoded values are
  passed through `sanitized()` (no negative currency, level ≥ 1, …). `dataVersion` is reserved for migrations.
- Profiles are created lazily with defaults on first access (and eagerly on join).

`ProgressionRules` (pure maths): XP from level *L* to *L+1* = `base + growth × (L−1)`; each level grants
`statPointsPerLevel`; level is capped at `maxLevel`; all arithmetic saturates instead of overflowing.

## Networking

All packets are `FabricPacket` records, each with a `PacketType` (`TYPE`) and explicit `write`/read logic.

| Payload | Direction | Purpose |
|---|---|---|
| `ProfileSyncPayload` | S→C | Authoritative profile snapshot for the receiving player |
| `CooldownSyncPayload` | S→C | Active ability cooldowns (remaining/total ticks), capped at 256 entries |
| `RequestProfileSyncPayload` | C→S | "Resend my profile & cooldowns" (no data) |
| `AbilityActivationRequestPayload` | C→S | "I pressed ability slot *n*" (intent only) |

**Why a client cannot change authoritative data**

1. The server registers receivers (in `GrandLineNetworking`) only for the two intent packets. Clientbound
   packets have no server receiver, so a modified client cannot push a profile or cooldowns to the server.
2. Serverbound payloads carry no authoritative values, only intents.
3. Every serverbound handler first passes `ServerRequestGuard.check()` (player must be connected and not
   removed; per-player, per-type rate limit from config), then validates its own content (e.g. slot range,
   alive, not spectator). Rejections are counted and logged at debug level.
4. `FabricPacket` handlers run on the server thread, so there are no data races.

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
| Client + dedicated server | Manual/scripted: `runServer` + `runClient --quickPlayMultiplayer`, commands over RCON | Real client joins, item renders, admin changes sync to client, profile on disk after shutdown, profile restored after full server restart. (The Fabric client gametest API does not exist for 1.20.1.) |

## Switching to Forge later

Fabric-specific code is confined to a few wiring points: `GrandLineMod` / `GrandLineClient` (entrypoints),
`registry/*` (registration calls), `data/ModAttachments` (Fabric attachments), `network/*` and the client
network class (Fabric networking), `player/PlayerProfileEvents` (Fabric events), and the command registration
callbacks. `PlayerProfile`, `ProgressionRules`, `GrandLineConfig`, `AbilityCooldowns` and the validation logic
are loader-neutral. A Forge port replaces the wiring points (DeferredRegister, SimpleChannel, capabilities or
saved player NBT, Forge events). To support both loaders at once, move the neutral code into a shared `common`
module (e.g. an Architectury or MultiLoader-Template layout).
