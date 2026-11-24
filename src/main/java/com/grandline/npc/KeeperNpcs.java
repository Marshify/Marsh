package com.grandline.npc;

import com.grandline.GrandLineMod;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.Islander;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.IslandSpawns;
import com.grandline.world.ModWorld;
import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NPCs that must always be on their island: the Innkeeper (on every island, sets your home) and a Swordsmith and
 * Movement Instructor on both starting islands (Millbrook for Pirates, the Marines' island: Ironhold Bastion).
 * Unlike the NPCs placed once when an island first loads, these are checked while players are near: if the spot's
 * chunk is loaded and the NPC is not there, it is (re)placed. That puts them on islands of existing worlds too, and
 * an NPC removed by any means comes back. Each NPC's spot is chosen once and saved, so the check always looks in the
 * same place and never doubles an NPC.
 */
public final class KeeperNpcs {
	public static final String SWORD_ISLAND = "Millbrook Village";
	/** The cold island whose village hosts the Flash Step and Aura masters. */
	public static final String FROST_ISLAND = "Frostpeak Isle";
	/** The island whose upper region hosts the Instinct Sage. */
	public static final String SKY_ISLAND = "Cloudtop Isles";
	/** Keepers only the starting islands have: they follow the Marines' island when it changes (see {@link #retire}). */
	static final List<Islander.Role> STARTING_ISLAND_ROLES = List.of(Islander.Role.SWORDSMITH, Islander.Role.MOVEMENT_INSTRUCTOR);
	static final int CHECK_TICKS = 40;
	static final int NEAR = 160;

	private KeeperNpcs() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_TICKS == 0) {
				tick(server);
			}
		});
	}

	/** The keeper roles an island must have. */
	public static List<Islander.Role> rolesFor(Island island) {
		List<Islander.Role> roles = new ArrayList<>();
		roles.add(Islander.Role.INNKEEPER);
		if (SWORD_ISLAND.equals(island.name()) || com.grandline.GrandLineMod.config().marineSpawnIsland.equals(island.name())) {
			roles.add(Islander.Role.SWORDSMITH);
			roles.add(Islander.Role.MOVEMENT_INSTRUCTOR);
		}
		if (FROST_ISLAND.equals(island.name())) {
			roles.add(Islander.Role.FLASH_STEP_MASTER); // the Ability Master: Flash Step and Aura
		}
		if (SKY_ISLAND.equals(island.name())) {
			roles.add(Islander.Role.INSTINCT_SAGE);
		}
		switch (island.name()) {
			case com.grandline.race.RaceProgress.MYSTIC_ISLAND -> roles.add(Islander.Role.RACE_MYSTIC);
			case com.grandline.race.RaceProgress.CYBORG_ISLAND -> roles.add(Islander.Role.TINKERER);
			case com.grandline.race.RaceProgress.GHOUL_ISLAND -> roles.add(Islander.Role.GRAVEKEEPER);
			case com.grandline.race.RaceProgress.TRIAL_ISLAND -> roles.add(Islander.Role.TRIAL_KEEPER);
			default -> { }
		}
		return roles;
	}

	private static void tick(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (level.players().isEmpty()) {
			return;
		}
		java.util.Set<Long> done = new java.util.HashSet<>();
		for (ServerPlayer player : level.players()) {
			for (Island island : IslandRegistry.reaching(server, player.getBlockX(), player.getBlockZ(), 64)) {
				if (done.add(island.id())) {
					ensure(level, island);
				}
			}
		}
	}

	/** Places any of the island's keepers that are missing (only where their spot is loaded). Exposed for tests. */
	public static void ensure(ServerLevel level, Island island) {
		if (RosterNpcs.manages(island)) {
			if (com.grandline.world.plan.IslandRoster.lowerOnly(island)) { // the upper region keeps its own keepers
				ensure(level, island, Islander.Role.INSTINCT_SAGE);
				for (Anchor a : IslandPlans.anchors(island, Anchor.Type.QUEST)) {
					if (a.region() != com.grandline.world.FirstSeaProgression.LOWER) {
						ensureRegionQuestGiver(level, island, a);
					}
				}
			}
			return; // the rest follows the island's fixed roster (RosterNpcs)
		}
		List<Islander.Role> roles = rolesFor(island);
		for (Islander.Role role : roles) {
			ensure(level, island, role);
		}
		for (Islander.Role role : STARTING_ISLAND_ROLES) {
			if (!roles.contains(role)) {
				retire(level, island, role);
			}
		}
		for (Anchor a : IslandPlans.anchors(island, Anchor.Type.QUEST)) {
			if (a.region() != com.grandline.world.FirstSeaProgression.LOWER) {
				ensureRegionQuestGiver(level, island, a);
			}
		}
	}

	/**
	 * A keeper this island placed but no longer has (it stopped being the Marines' starting island): the NPC is removed
	 * once its spot is loaded, and its saved spot forgotten, so it is not left behind next to the new starting island's.
	 */
	private static void retire(ServerLevel level, Island island, Islander.Role role) {
		Data data = Data.get(level);
		String key = island.id() + ":" + role.name();
		BlockPos spot = data.spots.get(key);
		if (spot == null || !level.isPositionEntityTicking(spot)) {
			return;
		}
		for (Islander npc : level.getEntitiesOfClass(Islander.class, new AABB(spot).inflate(48),
				n -> n.isAlive() && n.role() == role && n.islandId() == island.id())) {
			npc.discard();
		}
		data.spots.remove(key);
		data.setDirty();
		GrandLineMod.LOGGER.info("Removed the {} from {} (no longer a starting island)", role, island.name());
	}

	/**
	 * A region's own quest giver (the upper Cloudtop Isles): kept at its spot like the other keepers, so worlds whose
	 * island was populated before the region existed get one too. It hands out that region's quests.
	 */
	private static void ensureRegionQuestGiver(ServerLevel level, Island island, Anchor a) {
		int x = island.x() + a.x(), z = island.z() + a.z();
		BlockPos probe = new BlockPos(x, 64, z);
		if (!level.isPositionEntityTicking(probe)) {
			return;
		}
		IslandPlan plan = IslandPlans.get(island);
		int y = a.y() != Anchor.SURFACE ? a.y() : plan.terrain().ground(a.x(), a.z()) + 1;
		BlockPos spot = new BlockPos(x, y, z);
		List<Islander> present = level.getEntitiesOfClass(Islander.class, new AABB(spot).inflate(12),
				npc -> npc.isAlive() && npc.role() == Islander.Role.QUEST_GIVER && npc.islandId() == island.id() && npc.region() == a.region());
		if (!present.isEmpty()) {
			for (int i = 1; i < present.size(); i++) {
				present.get(i).discard();
			}
			return;
		}
		Islander npc = GrandLineEntities.ISLANDER.create(level);
		if (npc == null) {
			return;
		}
		float yaw = Float.isNaN(a.yaw()) ? 0 : a.yaw();
		npc.moveTo(x + 0.5, y, z + 0.5, yaw, 0);
		npc.setYHeadRot(yaw);
		npc.setYBodyRot(yaw);
		com.grandline.world.FirstSeaProgression.Stage stage = com.grandline.world.FirstSeaProgression.stage(island.name(), a.region());
		npc.setup(Islander.Role.QUEST_GIVER, island.id(), stage != null ? stage.min() : island.level());
		npc.setRegion(a.region());
		npc.setPersistenceRequired();
		level.addFreshEntity(npc);
		GrandLineMod.LOGGER.info("Placed the region {} quest giver on {} at {}", a.region(), island.name(), spot);
	}

	/**
	 * A keeper with a hand-picked spot that overrides the saved one (also in existing worlds): the Ability Master by
	 * the campfire in Frostpeak's ice cavern. Only where the island has that build (older worlds keep the old Frostpeak).
	 */
	static BlockPos fixedSpot(Island island, Islander.Role role) {
		if (role == Islander.Role.FLASH_STEP_MASTER && FROST_ISLAND.equals(island.name())
				&& IslandPlans.get(island) instanceof com.grandline.world.plan.FrostpeakIsle) {
			int[] o = com.grandline.world.plan.FrostpeakIsle.ABILITY_MASTER_SPOT;
			return new BlockPos(island.x() + o[0], o[1], island.z() + o[2]);
		}
		return null;
	}

	private static void ensure(ServerLevel level, Island island, Islander.Role role) {
		Data data = Data.get(level);
		String key = island.id() + ":" + role.name();
		BlockPos spot = data.spots.get(key);
		BlockPos fixed = fixedSpot(island, role);
		if (fixed != null && !fixed.equals(spot)) {
			spot = fixed; // moved: the keeper already standing at the old spot walks over below
			data.spots.put(key, spot);
			data.setDirty();
		}
		if (spot == null) {
			if (!level.isPositionEntityTicking(new BlockPos(island.x(), 64, island.z()))) {
				return; // choose the spot once the island is loaded
			}
			spot = chooseSpot(level, island, role, data);
			if (spot == null) {
				return;
			}
			data.spots.put(key, spot);
			data.setDirty();
		}
		if (!level.isPositionEntityTicking(spot)) {
			return;
		}
		// Searched well beyond the spot (any height), so a keeper whose spot has moved is brought over, not doubled.
		BlockPos at = spot;
		List<Islander> present = new ArrayList<>(level.getEntitiesOfClass(Islander.class, new AABB(spot).inflate(48),
				npc -> npc.isAlive() && npc.role() == role && npc.islandId() == island.id()));
		if (!present.isEmpty()) {
			present.sort(java.util.Comparator.comparingDouble(npc -> npc.distanceToSqr(at.getX() + 0.5, at.getY(), at.getZ() + 0.5)));
			for (int i = 1; i < present.size(); i++) {
				present.get(i).discard(); // never two of the same keeper
			}
			Islander keeper = present.get(0);
			if (keeper.distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5) > 64) {
				float yaw = yawFor(island, role, spot);
				keeper.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0);
				keeper.setYHeadRot(yaw);
				keeper.setYBodyRot(yaw);
				GrandLineMod.LOGGER.info("Moved {} on {} to {}", role, island.name(), spot);
			}
			return;
		}
		spawn(level, island, role, spot);
	}

	private static void spawn(ServerLevel level, Island island, Islander.Role role, BlockPos spot) {
		Islander npc = GrandLineEntities.ISLANDER.create(level);
		if (npc == null) {
			return;
		}
		BlockPos feet = IslandSpawns.isSafe(level, spot) ? spot : nearestSafe(level, spot.getX(), spot.getZ(), 4, island, List.of());
		if (feet == null) {
			feet = spot;
		}
		float yaw = yawFor(island, role, feet);
		npc.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, 0);
		npc.setYHeadRot(yaw);
		npc.setYBodyRot(yaw);
		npc.setup(role, island.id(), island.level());
		npc.setPersistenceRequired();
		level.addFreshEntity(npc);
		GrandLineMod.LOGGER.info("Placed {} on {} at {}", role, island.name(), feet);
	}

	/** Faces the planned yaw, else toward the island centre. */
	private static float yawFor(Island island, Islander.Role role, BlockPos at) {
		if (fixedSpot(island, role) != null) {
			return com.grandline.world.plan.FrostpeakIsle.ABILITY_MASTER_YAW;
		}
		Anchor anchor = plannedAnchor(island, role);
		if (anchor != null && !Float.isNaN(anchor.yaw())) {
			return anchor.yaw();
		}
		double dx = island.x() - at.getX(), dz = island.z() - at.getZ();
		return (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
	}

	private static Anchor plannedAnchor(Island island, Islander.Role role) {
		if (role != Islander.Role.INNKEEPER && role != Islander.Role.SWORDSMITH) {
			return null; // race NPCs and trainers stand beside the island's main NPCs
		}
		Anchor.Type type = role == Islander.Role.SWORDSMITH ? Anchor.Type.SWORDSMITH : Anchor.Type.HOME;
		List<Anchor> anchors = IslandPlans.anchors(island, type);
		return anchors.isEmpty() ? null : anchors.get(0);
	}

	/**
	 * The keeper's spot: the plan's anchor if it has one, else a free standing spot a few blocks from the island's main
	 * NPC (quest giver, gunsmith or arrival point), clear of the other NPCs, else near the centre.
	 */
	static BlockPos chooseSpot(ServerLevel level, Island island, Islander.Role role, Data data) {
		if (role == Islander.Role.INSTINCT_SAGE) {
			return upperSpot(level, island);
		}
		Anchor planned = plannedAnchor(island, role);
		if (planned != null) {
			int x = island.x() + planned.x(), z = island.z() + planned.z();
			level.getChunk(x >> 4, z >> 4);
			int y = planned.y() != Anchor.SURFACE ? planned.y() : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			return new BlockPos(x, y, z);
		}
		List<BlockPos> taken = new ArrayList<>();
		List<BlockPos> bases = new ArrayList<>();
		IslandPlan plan = IslandPlans.get(island);
		if (plan != null) {
			for (Anchor a : plan.anchors()) {
				BlockPos p = new BlockPos(island.x() + a.x(), 0, island.z() + a.z());
				switch (a.type()) {
					case QUEST, FRUIT_DEALER, FISHERMAN, NAVIGATOR, GACHA, GUNSMITH, ARRIVAL -> taken.add(p);
					default -> { }
				}
			}
			for (Anchor.Type t : new Anchor.Type[]{Anchor.Type.QUEST, Anchor.Type.GUNSMITH, Anchor.Type.ARRIVAL, Anchor.Type.FRUIT_DEALER}) {
				for (Anchor a : plan.anchors()) {
					if (a.type() == t) {
						// A built terrace (Frostpeak's village) gives its own height; natural ground is found per column.
						bases.add(new BlockPos(island.x() + a.x(), a.y() != Anchor.SURFACE ? a.y() : 0, island.z() + a.z()));
					}
				}
			}
		} else {
			taken.add(new BlockPos(island.x(), 0, island.z())); // the quest giver stands at the centre
			taken.add(new BlockPos(island.x() + 3, 0, island.z() + 1));
			taken.add(new BlockPos(island.x() - 3, 0, island.z() - 1));
			taken.add(new BlockPos(island.x() + 1, 0, island.z() - 4));
		}
		for (Map.Entry<String, BlockPos> other : data.spots.entrySet()) {
			if (other.getKey().startsWith(island.id() + ":")) {
				taken.add(other.getValue());
			}
		}
		bases.add(new BlockPos(island.x(), 0, island.z()));
		for (BlockPos base : bases) {
			BlockPos found = base.getY() != 0 ? nearestSafeAt(level, base, 8, taken) : nearestSafe(level, base.getX(), base.getZ(), 8, island, taken);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/**
	 * The Instinct Sage's spot: a few steps from the upper region's quest giver, on that region's own ground (high above
	 * the lower island), so it is found in existing worlds without touching any building.
	 */
	private static BlockPos upperSpot(ServerLevel level, Island island) {
		IslandPlan plan = IslandPlans.get(island);
		if (plan == null) {
			return null;
		}
		for (Anchor a : IslandPlans.anchors(island, Anchor.Type.QUEST)) {
			if (a.region() == com.grandline.world.FirstSeaProgression.LOWER) {
				continue;
			}
			int bx = island.x() + a.x(), bz = island.z() + a.z();
			int by = a.y() != Anchor.SURFACE ? a.y() : plan.terrain().ground(a.x(), a.z()) + 1;
			level.getChunk(bx >> 4, bz >> 4);
			for (int r = 3; r <= 7; r++) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
							continue;
						}
						for (int dy = 0; dy <= 2; dy++) {
							for (int sign : new int[]{1, -1}) {
								BlockPos p = new BlockPos(bx + dx, by + dy * sign, bz + dz);
								level.getChunk(p.getX() >> 4, p.getZ() >> 4);
								if (IslandSpawns.isSafe(level, p)) {
									return p;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	/** Nearest safe standing spot 2-{@code radius} blocks (square rings) from (cx, cz) on the island's land. */
	private static BlockPos nearestSafe(ServerLevel level, int cx, int cz, int radius, Island island, List<BlockPos> taken) {
		for (int r = 2; r <= radius; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					int x = cx + dx, z = cz + dz;
					if (!ModWorld.inGameplayArea(x, z, island)) {
						continue;
					}
					boolean clear = true;
					for (BlockPos t : taken) {
						if (Math.abs(t.getX() - x) <= 2 && Math.abs(t.getZ() - z) <= 2) { // keep NPCs a few steps apart
							clear = false;
							break;
						}
					}
					if (!clear) {
						continue;
					}
					level.getChunk(x >> 4, z >> 4);
					BlockPos p = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
					if (IslandSpawns.isSafe(level, p) && onGround(island, p)) {
						return p;
					}
				}
			}
		}
		return null;
	}

	/** On the island's ground (or a step above it), not on a roof, a wall or a canopy. */
	/** Like {@link #nearestSafe}, but on a built level: the anchor's own height, a step up or down. */
	private static BlockPos nearestSafeAt(ServerLevel level, BlockPos base, int radius, List<BlockPos> taken) {
		for (int r = 2; r <= radius; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					int x = base.getX() + dx, z = base.getZ() + dz;
					boolean clear = true;
					for (BlockPos t : taken) {
						if (Math.abs(t.getX() - x) <= 2 && Math.abs(t.getZ() - z) <= 2) {
							clear = false;
							break;
						}
					}
					if (!clear) {
						continue;
					}
					level.getChunk(x >> 4, z >> 4);
					for (int dy : new int[]{0, 1, -1}) {
						BlockPos p = new BlockPos(x, base.getY() + dy, z);
						if (IslandSpawns.isSafe(level, p) && level.canSeeSky(p)) {
							return p;
						}
					}
				}
			}
		}
		return null;
	}

	private static boolean onGround(Island island, BlockPos feet) {
		IslandPlan plan = IslandPlans.get(island);
		if (plan == null) {
			return true;
		}
		int ground = plan.terrain().ground(feet.getX() - island.x(), feet.getZ() - island.z());
		return feet.getY() <= ground + 2;
	}

	/** Forgets an island's saved keeper spots (tests: the test world keeps saved data between runs). */
	public static void forget(ServerLevel level, long islandId) {
		Data data = Data.get(level);
		data.spots.keySet().removeIf(k -> k.startsWith(islandId + ":"));
		data.setDirty();
	}

	/** Where a keeper stands (null until chosen). Exposed for tests. */
	public static BlockPos spot(ServerLevel level, Island island, Islander.Role role) {
		return Data.get(level).spots.get(island.id() + ":" + role.name());
	}

	/** Saved keeper spots, per island and role. */
	static final class Data extends SavedData {
		final Map<String, BlockPos> spots = new HashMap<>();

		static Data get(ServerLevel level) {
			return level.getServer().overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "grandline_keepers");
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			ListTag list = tag.getList("spots", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag e = list.getCompound(i);
				data.spots.put(e.getString("key"), BlockPos.of(e.getLong("pos")));
			}
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			ListTag list = new ListTag();
			spots.forEach((k, v) -> {
				CompoundTag e = new CompoundTag();
				e.putString("key", k);
				e.putLong("pos", v.asLong());
				list.add(e);
			});
			tag.put("spots", list);
			return tag;
		}
	}
}
