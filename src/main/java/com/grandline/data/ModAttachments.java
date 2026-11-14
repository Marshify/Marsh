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

	/** Equipped accessory and rings (persistent, survives death). */
	public static final AttachmentType<com.grandline.gear.EquippedGear> EQUIPPED_GEAR =
			AttachmentRegistry.<com.grandline.gear.EquippedGear>builder()
					.persistent(com.grandline.gear.EquippedGear.CODEC)
					.copyOnDeath()
					.buildAndRegister(GrandLineMod.id("equipped_gear"));

	/** Chosen side: "pirates", "marines" or "" (persistent, survives death). */
	public static final AttachmentType<String> TEAM = AttachmentRegistry.<String>builder()
			.persistent(com.mojang.serialization.Codec.STRING)
			.copyOnDeath()
			.buildAndRegister(GrandLineMod.id("team"));

	/** Bounty and Honor (persistent, survives death). Existing saves start at zero. */
	public static final AttachmentType<com.grandline.reputation.Reputation> REPUTATION =
			AttachmentRegistry.<com.grandline.reputation.Reputation>builder()
					.persistent(com.grandline.reputation.Reputation.CODEC)
					.copyOnDeath()
					.buildAndRegister(GrandLineMod.id("reputation"));

	/** Loadout hotbar order (persistent, survives death). Missing or invalid data loads as the default order. */
	public static final AttachmentType<java.util.List<Integer>> LOADOUT_ORDER =
			AttachmentRegistry.<java.util.List<Integer>>builder()
					.persistent(com.grandline.player.loadout.LoadoutOrder.CODEC)
					.copyOnDeath()
					.buildAndRegister(GrandLineMod.id("loadout_order"));

	/** Ships the player owns and the one Call Ship launches (persistent, survives death). */
	public static final AttachmentType<com.grandline.ship.ShipOwnership> SHIPS =
			AttachmentRegistry.<com.grandline.ship.ShipOwnership>builder()
					.persistent(com.grandline.ship.ShipOwnership.CODEC)
					.copyOnDeath()
					.buildAndRegister(GrandLineMod.id("ships"));

	/**
	 * Which discovery rules the player's DISCOVERED_ISLANDS list was built under (persistent). 0 = before arrival-based
	 * discovery: starter islands were granted on join, so they are re-earned by arriving; other entries are kept.
	 */
	public static final AttachmentType<Integer> DISCOVERY_VERSION = AttachmentRegistry.<Integer>builder()
			.persistent(com.mojang.serialization.Codec.INT)
			.copyOnDeath()
			.buildAndRegister(GrandLineMod.id("discovery_version"));

	/** The player's chosen home island id (persistent, survives death); respawn fallback. Set by NPCs (Part 2). */
	public static final AttachmentType<Long> HOME_ISLAND = AttachmentRegistry.<Long>builder()
			.persistent(com.mojang.serialization.Codec.LONG)
			.copyOnDeath()
			.buildAndRegister(GrandLineMod.id("home_island"));

	/** Set once the player has been placed on the spawn island on their very first join. */
	public static final AttachmentType<Boolean> FIRST_SPAWN_DONE = AttachmentRegistry.<Boolean>builder()
			.persistent(com.mojang.serialization.Codec.BOOL)
			.copyOnDeath()
			.buildAndRegister(GrandLineMod.id("first_spawn_done"));

	/** The island the player last died on (-1: at sea), kept until they respawn, through restarts. */
	public static final AttachmentType<Long> DEATH_ISLAND = AttachmentRegistry.<Long>builder()
			.persistent(com.mojang.serialization.Codec.LONG)
			.copyOnDeath()
			.buildAndRegister(GrandLineMod.id("death_island"));

	/** Islands whose "new quest available" card the player has already seen (or had reached before the card existed). */
	public static final AttachmentType<java.util.List<Long>> QUEST_UNLOCKS_SEEN = AttachmentRegistry.<java.util.List<Long>>builder()
			.persistent(com.mojang.serialization.Codec.LONG.listOf())
			.copyOnDeath()
			.buildAndRegister(GrandLineMod.id("quest_unlocks_seen"));

	private ModAttachments() {
	}

	/** Forces class loading so attachment types are registered during mod initialization. */
	public static void register() {
		GrandLineMod.LOGGER.debug("Registered attachments {} and {}", PLAYER_PROFILE.identifier(), ABILITY_COOLDOWNS.identifier());
	}
}
