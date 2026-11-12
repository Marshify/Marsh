package com.grandline.player;

import com.grandline.GrandLineMod;
import com.grandline.combat.StatEffects;
import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.MasteryRules;
import com.grandline.combat.ability.Power;
import com.grandline.data.ModAttachments;
import com.grandline.network.payload.ProfileSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * The only write path for player profiles.
 *
 * <p>All methods take a {@link ServerPlayer}, so they can only be called with server-side objects. Network handlers
 * never receive profile values from clients; they may only ask this service to perform validated operations.
 * Every change is persisted through the attachment system (saved with the vanilla player data file) and then pushed
 * to the owning client.
 */
public final class PlayerProfileService {
	private PlayerProfileService() {
	}

	public static ProgressionRules rules() {
		return ProgressionRules.fromConfig(GrandLineMod.config());
	}

	public static StatRules statRules() {
		return StatRules.fromConfig(GrandLineMod.config());
	}

	/** Returns the player's profile, creating and persisting a default one on first access. */
	public static PlayerProfile get(ServerPlayer player) {
		return player.getAttachedOrCreate(ModAttachments.PLAYER_PROFILE,
				() -> PlayerProfile.createDefault(GrandLineMod.config().startingCurrency));
	}

	/** Applies {@code operation} to the profile, stores the result and synchronizes it to the client. */
	public static PlayerProfile update(ServerPlayer player, UnaryOperator<PlayerProfile> operation) {
		PlayerProfile before = get(player);
		PlayerProfile after = Objects.requireNonNull(operation.apply(before), "Profile operation returned null").sanitized();

		if (!after.equals(before)) {
			player.setAttached(ModAttachments.PLAYER_PROFILE, after);
		}

		StatEffects.apply(player, after);
		sync(player);

		if (after.level() > before.level()) {
			int points = after.unallocatedStatPoints() - before.unallocatedStatPoints();
			player.sendSystemMessage(Component.translatable("message.grandline.level_up", after.level(), Math.max(0, points))
					.withStyle(ChatFormatting.GOLD));
			player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75F, 1.0F);
		}
		return after;
	}

	public static PlayerProfile addExperience(ServerPlayer player, long amount) {
		return update(player, profile -> rules().addExperience(profile, amount));
	}

	/**
	 * Server-validated stat allocation. The profile is only changed when the result is SUCCESS; the client is always
	 * resynced so that a rejected request cannot leave the client showing an optimistic value.
	 */
	public static StatRules.AllocationResult allocateStat(ServerPlayer player, StatType stat, int amount) {
		StatRules.AllocationResult[] result = new StatRules.AllocationResult[1];
		update(player, profile -> {
			StatRules.Allocation allocation = statRules().allocate(profile, stat, amount);
			result[0] = allocation.result();
			return allocation.profile();
		});
		return result[0];
	}

	/**
	 * Adds mastery experience for {@code power} and announces newly unlocked moves.
	 */
	public static PlayerProfile addMastery(ServerPlayer player, Power power, long experience) {
		String id = power.id().toString();
		MasteryRules rules = AbilityActivationHandler.masteryRules();
		int before = rules.level(get(player).powers().masteryXp(id));
		PlayerProfile after = update(player, profile -> profile.withPowers(
				profile.powers().withMasteryXp(id, profile.powers().masteryXp(id) + experience)));
		int now = rules.level(after.powers().masteryXp(id));
		if (now > before) {
			for (AbilityDef ability : power.abilities()) {
				if (ability.requiredMastery() > before && ability.requiredMastery() <= now) {
					player.sendSystemMessage(Component.translatable("message.grandline.ability.unlocked",
							Component.translatable(ability.translationKey()), Component.translatable(power.translationKey()))
							.withStyle(ChatFormatting.LIGHT_PURPLE));
				}
			}
		}
		return after;
	}

	/** Sets mastery level directly (admin). */
	public static PlayerProfile setMasteryLevel(ServerPlayer player, Power power, int level) {
		long xp = (long) Math.max(0, level - 1) * AbilityActivationHandler.masteryRules().experiencePerLevel();
		return update(player, profile -> profile.withPowers(profile.powers().withMasteryXp(power.id().toString(), xp)));
	}

	public static PlayerProfile resetStats(ServerPlayer player) {
		return update(player, profile -> statRules().resetStats(profile));
	}

	public static PlayerProfile addCurrency(ServerPlayer player, long amount) {
		return update(player, profile -> rules().addCurrency(profile, amount));
	}

	public static PlayerProfile setLevel(ServerPlayer player, int level) {
		return update(player, profile -> rules().setLevel(profile, level));
	}

	public static PlayerProfile reset(ServerPlayer player) {
		return update(player, profile -> PlayerProfile.createDefault(GrandLineMod.config().startingCurrency));
	}

	/** Sends the authoritative profile to the owning client, if it has the mod installed. */
	public static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, ProfileSyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, ProfileSyncPayload.of(get(player), rules(), statRules(), AbilityActivationHandler.masteryRules()));
		}
	}
}
