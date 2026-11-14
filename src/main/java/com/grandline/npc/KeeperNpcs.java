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
 * NPCs that must always be on their island: the Innkeeper (on every island, sets your home) and a Swordsmith on both
 * starting islands (Millbrook for Pirates, the Marines' island).
 * Unlike the NPCs placed once when an island first loads, these are checked while players are near: if the spot's
 * chunk is loaded and the NPC is not there, it is (re)placed. That puts them on islands of existing worlds too, and
 * an NPC removed by any means comes back. Each NPC's spot is chosen once and saved, so the check always looks in the
 * same place and never doubles an NPC.
 */
public final class KeeperNpcs {
	public static final String SWORD_ISLAND = "Millbrook Village";
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
			for (Island island : IslandRegistry.nearby(server, player.getBlockX(), player.getBlockZ(), NEAR)) {
				if (done.add(island.id())) {
					ensure(level, island);
				}
			}
		}
	}

	/** Places any of the island's keepers that are missing (only where their spot is loaded). Exposed for tests. */
	public static void ensure(ServerLevel level, Island island) {
		for (Islander.Role role : rolesFor(island)) {
			ensure(level, island, role);
		}
	}

	private static void ensure(ServerLevel level, Island island, Islander.Role role) {
		Data data = Data.get(level);
		String key = island.id() + ":" + role.name();
		BlockPos spot = data.spots.get(key);
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
		List<Islander> present = level.getEntitiesOfClass(Islander.class, new AABB(spot).inflate(12),
				npc -> npc.isAlive() && npc.role() == role && npc.islandId() == island.id());
		if (!present.isEmpty()) {
			for (int i = 1; i < present.size(); i++) {
				present.get(i).discard(); // never two of the same keeper
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
		Anchor anchor = plannedAnchor(island, role);
		if (anchor != null && !Float.isNaN(anchor.yaw())) {
			return anchor.yaw();
		}
		double dx = island.x() - at.getX(), dz = island.z() - at.getZ();
		return (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
	}

	private static Anchor plannedAnchor(Island island, Islander.Role role) {
		Anchor.Type type = role == Islander.Role.SWORDSMITH ? Anchor.Type.SWORDSMITH : Anchor.Type.HOME;
		List<Anchor> anchors = IslandPlans.anchors(island, type);
		return anchors.isEmpty() ? null : anchors.get(0);
	}

	/**
	 * The keeper's spot: the plan's anchor if it has one, else a free standing spot a few blocks from the island's main
	 * NPC (quest giver, gunsmith or arrival point), clear of the other NPCs, else near the centre.
	 */
	static BlockPos chooseSpot(ServerLevel level, Island island, Islander.Role role, Data data) {
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
						bases.add(new BlockPos(island.x() + a.x(), 0, island.z() + a.z()));
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
			BlockPos found = nearestSafe(level, base.getX(), base.getZ(), 8, island, taken);
			if (found != null) {
				return found;
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
						if (Math.abs(t.getX() - x) <= 1 && Math.abs(t.getZ() - z) <= 1) {
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
