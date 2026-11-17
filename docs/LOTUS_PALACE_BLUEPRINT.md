# Lotus Palace — blueprint (layout 17)

Built to the owner's brief. Three pieces are the owner-approved original design: the **nautilus dome** (in place of
the stacked pavilion), the **conch-spire towers with a scalloped rear wall**, and the **breaking-wave arches**.
Worlds made before layout 17 keep the old Lotus Palace. Code: `world/plan/LotusPalace.java`,
`world/LotusWhirlpools.java`, client `fx/WhirlpoolFx.java`.

## Coordinates
Island-local, from the **surface whirlpool's centre** (the chart anchor): +x east, +z south. Sea surface y 62.

| Piece | Where | Height |
|---|---|---|
| Surface whirlpool | centre (0, 0), 23 across, trigger 5 across | water 62, hidden support at 61 |
| North islet | centre (0, −30), 28 × 23 | shore 62–63, grass 63–65 |
| South-west islet | centre (−26, 18), 30 × 21 | |
| South-east islet | centre (27, 19), 34 × 26 | |
| North landing | x −1..1, z −19..−11 (9 × 3) | 63 |
| Palace origin P | (0, −120); garden level G = y 9 | |
| Air pocket | oval 172 × 156 round the complex | air G..54, invisible ceiling 55, sea 56–62 |
| Palace whirlpool | P + (0, 0, 60): arrival court, 12 across | floor medallion at G |

Palace-local (from P, +z south, y from G):

| Piece | Size | Notes |
|---|---|---|
| Foundation terrace | 67 × 49, clipped corners | +2 |
| Central hall | 57 × 39, clipped corners, interior 51 × 33 × 10 | south door 5 × 8, four window bays each side |
| Broad roof | 65 × 47, 3–4 overhang | red nether brick, purple edging, roof garden + 7 × 7 emblem |
| Nautilus dome *(own design)* | 25 across at its foot, 35 across at the flare | spiral shell, gold seams, lit cyan band, finial; top at +43 |
| Conch towers *(own design)* | (−58,−10), (−25,−43), (25,−43), (58,−10); shaft 9, gallery 13, tip 19 | 39 high, gold spiral band |
| Scalloped rear wall *(own design)* | joins the towers, 19 high | shell-curved bays, burgundy caps, blue diamonds |
| Side wings | 39 × 9, angled segments | roof +14, open passages |
| Breaking-wave arches *(own design)* | six, spans 25–43, rises 13–23 | light blue glass, cyan edges |
| Shell houses | eight, 9–13 across | low layered orange roofs / tall spiral roofs |
| Jellyfish | ten, bells 4–7 | sea lantern cores |

Gameplay: whirlpools link both ways (server-side; entry lock until you leave the destination trigger, plus a
cooldown); no sea damage on the whirlpool support or during the transfer; the palace grounds are dry (walking,
combat, quests). Chests on the islets and in the palace grounds.
