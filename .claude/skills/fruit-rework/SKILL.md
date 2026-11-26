---
name: fruit-rework
description: Rework or add a Devil Fruit moveset in Grand Line (the numbered fruit rework series). Use when the user sends a fruit spec.
---
1. Read the spec, then only: `docs/FRUIT_ROSTER.md`, the fruit's `docs/<FRUIT>.md` (if any), `fruit/<name>/<Name>Moves.java`,
   `client/fx/<Name>Fx.java`. Grep for the ability ids; do not read other fruits unless reusing a helper.
2. Reuse shared systems (list in CLAUDE.md) before writing new ones; put a genuinely reusable mechanic in a shared class.
3. Keep ability ids, item ids and saved data unchanged (save compatibility). Display names go through `names.json` only.
4. Server: damage, cooldowns, energy, mastery gates. Client: FX via `<Name>Fx` / VFX library, within particle budgets,
   with shader-pack fallback. Sounds via `tools/sounds` (key from env var only).
5. Add game tests in `src/gametest` for every move's core effect; run `./gradlew runGameTest`.
6. Verify each move in the dev client (`grandline admin fruit <player> grandline:<fruit>`,
   `grandline admin mastery <player> grandline:<fruit> <n>`); check `run/logs/latest.log` for errors.
7. Update `docs/<FRUIT>.md`, `CHANGELOG.md`, bump `mod_version`, build, copy jar to `dist/`, commit; ask before pushing.
8. Report concisely: moves done, tests run (real counts), what was verified in game, jar link.
