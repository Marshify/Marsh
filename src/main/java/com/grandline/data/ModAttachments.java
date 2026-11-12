package com.grandline.data;

import com.grandline.GrandLineMod;
import com.grandline.combat.ability.AbilityCooldowns;
import com.grandline.player.PlayerProfile;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Data attachments stored on game objects.
 *
 * <p>Persistent attachments are serialized into the vanilla player data file ({@code world/playerdata/<uuid>.dat}),
 * which the server writes on autosave, on logout and on shutdown. Vanilla writes that file to a temporary file
 * first and keeps a backup of the previous version, so a crash mid-save does not corrupt progression.
 */
public final class ModAttachments {
	/** Persistent RPG profile. Copied on death so progression is never lost when respawning. */
	public static final AttachmentType<PlayerProfile> PLAYER_PROFILE = AttachmentRegistry.<PlayerProfile>builder()
			.persistent(PlayerProfile.CODEC)
			.copyOnDeath()
			.buildAndRegister(GrandLineMod.id("player_profile"));

	/** Server-only, non-persistent ability cooldown state. Synchronized explicitly via the cooldown payload. */
	public static final AttachmentType<AbilityCooldowns> ABILITY_COOLDOWNS = AttachmentRegistry.<AbilityCooldowns>builder()
			.initializer(AbilityCooldowns::new)
			.buildAndRegister(GrandLineMod.id("ability_cooldowns"));

	/** Server-only, non-persistent energy. */
	public static final AttachmentType<com.grandline.combat.EnergyService.Energy> ENERGY =
			AttachmentRegistry.<com.grandline.combat.EnergyService.Energy>builder()
					.initializer(com.grandline.combat.EnergyService.Energy::new)
					.buildAndRegister(GrandLineMod.id("energy"));

	/** Ids of islands this player has discovered (persistent). */
	public static final AttachmentType<java.util.List<Long>> DISCOVERED_ISLANDS =
			AttachmentRegistry.<java.util.List<Long>>builder()
					.persistent(com.mojang.serialization.Codec.LONG.listOf())
					.copyOnDeath()
					.initializer(java.util.List::of)
					.buildAndRegister(GrandLineMod.id("discovered_islands"));

	/** The player's active quest, if any (persistent, survives death). */
	public static final AttachmentType<com.grandline.npc.QuestState> ACTIVE_QUEST =
			AttachmentRegistry.<com.grandline.npc.QuestState>builder()
					.persistent(com.grandline.npc.QuestState.CODEC)
					.copyOnDeath()
					.buildAndRegister(GrandLineMod.id("active_quest"));

	private ModAttachments() {
	}

	/** Forces class loading so attachment types are registered during mod initialization. */
	public static void register() {
		GrandLineMod.LOGGER.debug("Registered attachments {} and {}", PLAYER_PROFILE.identifier(), ABILITY_COOLDOWNS.identifier());
	}
}
