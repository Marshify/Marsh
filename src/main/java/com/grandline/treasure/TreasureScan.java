package com.grandline.treasure;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.grandline.world.Island;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dev tool behind {@code /grandline treasure scan}: generates an island and writes, for choosing chest spots, its
 * surface (top block of every column), its NPC and gameplay anchors, and every spot a chest could stand on (an open
 * block on a solid floor with room in front), tagged with what is around it: walls, roof, enclosure, nearby crates and
 * doors, sky light. Output: {@code treasure_scan/<island>.json} in the server folder.
 */
public final class TreasureScan {
	private TreasureScan() {
	}

	private static String id(BlockState s) {
		return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
	}

	private static boolean open(BlockState s) {
		return s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty());
	}

	public static Path scan(ServerLevel level, Island island) throws IOException {
		IslandPlan plan = IslandPlans.get(island);
		int r = plan != null ? plan.extent() : island.radius();
		for (int cx = (island.x() - r) >> 4; cx <= (island.x() + r) >> 4; cx++) {
			for (int cz = (island.z() - r) >> 4; cz <= (island.z() + r) >> 4; cz++) {
				level.getChunk(cx, cz); // generate
			}
		}
		JsonObject out = new JsonObject();
		out.addProperty("island", island.name());
		out.addProperty("x", island.x());
		out.addProperty("z", island.z());
		out.addProperty("extent", r);
		JsonArray anchors = new JsonArray();
		if (plan != null) {
			for (Anchor a : plan.anchors()) {
				JsonObject o = new JsonObject();
				o.addProperty("type", a.type().name());
				o.addProperty("x", a.x());
				o.addProperty("z", a.z());
				o.addProperty("y", a.y() == Anchor.SURFACE ? plan.terrain().ground(a.x(), a.z()) + 1 : a.y());
				anchors.add(o);
			}
		}
		out.add("anchors", anchors);
		JsonArray cols = new JsonArray(); // [x, z, topY, block]
		JsonArray cands = new JsonArray();
		JsonArray walk = new JsonArray(); // other standable blocks, for reachability
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				if (x * x + z * z > r * r) {
					continue;
				}
				int wx = island.x() + x, wz = island.z() + z;
				int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz) - 1;
				BlockState topState = level.getBlockState(m.set(wx, top, wz));
				JsonArray c = new JsonArray();
				c.add(x);
				c.add(z);
				c.add(top);
				c.add(id(topState));
				cols.add(c);
				for (int y = 62; y <= Math.min(top + 1, 200); y++) {
					BlockPos p = new BlockPos(wx, y, wz);
					BlockState here = level.getBlockState(p);
					if (!open(here) || !open(level.getBlockState(p.above()))) {
						// An open door (adventure mode cannot open or close one) can be walked through.
						if (here.is(BlockTags.DOORS) && here.hasProperty(net.minecraft.world.level.block.DoorBlock.OPEN)
								&& here.getValue(net.minecraft.world.level.block.DoorBlock.OPEN)
								&& here.getValue(net.minecraft.world.level.block.DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
							JsonArray w = new JsonArray();
							w.add(x);
							w.add(y);
							w.add(z);
							walk.add(w);
						}
						continue;
					}
					BlockState floor = level.getBlockState(p.below());
					if (floor.getCollisionShape(level, p.below()).isEmpty() || !floor.getFluidState().isEmpty() && !floor.isFaceSturdy(level, p.below(), Direction.UP)) {
						continue;
					}
					if (!floor.isFaceSturdy(level, p.below(), Direction.UP) || floor.is(BlockTags.LEAVES)) {
						JsonArray w = new JsonArray(); // somewhere to stand (stairs, slabs, leaves...), not for a chest
						w.add(x);
						w.add(y);
						w.add(z);
						walk.add(w);
						continue;
					}
					StringBuilder walls = new StringBuilder();
					StringBuilder fronts = new StringBuilder();
					for (Direction d : Direction.Plane.HORIZONTAL) {
						BlockPos n = p.relative(d);
						boolean wall = level.getBlockState(n).isCollisionShapeFullBlock(level, n)
								&& level.getBlockState(n.above()).isCollisionShapeFullBlock(level, n.above());
						if (wall) {
							walls.append(d.getName().charAt(0));
						}
						if (open(level.getBlockState(n)) && open(level.getBlockState(n.above()))
								&& level.getBlockState(n.below()).isFaceSturdy(level, n.below(), Direction.UP)) {
							fronts.append(d.getName().charAt(0));
						}
					}
					if (fronts.length() == 0) {
						continue; // nowhere to stand in front of it
					}
					int roof = 0;
					for (int dy = 2; dy <= 12; dy++) {
						if (!open(level.getBlockState(p.above(dy)))) {
							roof = dy;
							break;
						}
					}
					int enclosed = 0;
					for (Direction d : Direction.Plane.HORIZONTAL) {
						for (int k = 1; k <= 8; k++) {
							if (!open(level.getBlockState(p.relative(d, k).above()))) {
								enclosed++;
								break;
							}
						}
					}
					boolean crates = false, door = false, water = false;
					for (BlockPos q : BlockPos.betweenClosed(p.offset(-2, -1, -2), p.offset(2, 2, 2))) {
						BlockState s = level.getBlockState(q);
						if (s.is(Blocks.BARREL) || s.is(Blocks.HAY_BLOCK) || s.is(Blocks.CHEST) || s.is(BlockTags.LOGS) && q.getY() >= p.getY()
								|| s.is(Blocks.SCAFFOLDING) || s.is(Blocks.COMPOSTER)) {
							crates = true;
						}
						if (s.is(BlockTags.DOORS) && Math.abs(q.getX() - p.getX()) + Math.abs(q.getZ() - p.getZ()) <= 2) {
							door = true;
						}
						if (!s.getFluidState().isEmpty()) {
							water = true;
						}
					}
					JsonObject o = new JsonObject();
					o.addProperty("x", x);
					o.addProperty("y", y);
					o.addProperty("z", z);
					o.addProperty("floor", id(floor));
					o.addProperty("walls", walls.toString());
					o.addProperty("fronts", fronts.toString());
					o.addProperty("roof", roof);
					o.addProperty("enclosed", enclosed);
					o.addProperty("sky", level.getBrightness(LightLayer.SKY, p));
					o.addProperty("top", top);
					if (crates) {
						o.addProperty("crates", true);
					}
					if (door) {
						o.addProperty("door", true);
					}
					if (water) {
						o.addProperty("water", true);
					}
					cands.add(o);
				}
			}
		}
		out.add("columns", cols);
		out.add("candidates", cands);
		out.add("walk", walk);
		Path dir = level.getServer().getServerDirectory().toPath().resolve("treasure_scan");
		Files.createDirectories(dir);
		Path file = dir.resolve(island.name().replaceAll("[^A-Za-z0-9]+", "_") + ".json");
		Files.writeString(file, out.toString());
		return file;
	}
}
