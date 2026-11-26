---
name: verify-in-game
description: Launch the Grand Line dev client and check a change works in game, then read logs and crash reports.
---
1. Build first: `./gradlew build -x test -x runGameTest` (Windows: `tools/dev/build.ps1`). Fix compile errors before launching.
2. Launch: `./gradlew runClient` (Windows: `tools/dev/client.ps1`). Use a test world; never a player's real save.
3. Useful commands: `/grandline admin fruit <player> grandline:<fruit>`, `/grandline admin mastery ...`, `/glvfx` (VFX browser),
   `/glvfx play <id> [scale] [#RRGGBB]`, `/glvfx stats`.
4. After the session: `tools/dev/logs.ps1` (errors only) or `tools/dev/logs.ps1 -Crash`. Read only the stack trace's
   Grand Line frames and the lines around them.
5. Report what was seen, honestly: say which moves or effects were actually checked and which were not.
