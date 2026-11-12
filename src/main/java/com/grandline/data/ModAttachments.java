package com.grandline.data;

import com.grandline.GrandLineMod;
import com.grandline.combat.ability.AbilityCooldowns;
import com.grandline.player.PlayerProfile;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Data attachments stored on game objects.
 *
 * <p>Persistent attachments are serialized into the vanilla player data file ({@code world/players/data/<uuid>.dat}),
 * which the server writes on autosave, on logout and on shutdown. Vanilla writes that file to a temporary file
 * first and keeps a backup of the previous version, so a crash mid-save does not corrupt progression.
 */
public final class ModAttachments {
	/** Persistent RPG profile. Copied on death so progression is never lost when respawning. */
	public static final AttachmentType<PlayerProfile> PLAYER_PROFILE = AttachmentRegistry.create(
			GrandLineMod.id("player_profile"),
			builder -> builder.persistent(PlayerProfile.CODEC).copyOnDeath());

	/** Server-only, non-persistent ability cooldown state. Synchronized explicitly via the cooldown payload. */
	public static final AttachmentType<AbilityCooldowns> ABILITY_COOLDOWNS = AttachmentRegistry.create(
			GrandLineMod.id("ability_cooldowns"),
			builder -> builder.initializer(AbilityCooldowns::new));

	private ModAttachments() {
	}

	/** Forces class loading so attachment types are registered during mod initialization. */
	public static void register() {
		GrandLineMod.LOGGER.debug("Registered attachments {} and {}", PLAYER_PROFILE.identifier(), ABILITY_COOLDOWNS.identifier());
	}
}
