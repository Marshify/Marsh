package com.grandline.race;

import com.grandline.GrandLineMod;
import com.grandline.entity.EnemyKind;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Awakening trials, one per race, all solo. Each player gets a private arena high in the sky far from every island
 * (its own slot, so two players never share or disturb one), built when the trial starts and removed when it ends.
 * <ul>
 * <li>Human, Trial of Strength: three waves, then a captain.</li>
 * <li>Shark, Trial of Water: three waves fought in a deep pool.</li>
 * <li>Angel, Trial of the Sky: climb the floating platforms to the summit (a fall puts you back at the start).</li>
 * <li>Rabbit, Trial of Speed: touch eight markers in order before time runs out.</li>
 * <li>Cyborg, Trial of Technology: destroy four sentry cores while their guards attack.</li>
 * <li>Ghoul, Trial of Darkness: three waves in darkness.</li>
 * </ul>
 * Dying, leaving, logging out or running out of time fails the trial (no cost); the player is returned where they
 * started. A clear marks the trial done and gives Awakening Points (first clear more, capped).
 */
public final class RaceTrials {
	public static final String TAG = "grandline_trial";
	static final int ARENA_Y = 200;
	static final int ARENA_Z = 36_000;
	static final int ARENA_X0 = 36_000;
	static final int SLOT_SPACING = 320;

	static final class Session {
		final UUID player;
		final Race race;
		final int slot;
		final BlockPos center;
		final Vec3 returnPos;
		final float returnYaw;
		final long endTime;
		final long startTime;
		final List<BlockPos> built = new ArrayList<>();
		final List<UUID> enemies = new ArrayList<>();
		final List<BlockPos> markers = new ArrayList<>();
		final ServerBossEvent bar;
		int phase;
		int wave;
		boolean done;

		Session(ServerPlayer p, Race race, int slot, BlockPos center, long startTime, long endTime) {
			this.startTime = startTime;
			this.player = p.getUUID();
			this.race = race;
			this.slot = slot;
			this.center = center;
			this.returnPos = p.position();
			this.returnYaw = p.getYRot();
			this.endTime = endTime;
			this.bar = new ServerBossEvent(Component.translatable("race.grandline.trial." + race.id()), BossEvent.BossBarColor.PURPLE,
					BossEvent.BossBarOverlay.PROGRESS);
		}
	}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	private RaceTrials() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(RaceTrials::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player && SESSIONS.containsKey(player.getUUID())) {
				end(player.server, SESSIONS.get(player.getUUID()), false, "race.grandline.trial.failed_death");
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			Session s = SESSIONS.get(handler.getPlayer().getUUID());
			if (s != null) {
				// Returned now, so the saved position is outside the arena.
				handler.getPlayer().teleportTo(server.overworld(), s.returnPos.x, s.returnPos.y, s.returnPos.z, s.returnYaw, 0);
				end(server, s, false, null);
			}
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Session s : List.copyOf(SESSIONS.values())) {
				ServerPlayer p = server.getPlayerList().getPlayer(s.player);
				if (p != null) {
					p.teleportTo(server.overworld(), s.returnPos.x, s.returnPos.y, s.returnPos.z, s.returnYaw, 0);
				}
				end(server, s, false, null);
			}
		});
	}

	public static boolean inTrial(ServerPlayer player) {
		return SESSIONS.containsKey(player.getUUID());
	}

	/** Starts the player's race trial. Returns a message key if it can't start, else null. */
	public static String start(ServerPlayer player) {
		RaceData d = RaceService.get(player);
		if (d.stage() < 3 || (d.stage() == 3 && !(d.has("lore") && d.has("found_keeper")))) {
			return "race.grandline.trial.locked";
		}
		if (inTrial(player)) {
			return "race.grandline.trial.already";
		}
		if (player.level() != player.server.overworld()) {
			return "race.grandline.trial.locked";
		}
		int slot = 0;
		java.util.Set<Integer> used = new java.util.HashSet<>();
		SESSIONS.values().forEach(s -> used.add(s.slot));
		while (used.contains(slot)) {
			slot++;
		}
		ServerLevel level = player.server.overworld();
		BlockPos center = new BlockPos(ARENA_X0 + slot * SLOT_SPACING, ARENA_Y, ARENA_Z);
		int seconds = switch (d.kind()) {
			case RABBIT -> 50;
			case ANGEL -> 100;
			case CYBORG -> 120;
			default -> 180;
		};
		Session s = new Session(player, d.kind(), slot, center, level.getGameTime(), level.getGameTime() + seconds * 20L);
		SESSIONS.put(player.getUUID(), s);
		forceChunks(level, center, true);
		build(level, s);
		BlockPos spawn = spawnPoint(s);
		player.teleportTo(level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0, 0);
		player.fallDistance = 0;
		s.bar.addPlayer(player);
		RaceProgress.notify(player, "race.grandline.trial.start." + d.kind().id(), ChatFormatting.LIGHT_PURPLE);
		level.playSound(null, spawn, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6F, 1.4F);
		if (usesWaves(s.race)) {
			nextWave(level, player, s);
		}
		if (s.race == Race.CYBORG) {
			spawnCores(level, player, s);
		}
		return null;
	}

	static boolean usesWaves(Race race) {
		return race == Race.HUMAN || race == Race.SHARK || race == Race.GHOUL;
	}

	static int waves(Race race) {
		return race == Race.HUMAN ? 4 : 3; // Human's 4th wave is the captain
	}

	static BlockPos spawnPoint(Session s) {
		return switch (s.race) {
			case SHARK -> s.center.above(1);
			case ANGEL -> s.center.offset(0, 1, 0);
			default -> s.center.above(1);
		};
	}

	// ---- arena ----

	private static void forceChunks(ServerLevel level, BlockPos center, boolean force) {
		for (int cx = (center.getX() - 32) >> 4; cx <= (center.getX() + 32) >> 4; cx++) {
			for (int cz = (center.getZ() - 32) >> 4; cz <= (center.getZ() + 32) >> 4; cz++) {
				level.setChunkForced(cx, cz, force);
			}
		}
	}

	private static void set(ServerLevel level, Session s, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_CLIENTS);
		s.built.add(pos.immutable());
	}

	private static void build(ServerLevel level, Session s) {
		BlockPos c = s.center;
		switch (s.race) {
			case ANGEL -> {
				disc(level, s, c, 4, Blocks.QUARTZ_BLOCK.defaultBlockState());
				int steps = 9;
				for (int i = 1; i <= steps; i++) {
					double a = i * 0.9;
					int r = 6 + (i % 2);
					BlockPos p = c.offset((int) Math.round(Math.cos(a) * r), i * 3, (int) Math.round(Math.sin(a) * r));
					disc(level, s, p, 1, i == steps ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.WHITE_STAINED_GLASS.defaultBlockState());
					s.markers.add(p.above());
				}
				BlockPos top = c.offset(0, steps * 3 + 3, 0);
				disc(level, s, top, 3, Blocks.QUARTZ_BRICKS.defaultBlockState());
				s.markers.clear();
				s.markers.add(top.above());
			}
			case RABBIT -> {
				disc(level, s, c, 20, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
				wall(level, s, c, 21, 3);
				int[][] order = {{14, 0}, {-12, 10}, {6, -15}, {-15, -8}, {12, 13}, {0, -6}, {-6, 16}, {16, -12}};
				for (int[] o : order) {
					BlockPos m = c.offset(o[0], 0, o[1]);
					set(level, s, m, Blocks.GLOWSTONE.defaultBlockState());
					s.markers.add(m.above());
				}
			}
			case SHARK -> {
				disc(level, s, c.below(3), 14, Blocks.PRISMARINE_BRICKS.defaultBlockState());
				for (int y = -2; y <= 0; y++) {
					discIf(level, s, c.above(y), 14, Blocks.WATER.defaultBlockState());
				}
				ringWall(level, s, c.below(2), 15, 6, Blocks.DARK_PRISMARINE.defaultBlockState());
				disc(level, s, c, 2, Blocks.SEA_LANTERN.defaultBlockState()); // a small island to stand on
			}
			case GHOUL -> {
				disc(level, s, c, 14, Blocks.BLACKSTONE.defaultBlockState());
				wall(level, s, c, 15, 6);
			}
			case CYBORG -> {
				disc(level, s, c, 14, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
				wall(level, s, c, 15, 6);
			}
			default -> {
				disc(level, s, c, 14, Blocks.STONE_BRICKS.defaultBlockState());
				wall(level, s, c, 15, 6);
			}
		}
	}

	private static void disc(ServerLevel level, Session s, BlockPos c, int r, BlockState state) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz <= r * r + r) {
					set(level, s, c.offset(dx, 0, dz), state);
				}
			}
		}
	}

	private static void discIf(ServerLevel level, Session s, BlockPos c, int r, BlockState state) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				BlockPos p = c.offset(dx, 0, dz);
				if (dx * dx + dz * dz <= r * r + r && level.getBlockState(p).isAir()) {
					set(level, s, p, state);
				}
			}
		}
	}

	/** An invisible barrier ring with a visible glass rim at floor level. */
	private static void wall(ServerLevel level, Session s, BlockPos c, int r, int height) {
		ringWall(level, s, c.above(1), r, 1, Blocks.TINTED_GLASS.defaultBlockState());
		ringWall(level, s, c.above(2), r, height, Blocks.BARRIER.defaultBlockState());
	}

	private static void ringWall(ServerLevel level, Session s, BlockPos c, int r, int height, BlockState state) {
		for (int dx = -r - 1; dx <= r + 1; dx++) {
			for (int dz = -r - 1; dz <= r + 1; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 > r * r + r && d2 <= (r + 1) * (r + 1) + r + 1) {
					for (int y = 0; y < height; y++) {
						set(level, s, c.offset(dx, y, dz), state);
					}
				}
			}
		}
	}

	private static void clear(ServerLevel level, Session s) {
		// Water first, so nothing flows as the floor goes.
		for (BlockPos p : s.built) {
			if (!level.getFluidState(p).isEmpty()) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		for (BlockPos p : s.built) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		s.built.clear();
	}

	// ---- enemies ----

	private static int enemyLevel(ServerPlayer player) {
		return Math.max(1, Math.min(PlayerProfileService.get(player).level(), 700));
	}

	private static GrandLineEnemy spawnEnemy(ServerLevel level, Session s, EnemyKind kind, int lvl, BlockPos at) {
		GrandLineEnemy enemy = GrandLineEntities.typeOf(kind).create(level);
		if (enemy == null) {
			return null;
		}
		enemy.addTag(TAG);
		enemy.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
		enemy.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
		enemy.equip();
		enemy.setGrandLineLevel(lvl);
		enemy.setPersistenceRequired();
		level.addFreshEntity(enemy);
		s.enemies.add(enemy.getUUID());
		return enemy;
	}

	private static void nextWave(ServerLevel level, ServerPlayer player, Session s) {
		s.wave++;
		int lvl = enemyLevel(player);
		EnemyKind kind = EnemyKind.forLevel(lvl);
		if (s.race == Race.HUMAN && s.wave == 4) {
			spawnEnemy(level, s, EnemyKind.CAPTAIN, lvl, s.center.offset(0, 1, 9));
		} else {
			int count = 2 + s.wave;
			for (int i = 0; i < count; i++) {
				double a = (Math.PI * 2 * i) / count + s.wave;
				BlockPos at = s.center.offset((int) Math.round(Math.cos(a) * 10), 1, (int) Math.round(Math.sin(a) * 10));
				spawnEnemy(level, s, kind, lvl, at);
			}
		}
		player.displayClientMessage(Component.translatable("race.grandline.trial.wave", s.wave, waves(s.race)).withStyle(ChatFormatting.GOLD), true);
	}

	private static void spawnCores(ServerLevel level, ServerPlayer player, Session s) {
		int lvl = enemyLevel(player);
		int[][] at = {{9, 9}, {-9, 9}, {9, -9}, {-9, -9}};
		for (int[] o : at) {
			GrandLineEnemy core = spawnEnemy(level, s, EnemyKind.RAIDER, lvl, s.center.offset(o[0], 1, o[1]));
			if (core != null) {
				core.setNoAi(true);
				core.setCustomName(Component.translatable("race.grandline.trial.core"));
				core.addTag(TAG + "_core");
			}
		}
		for (int i = 0; i < 3; i++) {
			spawnEnemy(level, s, EnemyKind.forLevel(lvl), lvl, s.center.offset(i * 3 - 3, 1, 0));
		}
	}

	private static int alive(ServerLevel level, Session s, boolean coresOnly) {
		int n = 0;
		for (UUID id : s.enemies) {
			var e = level.getEntity(id);
			if (e != null && e.isAlive() && (!coresOnly || e.getTags().contains(TAG + "_core"))) {
				n++;
			}
		}
		return n;
	}

	// ---- running ----

	private static void tick(MinecraftServer server) {
		if (SESSIONS.isEmpty()) {
			return;
		}
		ServerLevel level = server.overworld();
		long now = level.getGameTime();
		for (Session s : List.copyOf(SESSIONS.values())) {
			ServerPlayer player = server.getPlayerList().getPlayer(s.player);
			if (player == null) {
				end(server, s, false, null);
				continue;
			}
			long left = s.endTime - now;
			if (left <= 0) {
				end(server, s, false, "race.grandline.trial.failed_time");
				continue;
			}
			boolean far = player.level() != level || player.position().distanceTo(Vec3.atCenterOf(s.center)) > 64;
			if (far) {
				end(server, s, false, "race.grandline.trial.failed_left");
				continue;
			}
			s.bar.setProgress(Math.max(0, Math.min(1, left / (float) Math.max(1, s.endTime - s.startTime))));
			s.bar.setName(Component.translatable("race.grandline.trial." + s.race.id()).append(" · " + (left / 20) + "s"));
			if (s.race == Race.GHOUL && now % 40 == 0) {
				player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
			}
			switch (s.race) {
				case ANGEL -> {
					if (player.getY() < s.center.getY() - 6) {
						BlockPos sp = spawnPoint(s);
						player.teleportTo(level, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, player.getYRot(), 0);
						player.fallDistance = 0;
					}
					BlockPos top = s.markers.get(0);
					if (now % 10 == 0) {
						level.sendParticles(ParticleTypes.END_ROD, top.getX() + 0.5, top.getY() + 1, top.getZ() + 0.5, 6, 0.3, 1, 0.3, 0.01);
					}
					if (player.position().distanceTo(Vec3.atBottomCenterOf(top)) < 2.5) {
						end(server, s, true, null);
					}
				}
				case RABBIT -> {
					if (player.getY() < s.center.getY() - 6) {
						end(server, s, false, "race.grandline.trial.failed_left");
						continue;
					}
					BlockPos next = s.markers.get(s.phase);
					if (now % 5 == 0) {
						level.sendParticles(ParticleTypes.END_ROD, next.getX() + 0.5, next.getY() + 0.5, next.getZ() + 0.5, 4, 0.2, 1.2, 0.2, 0.01);
					}
					if (player.position().distanceTo(Vec3.atBottomCenterOf(next)) < 2) {
						s.phase++;
						level.playSound(null, next, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1F, 0.8F + s.phase * 0.1F);
						player.displayClientMessage(Component.translatable("race.grandline.trial.marker", s.phase, s.markers.size())
								.withStyle(ChatFormatting.GOLD), true);
						if (s.phase >= s.markers.size()) {
							end(server, s, true, null);
						}
					}
				}
				case CYBORG -> {
					if (now % 10 == 0 && alive(level, s, true) == 0) {
						end(server, s, true, null);
					}
				}
				default -> {
					if (now % 10 == 0 && alive(level, s, false) == 0) {
						if (s.wave >= waves(s.race)) {
							end(server, s, true, null);
						} else {
							nextWave(level, player, s);
						}
					}
				}
			}
		}
	}

	static void end(MinecraftServer server, Session s, boolean cleared, String failKey) {
		if (s.done) {
			return;
		}
		s.done = true;
		SESSIONS.remove(s.player);
		ServerLevel level = server.overworld();
		for (UUID id : s.enemies) {
			var e = level.getEntity(id);
			if (e != null) {
				e.discard();
			}
		}
		ServerPlayer player = server.getPlayerList().getPlayer(s.player);
		s.bar.removeAllPlayers();
		if (player != null) {
			player.removeEffect(MobEffects.DARKNESS);
			if (player.isAlive() && player.level() == level && player.position().distanceTo(Vec3.atCenterOf(s.center)) < 80) {
				player.teleportTo(level, s.returnPos.x, s.returnPos.y, s.returnPos.z, s.returnYaw, 0);
				player.fallDistance = 0;
			}
			if (cleared) {
				reward(player, s.race);
			} else if (failKey != null) {
				RaceProgress.notify(player, failKey, ChatFormatting.RED);
			}
		}
		clear(level, s);
		forceChunks(level, s.center, false);
	}

	/** Marks the trial cleared and gives Awakening Points (first clear more; capped). */
	static void reward(ServerPlayer player, Race race) {
		var config = GrandLineMod.config();
		RaceData d = RaceService.get(player);
		if (d.kind() != race) {
			return;
		}
		boolean first = !d.has("trial:" + race.id());
		long points = Math.min(config.awakeningPointsMax, d.counter("awaken_points") + (first ? config.awakeningPointsFirstClear : config.awakeningPointsRepeat));
		RaceService.update(player, x -> x.withFlag("trial:" + race.id()).withCounter("awaken_points", points));
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1F, 1.2F);
		Component msg = Component.translatable(first ? "race.grandline.trial.cleared_first" : "race.grandline.trial.cleared", points,
				config.awakeningPointsMax).withStyle(ChatFormatting.GOLD);
		player.sendSystemMessage(msg);
		player.displayClientMessage(msg, true);
	}

	/** The arena center of a player's running trial (tests). */
	public static BlockPos arena(ServerPlayer player) {
		Session s = SESSIONS.get(player.getUUID());
		return s == null ? null : s.center;
	}

	/** Ends a running trial as cleared or failed (tests). */
	public static void finish(ServerPlayer player, boolean cleared) {
		Session s = SESSIONS.get(player.getUUID());
		if (s != null) {
			end(player.server, s, cleared, cleared ? null : "race.grandline.trial.failed_left");
		}
	}
}
