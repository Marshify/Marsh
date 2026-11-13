package com.grandline.player.reward;

import com.grandline.GrandLineMod;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Grants experience and currency when a player kills a hostile mob. Everything is decided on the server from the
 * actual death event; the client is only told the result.
 *
 * <p>Rules: only {@link Enemy} mobs give rewards (no farming passive animals or other players), the killer must be a
 * player (directly or via their projectile), and per-player per-minute caps limit mob-farm abuse.
 */
public final class MobKillRewards {
	private static final Map<UUID, RewardRules.Budget> BUDGETS = new HashMap<>();

	private MobKillRewards() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register(MobKillRewards::onDeath);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> BUDGETS.remove(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> BUDGETS.clear());
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof Enemy) || !(source.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		grant(player, entity.getMaxHealth());
		com.grandline.combat.ability.KillCredit.onKill(player, entity);
		com.grandline.npc.QuestService.onKill(player, entity);
		com.grandline.reputation.ReputationService.onMobKilled(player, entity);
	}

	/** Grants the (budget-limited) reward for killing a mob with {@code mobMaxHealth}. Returns what was granted. */
	public static RewardRules.Reward grant(ServerPlayer player, float mobMaxHealth) {
		RewardRules base0 = RewardRules.fromConfig(GrandLineMod.config());
		// Anti-farm caps grow with the player's level so high-level enemies stay worth fighting.
		double capScale = 1.0 + PlayerProfileService.get(player).level() / 25.0;
		RewardRules rules = new RewardRules(base0.experiencePerMobHealth(), base0.currencyPerMobHealth(),
				(long) (base0.maxExperiencePerMinute() * capScale), (long) (base0.maxCurrencyPerMinute() * capScale));
		RewardRules.Reward base = rules.rewardFor(mobMaxHealth);
		if (base.isEmpty()) {
			return RewardRules.Reward.NONE;
		}

		RewardRules.Budget budget = BUDGETS.computeIfAbsent(player.getUUID(), uuid -> new RewardRules.Budget());
		RewardRules.Reward granted = budget.claim(rules, base, player.server.getTickCount());
		if (granted.isEmpty()) {
			player.displayClientMessage(Component.translatable("message.grandline.reward_capped").withStyle(ChatFormatting.GRAY), true);
			return granted;
		}

		if (granted.currency() > 0) {
			PlayerProfileService.addCurrency(player, granted.currency());
		}
		if (granted.experience() > 0) {
			PlayerProfileService.addExperience(player, granted.experience());
		}

		player.displayClientMessage(Component.translatable("message.grandline.kill_reward", granted.experience(), granted.currency())
				.withStyle(ChatFormatting.AQUA), true);
		return granted;
	}
}
