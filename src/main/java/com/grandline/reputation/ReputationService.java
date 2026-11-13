package com.grandline.reputation;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.IslandBoss;
import com.grandline.network.payload.ReputationSyncPayload;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.TeamService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Bounty (Pirates) and Honor (Marines). Server-authoritative: every change happens here, from the real death and
 * damage events, and is then synced to the player.
 *
 * <ul>
 *   <li>PvE: called from the kill-reward hook ({@link #onMobKilled}) with the same killer that quest credit uses, so
 *   one death pays once.</li>
 *   <li>PvP: player deaths credited to a player (directly, by projectile, by fruit move, or by vanilla's kill credit for
 *   e.g. a knock-off fall) follow the PvP rules, including the per-pair limit. Deaths credited to a Grand Line enemy
 *   cost that enemy's level. Uncredited deaths cost nothing.</li>
 *   <li>Combat tag: valid PvP damage tags both players; while tagged they cannot switch sides. A tagged player at risk
 *   who received eligible player damage and disconnects takes the PvP loss once (normal shutdown is exempt).</li>
 * </ul>
 */
public final class ReputationService {
	private static final String DATA_NAME = "grandline_reputation";
	private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.ROOT);

	/** A combat tag: until when, and the last eligible player who damaged this player (if any). */
	private record Tag(int untilTick, UUID attacker, String attackerName, int attackerLevel, boolean attackerPirate, boolean received) {
	}

	private static final Map<UUID, Tag> TAGS = new HashMap<>();
	private static boolean stopping;
	/** Test hook: when set, used instead of the wall clock for the pair window. */
	static Long clockOverride;

	private ReputationService() {
	}

	public static ReputationRules rules() {
		return ReputationRules.fromConfig(GrandLineMod.config());
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register(ReputationService::onDeath);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer victim && source.getEntity() instanceof ServerPlayer attacker) {
				onPvpDamage(attacker, victim);
			}
			return true;
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onDisconnect(handler.getPlayer()));
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			stopping = false;
			TAGS.clear();
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> stopping = true);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0 || TAGS.isEmpty()) {
				return;
			}
			int now = server.getTickCount();
			TAGS.entrySet().removeIf(e -> {
				ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
				boolean expired = e.getValue().untilTick() <= now;
				if (p != null) {
					sync(p); // countdown / clear
				}
				return expired;
			});
		});
	}

	// ------------------------------------------------------------------------------------------------ state

	public static Reputation get(ServerPlayer player) {
		return player.getAttachedOrElse(ModAttachments.REPUTATION, Reputation.ZERO);
	}

	/** The player's side, or null if they have not picked one. */
	static Boolean pirate(ServerPlayer player) {
		TeamService.Side side = TeamService.get(player);
		return side == null ? null : side == TeamService.Side.PIRATES;
	}

	/** The total the player currently earns and shows (Bounty for Pirates, Honor for Marines); 0 without a side. */
	public static long active(ServerPlayer player) {
		Boolean pirate = pirate(player);
		return pirate == null ? 0 : get(player).get(pirate);
	}

	static void set(ServerPlayer player, boolean pirate, long value) {
		Reputation r = get(player).with(pirate, rules().clamp(value));
		player.setAttached(ModAttachments.REPUTATION, r);
		data(player.server).updateBoard(player.getUUID(), player.getGameProfile().getName(), r.bounty(), r.honor());
		sync(player);
	}

	/** Admin/test: set a total directly (clamped). */
	public static void setTotal(ServerPlayer player, boolean pirate, long value) {
		set(player, pirate, value);
	}

	static ReputationData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(ReputationData::load, ReputationData::new, DATA_NAME);
	}

	public static boolean isTagged(ServerPlayer player) {
		Tag tag = TAGS.get(player.getUUID());
		return tag != null && tag.untilTick() > player.server.getTickCount();
	}

	public static int tagTicksLeft(ServerPlayer player) {
		Tag tag = TAGS.get(player.getUUID());
		return tag == null ? 0 : Math.max(0, tag.untilTick() - player.server.getTickCount());
	}

	private static long now() {
		return clockOverride != null ? clockOverride : System.currentTimeMillis();
	}

	// ------------------------------------------------------------------------------------------------ PvE

	/** Called once per mob death, with the same killer quest credit uses. */
	public static void onMobKilled(ServerPlayer killer, LivingEntity killed) {
		if (!(killed instanceof GrandLineEnemy enemy)) {
			return; // only Grand Line enemies give reputation
		}
		Boolean pirate = pirate(killer);
		if (pirate == null) {
			return;
		}
		long before = get(killer).get(pirate);
		long after = killed instanceof IslandBoss ? rules().afterBoss(before, enemy.grandLineLevel())
				: rules().afterEnemy(before, enemy.grandLineLevel());
		if (after != before) {
			set(killer, pirate, after);
			feedback(killer, Component.translatable("message.grandline.reputation.gain", label(pirate), "+" + NUMBERS.format(after - before)),
					ChatFormatting.GOLD);
		}
	}

	// ------------------------------------------------------------------------------------------------ PvP

	/** Why PvP reputation was or was not awarded. */
	public enum Result { AWARDED, NO_SIDE, PVP_OFF, CANNOT_HARM, LEVEL_GAP, PAIR_LIMIT, SELF }

	/** Checks everything except the pair limit. */
	static Result eligibility(ServerPlayer killer, ServerPlayer victim) {
		if (killer == victim) {
			return Result.SELF;
		}
		if (pirate(killer) == null || pirate(victim) == null) {
			return Result.NO_SIDE;
		}
		if (!killer.server.isPvpAllowed()) {
			return Result.PVP_OFF;
		}
		if (!killer.canHarmPlayer(victim)) {
			return Result.CANNOT_HARM; // team rules (Marines cannot hurt Marines)
		}
		int a = PlayerProfileService.get(killer).level();
		int b = PlayerProfileService.get(victim).level();
		return rules().levelsEligible(a, b) ? Result.AWARDED : Result.LEVEL_GAP;
	}

	private static void onPvpDamage(ServerPlayer attacker, ServerPlayer victim) {
		if (eligibility(attacker, victim) != Result.AWARDED) {
			return;
		}
		int until = attacker.server.getTickCount() + GrandLineMod.config().combatTagSeconds * 20;
		Tag mine = TAGS.get(attacker.getUUID());
		TAGS.put(attacker.getUUID(), new Tag(until, mine == null ? null : mine.attacker(), mine == null ? "" : mine.attackerName(),
				mine == null ? 0 : mine.attackerLevel(), mine != null && mine.attackerPirate(), mine != null && mine.received()));
		TAGS.put(victim.getUUID(), new Tag(until, attacker.getUUID(), attacker.getGameProfile().getName(),
				PlayerProfileService.get(attacker).level(), Boolean.TRUE.equals(pirate(attacker)), true));
		sync(attacker);
		sync(victim);
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof ServerPlayer victim)) {
			return;
		}
		if (source.getEntity() instanceof ServerPlayer killer) {
			pvpKill(killer, victim);
		} else if (source.getEntity() instanceof GrandLineEnemy enemy) {
			npcKill(victim, enemy.grandLineLevel());
		} else if (source.getEntity() == null && victim.getKillCredit() instanceof ServerPlayer credited) {
			pvpKill(credited, victim); // e.g. knocked off a cliff: vanilla credits the player who caused it
		}
		TAGS.remove(victim.getUUID());
		sync(victim);
	}

	/** Applies a PvP kill. Returns what happened (and does nothing unless it is AWARDED). */
	public static Result pvpKill(ServerPlayer killer, ServerPlayer victim) {
		Result eligible = eligibility(killer, victim);
		if (eligible != Result.AWARDED) {
			if (eligible == Result.LEVEL_GAP) {
				feedback(killer, Component.translatable("message.grandline.reputation.rejected.level"), ChatFormatting.GRAY);
			}
			return eligible;
		}
		boolean killerPirate = pirate(killer);
		boolean victimPirate = pirate(victim);
		return settle(killer.getUUID(), killer.getGameProfile().getName(), killer, killerPirate, victim, victimPirate,
				PlayerProfileService.get(victim).level(), false);
	}

	/**
	 * Settles one PvP result: pair limit, payout from the victim's active total before the kill, victim loss (computed
	 * before the winner's cap), winner gain clamped to their own cap. The winner may be offline (then it is owed).
	 */
	private static Result settle(UUID winnerId, String winnerName, ServerPlayer winner, boolean winnerPirate, ServerPlayer victim,
			boolean victimPirate, int victimLevel, boolean disconnect) {
		ReputationData data = data(victim.server);
		long now = now();
		long window = GrandLineMod.config().pvpPairWindowHours * 3_600_000L;
		if (data.pairCount(winnerId, victim.getUUID(), now, window) >= GrandLineMod.config().pvpPairLimit) {
			if (winner != null) {
				feedback(winner, Component.translatable("message.grandline.reputation.rejected.pair", victim.getGameProfile().getName()),
						ChatFormatting.GRAY);
			}
			return Result.PAIR_LIMIT;
		}
		data.recordPair(winnerId, victim.getUUID(), now);

		ReputationRules rules = rules();
		long victimBefore = get(victim).get(victimPirate);
		long payout = rules.payout(victimLevel, victimBefore);
		long loss = rules.loss(victimBefore, payout);
		if (loss > 0) {
			set(victim, victimPirate, victimBefore - loss);
		}
		String lossText = Component.translatable("message.grandline.reputation.loss", label(victimPirate), "-" + NUMBERS.format(loss)).getString();
		if (disconnect) {
			// Shown when they come back.
			victim.setAttached(ModAttachments.REPUTATION, get(victim).withNotice(Component.translatable(
					"message.grandline.reputation.disconnect", winnerName, lossText).getString()));
		} else if (loss > 0) {
			feedback(victim, Component.literal(lossText), ChatFormatting.RED);
		}

		if (winner != null) {
			long before = get(winner).get(winnerPirate);
			long after = rules.clamp(before + payout);
			set(winner, winnerPirate, after);
			feedback(winner, Component.translatable("message.grandline.reputation.gain", label(winnerPirate), "+" + NUMBERS.format(after - before)),
					ChatFormatting.GOLD);
		} else {
			data.addPending(winnerId, winnerPirate ? payout : 0, winnerPirate ? 0 : payout,
					Component.translatable("message.grandline.reputation.owed", label(winnerPirate), "+" + NUMBERS.format(payout),
							victim.getGameProfile().getName()).getString());
		}
		return Result.AWARDED;
	}

	private static void npcKill(ServerPlayer victim, int npcLevel) {
		Boolean pirate = pirate(victim);
		if (pirate == null || npcLevel <= 0) {
			return;
		}
		long before = get(victim).get(pirate);
		long after = rules().clamp(before - npcLevel);
		if (after != before) {
			set(victim, pirate, after);
			feedback(victim, Component.translatable("message.grandline.reputation.loss", label(pirate), "-" + NUMBERS.format(before - after)),
					ChatFormatting.RED);
		}
	}

	// ------------------------------------------------------------------------------------------------ connection

	/** A tagged player at risk who took eligible player damage and leaves loses as if killed (once). */
	static void onDisconnect(ServerPlayer player) {
		Tag tag = TAGS.remove(player.getUUID());
		if (stopping || tag == null || tag.untilTick() <= player.server.getTickCount() || !tag.received() || tag.attacker() == null) {
			return;
		}
		Boolean pirate = pirate(player);
		if (pirate == null || get(player).get(pirate) < rules().riskThreshold()) {
			return;
		}
		// Same eligibility as a kill, judged from what the tag recorded (the attacker may have left too).
		if (!rules().levelsEligible(tag.attackerLevel(), PlayerProfileService.get(player).level())) {
			return;
		}
		ServerPlayer attacker = player.server.getPlayerList().getPlayer(tag.attacker());
		settle(tag.attacker(), tag.attackerName(), attacker, tag.attackerPirate(), player, pirate, PlayerProfileService.get(player).level(), true);
	}

	private static void onJoin(ServerPlayer player) {
		Reputation r = get(player);
		ReputationData.Pending owed = data(player.server).takePending(player.getUUID());
		if (owed != null) {
			r = new Reputation(rules().clamp(r.bounty() + owed.bounty()), rules().clamp(r.honor() + owed.honor()), r.notice());
			player.setAttached(ModAttachments.REPUTATION, r);
			if (!owed.notice().isEmpty()) {
				player.sendSystemMessage(Component.literal(owed.notice()).withStyle(ChatFormatting.GOLD));
			}
		}
		if (!r.notice().isEmpty()) {
			player.sendSystemMessage(Component.literal(r.notice()).withStyle(ChatFormatting.RED));
			player.setAttached(ModAttachments.REPUTATION, r.withNotice(""));
		}
		data(player.server).updateBoard(player.getUUID(), player.getGameProfile().getName(), get(player).bounty(), get(player).honor());
		sync(player);
	}

	// ------------------------------------------------------------------------------------------------ client

	public static Component label(boolean pirate) {
		return Component.translatable(pirate ? "reputation.grandline.bounty" : "reputation.grandline.honor");
	}

	private static void feedback(ServerPlayer player, Component text, ChatFormatting color) {
		player.displayClientMessage(text.copy().withStyle(color), true);
	}

	public static void sync(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, ReputationSyncPayload.TYPE)) {
			return;
		}
		Reputation r = get(player);
		Boolean pirate = pirate(player);
		int side = pirate == null ? -1 : pirate ? 0 : 1;
		boolean atRisk = pirate != null && r.get(pirate) >= rules().riskThreshold();
		ServerPlayNetworking.send(player, new ReputationSyncPayload(r.bounty(), r.honor(), side, tagTicksLeft(player), atRisk));
	}

	/**
	 * PvP-only tier bonuses: the attacker's tier raises damage, the victim's tier reduces it (faster per tier). Only for
	 * player-versus-player hits; returns {@code amount} unchanged for mobs or when bonuses are off (the default).
	 */
	public static float pvpDamage(LivingEntity target, DamageSource source, float amount) {
		var config = GrandLineMod.config();
		if (!config.reputationCombatBonuses || !(target instanceof ServerPlayer victim) || !(source.getEntity() instanceof ServerPlayer attacker)
				|| attacker == victim) {
			return amount;
		}
		double up = 1 + ReputationRules.tier(active(attacker)) * config.pvpDamagePerTier;
		double down = 1 + ReputationRules.tier(active(victim)) * config.pvpDefensePerTier;
		return (float) (amount * up / down);
	}

	/** Test hook: drop all combat tags. */
	static void clearTags() {
		TAGS.clear();
	}
}
