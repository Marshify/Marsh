package com.grandline.npc;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.EnemyKind;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Raid events: Redbrick Port's Quake Monarch (Lv 750). Every {@code raidIntervalMinutes} while players are online (or
 * on an admin's command) he rises in his arena on the fortress grounds, announced to the whole server, and leaves after
 * {@code raidDurationMinutes} if no one defeats him. There is never more than one. Everyone who hurt him shares the
 * reward when he falls, and those who own the Bisento unlock its V2 form. He is not part of the island's population.
 */
public final class RaidEvents {
	public static final String ISLAND = "Redbrick Port";
	public static final String TAG = "grandline.raid";
	public static final String SKIN = "quake_monarch";
	public static final int LEVEL = 750;
	/** The arena: the open plaza between the headquarters and the crescent wall (island-local). */
	public static final int ARENA_X = 0, ARENA_Z = -24;
	private static final Set<UUID> FIGHTERS = new HashSet<>();

	private RaidEvents() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				tick(server);
			}
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity.getTags().contains(TAG) && source.getEntity() instanceof ServerPlayer player) {
				FIGHTERS.add(player.getUUID());
			}
			return true;
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.getTags().contains(TAG) && entity.level() instanceof ServerLevel level) {
				defeated(level.getServer());
			}
		});
	}

	static void tick(MinecraftServer server) {
		ServerLevel level = server.overworld();
		Data data = Data.get(level);
		long now = level.getGameTime();
		if (data.boss != null) {
			Entity boss = level.getEntity(data.boss);
			if (now >= data.ends) {
				if (boss != null) {
					boss.discard();
				}
				announce(server, "message.grandline.raid.left");
				end(data, now);
			} else if (boss == null && level.isPositionEntityTicking(arena(server))) {
				end(data, now); // gone some other way (killed by command): the next raid is scheduled as usual
			}
			return;
		}
		if (data.next == 0) {
			data.next = now + interval();
			data.setDirty();
		}
		if (now >= data.next && !level.players().isEmpty()) {
			start(server);
		}
	}

	private static long interval() {
		return GrandLineMod.config().raidIntervalMinutes * 60L * 20;
	}

	/** Where the Quake Monarch rises, or null if Redbrick Port is not in this world. */
	static BlockPos arena(MinecraftServer server) {
		Island island = IslandRegistry.get(server).byName(ISLAND);
		if (island == null) {
			return null;
		}
		var plan = com.grandline.world.plan.IslandPlans.get(island);
		int y = plan != null ? plan.terrain().ground(ARENA_X, ARENA_Z) + 1 : 66;
		return new BlockPos(island.x() + ARENA_X, y, island.z() + ARENA_Z);
	}

	/** Starts a raid now (admin command, or the timer). Returns false if one is already on or there is no Redbrick Port. */
	public static boolean start(MinecraftServer server) {
		ServerLevel level = server.overworld();
		Data data = Data.get(level);
		BlockPos at = arena(server);
		if (data.boss != null || at == null) {
			return false;
		}
		level.getChunk(at.getX() >> 4, at.getZ() >> 4); // load the arena so he can stand there
		GrandLineEnemy boss = GrandLineEntities.CAPTAIN.create(level);
		if (boss == null) {
			return false;
		}
		Island island = IslandRegistry.get(server).byName(ISLAND);
		boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0);
		boss.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
		boss.equip();
		boss.setSkin(SKIN);
		boss.addTag(TAG);
		boss.setOrigin(at, island.id());
		boss.setGrandLineLevel(LEVEL);
		boss.setPersistenceRequired();
		level.addFreshEntity(boss);
		FIGHTERS.clear();
		data.boss = boss.getUUID();
		data.ends = level.getGameTime() + GrandLineMod.config().raidDurationMinutes * 60L * 20;
		data.setDirty();
		announce(server, "message.grandline.raid.start");
		return true;
	}

	/** Ends the current raid at once (admin command). */
	public static boolean stop(MinecraftServer server) {
		ServerLevel level = server.overworld();
		Data data = Data.get(level);
		if (data.boss == null) {
			return false;
		}
		Entity boss = level.getEntity(data.boss);
		if (boss != null) {
			boss.discard();
		}
		end(data, level.getGameTime());
		return true;
	}

	public static boolean active(MinecraftServer server) {
		return Data.get(server.overworld()).boss != null;
	}

	private static void defeated(MinecraftServer server) {
		ServerLevel level = server.overworld();
		announce(server, "message.grandline.raid.defeated");
		Island island = IslandRegistry.get(server).byName(ISLAND);
		var stage = island == null ? null : com.grandline.world.FirstSeaProgression.stage(island.name(), com.grandline.world.FirstSeaProgression.LOWER);
		long xp = stage != null ? com.grandline.world.FirstSeaProgression.questExperience(stage) * 10 : 100_000;
		long beli = stage != null ? com.grandline.world.FirstSeaProgression.questCurrency(stage) * 10 : 50_000;
		for (UUID id : FIGHTERS) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p == null) {
				continue;
			}
			com.grandline.player.PlayerProfileService.addExperience(p, xp);
			com.grandline.player.PlayerProfileService.addCurrency(p, beli);
			p.sendSystemMessage(Component.translatable("message.grandline.raid.reward", xp, beli).withStyle(ChatFormatting.GOLD));
			if (com.grandline.gear.EquipmentOwnership.owns(p, com.grandline.gear.Gear.BISENTO) && !p.getAttachedOrElse(ModAttachments.BISENTO_V2, false)) {
				p.setAttached(ModAttachments.BISENTO_V2, true);
				p.sendSystemMessage(Component.translatable("message.grandline.raid.bisento").withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}
		end(Data.get(level), level.getGameTime());
	}

	private static void end(Data data, long now) {
		data.boss = null;
		data.next = now + interval();
		data.setDirty();
		FIGHTERS.clear();
	}

	private static void announce(MinecraftServer server, String key) {
		server.getPlayerList().broadcastSystemMessage(Component.translatable(key,
				com.grandline.names.GrandLineNames.npc("entity.grandline." + SKIN, ISLAND),
				com.grandline.names.GrandLineNames.island(ISLAND)).withStyle(ChatFormatting.RED), false);
	}

	/** The raid's saved state: the boss now up (if any), when he leaves, when the next raid comes. */
	static final class Data extends SavedData {
		UUID boss;
		long ends;
		long next;

		static Data get(ServerLevel level) {
			return level.getServer().overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "grandline_raids");
		}

		static Data load(CompoundTag tag) {
			Data d = new Data();
			if (tag.hasUUID("boss")) {
				d.boss = tag.getUUID("boss");
			}
			d.ends = tag.getLong("ends");
			d.next = tag.getLong("next");
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			if (boss != null) {
				tag.putUUID("boss", boss);
			}
			tag.putLong("ends", ends);
			tag.putLong("next", next);
			return tag;
		}
	}
}
