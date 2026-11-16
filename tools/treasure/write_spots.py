"""Writes TreasureSpots.java from a picks.json (see pick_spots.py), keeping each pick's note."""
import json, sys
picks = json.load(open(sys.argv[1]))
out = sys.argv[2]
lines = []
for island in sorted(picks):
    rows = ',\n'.join(f'\t\t\t\tnew TreasureSpot(TreasureTier.{p["tier"]}, {p["x"]}, {p["y"]}, {p["z"]}, Direction.{p["facing"]}, "{p["why"]}")'
                      for p in picks[island])
    lines.append(f'\t\t\tMap.entry("{island}", List.of(\n{rows}))')
body = ',\n'.join(lines)
src = open(out).read()
start = src.index('\tprivate static final Map<String, List<TreasureSpot>> SPOTS')
end = src.index(';', src.index('Map.', start)) + 1
src = src[:start] + '\tprivate static final Map<String, List<TreasureSpot>> SPOTS = Map.ofEntries(\n' + body + ');' + src[end:]
if 'import net.minecraft.core.Direction;' not in src:
    src = src.replace('import java.util.List;', 'import net.minecraft.core.Direction;\n\nimport java.util.List;')
open(out, 'w').write(src)
