package com.grandline.race;

import com.grandline.GrandLineMod;
import com.grandline.combat.CombatCredit;
import com.grandline.config.GrandLineConfig;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.IslandBoss;
import com.grandline.player.PlayerProfileService;
import com.grandline.registry.ModItems;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.IslandSpawns;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Race progression, all solo:
 * <ul>
 * <li>V2: start the Race Mystic's quest, reach the level, defeat enemies and visit islands, then pay to evolve.</li>
 * <li>V3: start the Mystic's second quest, reach the level, complete the race's own objective, then pay.</li>
 * <li>V4: reach the level, hear the Mystic's lore and find the Trial Keeper (discovery), carry an Awakening Key (an
 * island boss drop), clear the race's trial, then pay the Trial Keeper to awaken.</li>
 * <li>Cyborg/Ghoul: Machine Cores (Ironhold enemies) / Blood Vials (Stonewall enemies at night), that island's boss
 * defeated (Stonewall's at night), then pay the Tinkerer / Gravekeeper.</li>
 * </ul>
 * Progress only counts while the matching quest is active, and is saved in the player's race data.
 */
public final class RaceProgress {
	public static final String MYSTIC_ISLAND = "Dawnreach Harbor";
	public static final String CYBORG_ISLAND = "Ironhold Bastion";
	public static final String GHOUL_ISLAND = "Stonewall Prison";
	public static final String TRIAL_ISLAND = "Saltmarket Docks";
	/** Blocks of swimming/sprinting per objective point. */
	static final double BLOCKS_PER_POINT = 25;

	private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
	private static final Map<UUID, Float> LAST_HEALTH = new HashMap<>();
	private static final Map<UUID, double[]> DISTANCE = new HashMap<>();

	private RaceProgress() {
	}

	static GrandLineConfig config() {
		return GrandLineMod.config();
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register(RaceProgress::onDeath);
		ServerTickEvents.END_SERVER_TICK.register(RaceProgress::tick);
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUUID();
			LAST_POS.remove(id);
			LAST_HEALTH.remove(id);
			DISTANCE.remove(id);
			RaceAwakening.forget(id);
		});
	}

	// ---- tracking ----

	private static void onDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
		if (!(entity instanceof GrandLineEnemy enemy) || entity.level().isClientSide) {
			return;
		}
		ServerPlayer player = CombatCredit.recentAttacker(entity);
		if (player == null) {
			return;
		}
		RaceData data = RaceService.get(player);
		boolean night = RacePassives.isNight(player.level());
		if (data.has("q2") && data.stage() == 1) {
			add(player, "v2_kills", 1, config().raceV2Kills);
		}
		if (data.has("q3") && data.stage() == 2) {
			if (data.kind() == Race.HUMAN || (data.kind() == Race.GHOUL && night)) {
				add(player, "v3_goal", 1, config().raceV3Goal);
			}
		}
		Island island = IslandRegistry.get(player.server).get(enemy.islandId());
		String name = island == null ? "" : island.name();
		// Race unlock parts: given straight to the player who earned them.
		if (name.equals(CYBORG_ISLAND) && !data.has("unlock:cyborg") && player.getRandom().nextDouble() < config().raceUnlockDropChance) {
			give(player, ModItems.MACHINE_CORE);
		}
		if (name.equals(GHOUL_ISLAND) && night && !data.has("unlock:ghoul") && player.getRandom().nextDouble() < config().raceUnlockDropChance) {
			give(player, ModItems.BLOOD_VIAL);
		}
		if (entity instanceof IslandBoss) {
			if (name.equals(CYBORG_ISLAND) && !data.has("boss:cyborg")) {
				RaceService.update(player, d -> d.withFlag("boss:cyborg"));
				notify(player, "race.grandline.unlock.boss_done", ChatFormatting.AQUA);
			}
			if (name.equals(GHOUL_ISLAND) && night && !data.has("boss:ghoul")) {
				RaceService.update(player, d -> d.withFlag("boss:ghoul"));
				notify(player, "race.grandline.unlock.boss_done", ChatFormatting.DARK_RED);
			}
			if (enemy.grandLineLevel() >= config().raceKeyBossLevel && data.stage() == 3 && !hasItem(player, ModItems.AWAKENING_KEY)
					&& player.getRandom().nextDouble() < config().raceKeyDropChance) {
				give(player, ModItems.AWAKENING_KEY);
				notify(player, "race.grandline.v4.key_found", ChatFormatting.LIGHT_PURPLE);
			}
		}
	}

	private static void tick(MinecraftServer server) {
		boolean check = server.getTickCount() % 20 == 0;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			RaceData data = RaceService.get(player);
			UUID id = player.getUUID();
			boolean q3 = data.has("q3") && data.stage() == 2;
			if (q3 && (data.kind() == Race.SHARK || data.kind() == Race.RABBIT)) {
				Vec3 now = player.position();
				Vec3 last = LAST_POS.put(id, now);
				if (last != null) {
					double moved = now.distanceTo(last);
					boolean counts = moved < 2 && (data.kind() == Race.SHARK ? player.isSwimming() || player.isInWater() : player.isSprinting() && player.onGround());
					if (counts) {
						double[] total = DISTANCE.computeIfAbsent(id, k -> new double[1]);
						total[0] += moved;
						if (total[0] >= BLOCKS_PER_POINT) {
							total[0] -= BLOCKS_PER_POINT;
							add(player, "v3_goal", 1, config().raceV3Goal);
						}
					}
				}
			}
			if (q3 && data.kind() == Race.ANGEL) {
				float health = player.getHealth();
				Float last = LAST_HEALTH.put(id, health);
				if (last != null && health > last && player.isAlive()) {
					double[] total = DISTANCE.computeIfAbsent(id, k -> new double[1]);
					total[0] += health - last;
					while (total[0] >= 2) { // one point per heart healed
						total[0] -= 2;
						add(player, "v3_goal", 1, config().raceV3Goal);
					}
				}
			}
			if (check && data.has("q2") && data.stage() == 1) {
				Island island = IslandSpawns.islandAt(player.serverLevel(), player.getBlockX(), player.getBlockZ());
				if (island != null && !data.has("isl:" + island.id()) && visited(data) < config().raceV2Islands) {
					RaceService.update(player, d -> d.withFlag("isl:" + island.id()));
					player.displayClientMessage(Component.translatable("race.grandline.v2.island", island.name(),
							visited(RaceService.get(player)), config().raceV2Islands).withStyle(ChatFormatting.GOLD), true);
				}
			}
		}
	}

	/** Cyborg V3 objective: each move used counts (called when a move's cooldown starts). */
	public static void onMoveUsed(ServerPlayer player) {
		RaceData data = RaceService.get(player);
		if (data.kind() == Race.CYBORG && data.has("q3") && data.stage() == 2) {
			add(player, "v3_goal", 1, config().raceV3Goal);
		}
	}

	static int visited(RaceData data) {
		return (int) data.flags().stream().filter(f -> f.startsWith("isl:")).count();
	}

	private static void add(ServerPlayer player, String counter, int amount, int goal) {
		RaceData data = RaceService.get(player);
		long now = data.counter(counter);
		if (now >= goal) {
			return;
		}
		long next = Math.min(goal, now + amount);
		RaceService.update(player, d -> d.withCounter(counter, next));
		if (next >= goal) {
			notify(player, "race.grandline.quest.ready", ChatFormatting.GREEN);
		}
	}

	// ---- requirements ----

	/** One requirement line for the race menu. */
	record Line(String key, boolean done, Object... args) {
	}

	/** The requirements for the player's next step (shown in the menu, checked when evolving). */
	static java.util.List<Line> requirements(ServerPlayer player) {
		RaceData d = RaceService.get(player);
		int level = PlayerProfileService.get(player).level();
		long money = PlayerProfileService.get(player).currency();
		java.util.List<Line> lines = new java.util.ArrayList<>();
		int stage = d.stage();
		if (stage >= 4) {
			return lines;
		}
		int needLevel = config().raceEvolutionLevels[stage - 1];
		long cost = config().raceEvolutionCosts[stage - 1];
		if (stage == 1) {
			lines.add(new Line("race.grandline.req.quest_v2", d.has("q2")));
			lines.add(new Line("race.grandline.req.level", level >= needLevel, needLevel));
			lines.add(new Line("race.grandline.req.kills", d.counter("v2_kills") >= config().raceV2Kills, d.counter("v2_kills"), config().raceV2Kills));
			lines.add(new Line("race.grandline.req.islands", visited(d) >= config().raceV2Islands, visited(d), config().raceV2Islands));
		} else if (stage == 2) {
			lines.add(new Line("race.grandline.req.quest_v3", d.has("q3")));
			lines.add(new Line("race.grandline.req.level", level >= needLevel, needLevel));
			lines.add(new Line("race.grandline.req.v3." + d.kind().id(), d.counter("v3_goal") >= config().raceV3Goal, d.counter("v3_goal"), config().raceV3Goal));
		} else {
			lines.add(new Line("race.grandline.req.lore", d.has("lore")));
			lines.add(new Line("race.grandline.req.keeper", d.has("found_keeper")));
			lines.add(new Line("race.grandline.req.level", level >= needLevel, needLevel));
			lines.add(new Line("race.grandline.req.key", hasItem(player, ModItems.AWAKENING_KEY), config().raceKeyBossLevel));
			lines.add(new Line("race.grandline.req.trial." + d.kind().id(), d.has("trial:" + d.kind().id())));
		}
		lines.add(new Line("race.grandline.req.money", money >= cost, cost));
		return lines;
	}

	static boolean allDone(java.util.List<Line> lines) {
		return lines.stream().allMatch(Line::done);
	}

	/** Lines for unlocking (Tinkerer / Gravekeeper). */
	static java.util.List<Line> unlockRequirements(ServerPlayer player, Race race) {
		RaceData d = RaceService.get(player);
		java.util.List<Line> lines = new java.util.ArrayList<>();
		if (d.has("unlock:" + race.id())) {
			lines.add(new Line("race.grandline.req.unlocked", true));
			lines.add(new Line("race.grandline.req.money", PlayerProfileService.get(player).currency() >= config().raceRerollCost, config().raceRerollCost));
			return lines;
		}
		Item part = race == Race.CYBORG ? ModItems.MACHINE_CORE : ModItems.BLOOD_VIAL;
		int have = count(player, part);
		lines.add(new Line("race.grandline.req.parts." + race.id(), have >= config().raceUnlockParts, Math.min(have, config().raceUnlockParts), config().raceUnlockParts));
		lines.add(new Line("race.grandline.req.boss." + race.id(), d.has("boss:" + race.id())));
		lines.add(new Line("race.grandline.req.money", PlayerProfileService.get(player).currency() >= config().raceUnlockCost, config().raceUnlockCost));
		return lines;
	}

	static ListTag toTag(java.util.List<Line> lines) {
		ListTag list = new ListTag();
		for (Line line : lines) {
			CompoundTag t = new CompoundTag();
			t.putString("k", line.key());
			t.putBoolean("d", line.done());
			ListTag args = new ListTag();
			for (Object a : line.args()) {
				args.add(StringTag.valueOf(a instanceof Long || a instanceof Integer ? String.format(java.util.Locale.ROOT, "%,d", ((Number) a).longValue()) : String.valueOf(a)));
			}
			t.put("a", args);
			list.add(t);
		}
		return list;
	}

	// ---- helpers ----

	static boolean hasItem(ServerPlayer player, Item item) {
		return count(player, item) > 0;
	}

	static int count(ServerPlayer player, Item item) {
		int n = 0;
		for (ItemStack stack : player.getInventory().items) {
			if (stack.is(item)) {
				n += stack.getCount();
			}
		}
		return n;
	}

	/** Removes {@code amount} of an item; false (nothing removed) if the player doesn't have that many. */
	static boolean take(ServerPlayer player, Item item, int amount) {
		if (count(player, item) < amount) {
			return false;
		}
		int left = amount;
		for (ItemStack stack : player.getInventory().items) {
			if (left > 0 && stack.is(item)) {
				int n = Math.min(left, stack.getCount());
				stack.shrink(n);
				left -= n;
			}
		}
		return true;
	}

	static void give(ServerPlayer player, Item item) {
		ItemStack stack = new ItemStack(item);
		player.displayClientMessage(Component.translatable("race.grandline.item_found", stack.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	static void notify(ServerPlayer player, String key, ChatFormatting color) {
		Component c = Component.translatable(key).withStyle(color);
		player.sendSystemMessage(c);
		player.displayClientMessage(c, true);
	}

	/** Clears saved trackers (tests). */
	public static void forget(UUID id) {
		LAST_POS.remove(id);
		LAST_HEALTH.remove(id);
		DISTANCE.remove(id);
	}
}
