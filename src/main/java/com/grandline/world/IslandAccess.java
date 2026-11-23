package com.grandline.world;

import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import com.grandline.world.plan.Terrain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accessibility check for an island as it stands in the world: can a player with no movement abilities step off a boat
 * at the island's dock and walk to its quest giver (and back) without jumping? A player steps up 0.5 block (a slab, a
 * stair) without jumping; a full block needs a jump. Doors, gates and trapdoors count as passable; the route needs two
 * blocks of headroom and may not swim. Also checks that a boat can reach the dock from open sea. Used by the admin
 * command {@code /grandline admin access} and the accessibility tests; it reads blocks only.
 */
public final class IslandAccess {
	static final double STEP = 0.6;
	/** For reports: the direction a jump spot is climbed in (from the lower cell). */
	public static final Map<BlockPos, net.minecraft.core.Direction> CLIMB = new java.util.concurrent.ConcurrentHashMap<>();
	static final int DROP = 4;
	/** A sloop's deck is 0.56 above the water: a landing up to 1.15 above the water is a step off the deck. */
	public static final double LANDING_MAX = 1.15;

	public record Report(String island, boolean boatRoute, double deckAboveWater, boolean walkThere, boolean walkBack, int pathLength,
			List<BlockPos> jumpSpots, String note) {
		public boolean ok() {
			return boatRoute && walkThere && walkBack && deckAboveWater <= LANDING_MAX;
		}
	}

	private IslandAccess() {
	}

	/** Checks one island (its chunks are generated as needed). */
	public static Report check(ServerLevel level, Island island) {
		IslandPlan plan = IslandPlans.get(island);
		if (plan == null) {
			return new Report(island.name(), false, 0, false, false, 0, List.of(), "no plan");
		}
		int reach = Math.max(island.radius(), plan.extent()) + 24;
		for (int cx = (island.x() - reach) >> 4; cx <= (island.x() + reach) >> 4; cx++) {
			for (int cz = (island.z() - reach) >> 4; cz <= (island.z() + reach) >> 4; cz++) {
				level.getChunk(cx, cz);
			}
		}
		BlockPos quest = null, home = null;
		List<BlockPos> starts = new ArrayList<>();
		for (Anchor a : plan.anchors()) {
			BlockPos p = world(level, island, a);
			if (a.type() == Anchor.Type.QUEST && a.region() == com.grandline.world.FirstSeaProgression.LOWER && quest == null) {
				quest = p;
			}
			if (a.type() == Anchor.Type.HOME && home == null) {
				home = p;
			}
			if (a.type() == Anchor.Type.DOCK) {
				starts.add(p);
			}
		}
		if (starts.isEmpty()) {
			BlockPos arrival = IslandPlans.arrival(island.name(), island.x(), island.z());
			if (arrival != null) {
				starts.add(arrival);
			}
		}
		String goal = "quest";
		BlockPos arrivalSpot = IslandPlans.arrival(island.name(), island.x(), island.z());
		if (quest != null && quest.getY() < Terrain.WATER_TOP - 10 && arrivalSpot != null) {
			quest = arrivalSpot; // an underwater quest giver is reached through the island's whirlpool: walk to that
			goal = "whirlpool islet";
		} else if (quest == null) {
			quest = home != null ? home : arrivalSpot; // the hub has no quest giver: its Innkeeper
			goal = home != null ? "innkeeper" : "arrival";
		}
		if (quest == null || starts.isEmpty()) {
			return new Report(island.name(), false, 0, false, false, 0, List.of(), "no quest giver or dock in the plan");
		}
		// Landings: dry places to stand beside water a boat floats on, near the dock, low enough to step onto from a
		// ship's deck. The walk may start from any of them; the lowest is reported.
		BlockPos deck = null, water = null;
		double deckAbove = 99;
		List<BlockPos> landings = new ArrayList<>();
		for (BlockPos start : starts) {
			for (BlockPos p : BlockPos.betweenClosed(start.offset(-16, -2, -16), start.offset(16, 4, 16))) {
				double h = standHeight(level, p);
				if (Double.isNaN(h) || !level.getFluidState(p).isEmpty()) {
					continue; // a landing is dry footing (wading ashore is not a landing)
				}
				for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
					for (int k = 1; k <= 2; k++) {
						BlockPos w = new BlockPos(p.getX() + d.getStepX() * k, Terrain.WATER_TOP, p.getZ() + d.getStepZ() * k);
						if (boatWater(level, w) && h - (Terrain.WATER_TOP + 1) <= LANDING_MAX && !landings.contains(p)) {
							landings.add(p.immutable());
						}
						if (boatWater(level, w) && h - (Terrain.WATER_TOP + 1) < deckAbove) {
							deckAbove = h - (Terrain.WATER_TOP + 1);
							deck = p.immutable();
							water = w;
						}
					}
				}
			}
		}
		boolean boat = water != null && boatReachesSea(level, water, island, reach);
		BlockPos target = nearestStand(level, quest, 6);
		if (deck == null || target == null) {
			return new Report(island.name(), boat, deckAbove, false, false, 0, List.of(), deck == null ? "no standing room at the dock" : "no standing room at the quest giver");
		}
		// Walk from the landing that does best (fewest jumps), and back to the nearest landing.
		Walk there = null;
		BlockPos used = deck;
		for (BlockPos l : landings.isEmpty() ? List.of(deck) : landings) {
			Walk w = walk(level, l, target, island, reach);
			if (there == null || (w.reached && !there.reached) || (!there.reached && w.jumps.size() < there.jumps.size() && !w.jumps.isEmpty())
					|| (there.length == 0 && w.length > 0)) {
				there = w;
				used = l;
			}
			if (there.reached) {
				break;
			}
		}
		deck = used;
		Walk back = walk(level, target, deck, island, reach);
		List<BlockPos> jumps = new ArrayList<>(there.jumps);
		for (BlockPos b : back.jumps) {
			if (!jumps.contains(b)) {
				jumps.add(b);
			}
		}
		return new Report(island.name(), boat, deckAbove, there.reached, back.reached, there.length, jumps,
				"dock " + deck.toShortString() + " -> " + goal + " " + target.toShortString());
	}

	private static BlockPos world(ServerLevel level, Island island, Anchor a) {
		int x = island.x() + a.x(), z = island.z() + a.z();
		int y = a.y() != Anchor.SURFACE ? a.y() : level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		return new BlockPos(x, y, z);
	}

	// ---- standing ----

	/** Collision height of a block (0 = none; doors, gates and trapdoors count as passable). */
	static double top(ServerLevel level, BlockPos pos) {
		BlockState s = level.getBlockState(pos);
		if (s.getBlock() instanceof DoorBlock || s.getBlock() instanceof FenceGateBlock || s.getBlock() instanceof TrapDoorBlock
				|| s.is(BlockTags.CLIMBABLE)) {
			return 0;
		}
		var shape = s.getCollisionShape(level, pos);
		return shape.isEmpty() ? 0 : shape.max(net.minecraft.core.Direction.Axis.Y);
	}

	/** A bottom stair or bottom slab: the part a player steps onto halfway up. */
	static boolean halfStep(BlockState s) {
		return (s.getBlock() instanceof StairBlock && s.getValue(StairBlock.HALF) == Half.BOTTOM)
				|| (s.getBlock() instanceof SlabBlock && s.getValue(SlabBlock.TYPE) == SlabType.BOTTOM);
	}

	/**
	 * The height a player stands at with feet in block {@code pos}, or NaN if they cannot stand there: the feet block is
	 * empty (or a half step) with a full floor under it, two blocks of headroom, and not swimming.
	 */
	static double standHeight(ServerLevel level, BlockPos pos) {
		BlockState feet = level.getBlockState(pos);
		double h;
		if (feet.is(BlockTags.CLIMBABLE)) {
			return pos.getY(); // on a ladder: held there, climbing up or down without jumping
		}
		double own = top(level, pos);
		if (halfStep(feet)) {
			h = pos.getY() + 0.5;
		} else if (own > 0 && own <= 0.6) {
			h = pos.getY() + own; // carpet, snow layer, low block
		} else if (own == 0) {
			double below = top(level, pos.below());
			if (below < 0.8 || below > 1.01) {
				return Double.NaN; // nothing under the feet (or a fence/wall top)
			}
			h = pos.getY() - 1 + below; // full blocks, dirt paths, farmland, mud
		} else {
			return Double.NaN;
		}
		if (top(level, pos.above()) > 0 || (h - pos.getY() > 0.2 && top(level, pos.above(2)) > 0)) {
			return Double.NaN;
		}
		if (level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos.above()).is(FluidTags.WATER)) {
			return Double.NaN; // swimming
		}
		return h;
	}

	static BlockPos nearestStand(ServerLevel level, BlockPos around, int r) {
		BlockPos best = null;
		double bestD = Double.MAX_VALUE;
		for (BlockPos p : BlockPos.betweenClosed(around.offset(-r, -3, -r), around.offset(r, 3, r))) {
			if (!Double.isNaN(standHeight(level, p))) {
				double d = p.distSqr(around);
				if (d < bestD) {
					bestD = d;
					best = p.immutable();
				}
			}
		}
		return best;
	}

	// ---- walking ----

	record Walk(boolean reached, int length, List<BlockPos> jumps) {
	}

	/**
	 * Walk without jumping. If the goal cannot be reached that way, the walk that needs the fewest jumps (up to 1.25
	 * blocks each) is found instead and its jump spots reported, so they can be fixed together.
	 */
	static Walk walk(ServerLevel level, BlockPos from, BlockPos to, Island island, int reach) {
		Map<BlockPos, Integer> cost = new HashMap<>();
		Map<BlockPos, BlockPos> prev = new HashMap<>();
		Map<BlockPos, Boolean> jumped = new HashMap<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		cost.put(from, 0);
		queue.add(from);
		BlockPos goal = null;
		java.util.PriorityQueue<long[]> pq = new java.util.PriorityQueue<>(java.util.Comparator.comparingLong(a -> a[0]));
		pq.add(new long[]{0, from.asLong()});
		while (!pq.isEmpty()) {
			long[] e = pq.poll();
			BlockPos p = BlockPos.of(e[1]);
			if (e[0] > cost.getOrDefault(p, Integer.MAX_VALUE)) {
				continue;
			}
			if (Math.abs(p.getX() - to.getX()) <= 2 && Math.abs(p.getZ() - to.getZ()) <= 2 && Math.abs(p.getY() - to.getY()) <= 2) {
				goal = p;
				break;
			}
			double h = standHeight(level, p);
			if (level.getBlockState(p).is(BlockTags.CLIMBABLE)) {
				h += 1; // at a ladder's top a player climbs until the feet are level with the ledge, then steps off
			}
			List<BlockPos> next = new ArrayList<>();
			List<Boolean> isJump = new ArrayList<>();
			if (halfStep(level.getBlockState(p))) {
				next.add(p.above());
				isJump.add(false);
			}
			if (level.getBlockState(p).is(BlockTags.CLIMBABLE) || level.getBlockState(p.above()).is(BlockTags.CLIMBABLE)) {
				next.add(p.above()); // climb
				isJump.add(false);
			}
			if (level.getBlockState(p.below()).is(BlockTags.CLIMBABLE)) {
				next.add(p.below());
				isJump.add(false);
			}
			for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
				BlockPos n = p.relative(d);
				if (Math.abs(n.getX() - island.x()) > reach || Math.abs(n.getZ() - island.z()) > reach) {
					continue;
				}
				for (int dy = 2; dy >= -DROP; dy--) {
					BlockPos m = n.above(dy);
					double h2 = standHeight(level, m);
					if (Double.isNaN(h2) || h - h2 > DROP || h2 - h > 1.25) {
						continue;
					}
					next.add(m);
					isJump.add(h2 - h > STEP);
				}
			}
			for (int k = 0; k < next.size(); k++) {
				BlockPos m = next.get(k).immutable();
				int c = (int) e[0] + (isJump.get(k) ? 1000 : 1);
				if (c < cost.getOrDefault(m, Integer.MAX_VALUE)) {
					cost.put(m, c);
					prev.put(m, p);
					jumped.put(m, isJump.get(k));
					pq.add(new long[]{c, m.asLong()});
				}
			}
		}
		if (goal == null) {
			return new Walk(false, 0, List.of());
		}
		List<BlockPos> jumps = new ArrayList<>();
		int length = 0;
		for (BlockPos p = goal; prev.containsKey(p); p = prev.get(p)) {
			length++;
			if (Boolean.TRUE.equals(jumped.get(p))) {
				jumps.add(0, p);
				BlockPos below = prev.get(p);
				CLIMB.put(p, net.minecraft.core.Direction.fromDelta(Integer.signum(p.getX() - below.getX()), 0, Integer.signum(p.getZ() - below.getZ())));
			}
		}
		return new Walk(jumps.isEmpty(), length, jumps);
	}

	// ---- boats ----

	/** Water a boat floats on (surface water with water under it and air above) next to the deck. */
	static BlockPos nearestBoatWater(ServerLevel level, BlockPos deck, int r) {
		BlockPos best = null;
		double bestD = Double.MAX_VALUE;
		for (BlockPos p : BlockPos.betweenClosed(deck.offset(-r, -3, -r), deck.offset(r, 1, r))) {
			if (boatWater(level, p)) {
				double d = p.distSqr(deck);
				if (d < bestD) {
					bestD = d;
					best = p.immutable();
				}
			}
		}
		return best;
	}

	static boolean boatWater(ServerLevel level, BlockPos p) {
		return p.getY() == Terrain.WATER_TOP && level.getFluidState(p).is(FluidTags.WATER) && level.getFluidState(p.below()).is(FluidTags.WATER)
				&& top(level, p.above()) == 0 && top(level, p.above(2)) == 0 && !level.getBlockState(p).is(BlockTags.SLABS);
	}

	/** Whether surface water connects the dock with open sea beyond the island. */
	static boolean boatReachesSea(ServerLevel level, BlockPos start, Island island, int reach) {
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		java.util.Set<BlockPos> seen = new java.util.HashSet<>();
		queue.add(start);
		seen.add(start);
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			if (Math.abs(p.getX() - island.x()) >= reach || Math.abs(p.getZ() - island.z()) >= reach) {
				return true;
			}
			for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
				BlockPos n = p.relative(d);
				if (!seen.contains(n) && boatWater(level, n)) {
					seen.add(n);
					queue.add(n);
				}
			}
		}
		return false;
	}
}
