package com.grandline.player;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.network.payload.ProfileSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

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

		sync(player);
		return after;
	}

	public static PlayerProfile addExperience(ServerPlayer player, long amount) {
		return update(player, profile -> rules().addExperience(profile, amount));
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
			ServerPlayNetworking.send(player, new ProfileSyncPayload(get(player)));
		}
	}
}
