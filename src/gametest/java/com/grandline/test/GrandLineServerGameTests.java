package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.combat.ability.AbilityActivationHandler;
import com.grandline.combat.ability.AbilityCooldownService;
import com.grandline.data.ModAttachments;
import com.grandline.network.ServerRequestGuard;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.player.PlayerProfile;
import com.grandline.player.PlayerProfileService;
import com.grandline.registry.ModItems;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Server-side game tests. Run headless with {@code ./gradlew runGameTest}; these execute inside a real
 * (dedicated-style) Minecraft server with the mod loaded.
 *
 * <p>{@code makeMockServerPlayerInLevel()} is marked for removal by Mojang but is still the only helper that yields a
 * {@link ServerPlayer} with a (fake) network connection, which the profile/cooldown services need. Revisit when a
 * replacement ships.
 */
@SuppressWarnings("removal")
public final class GrandLineServerGameTests {
	@GameTest
	public void crystalItemIsRegistered(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.ITEM.getValue(GrandLineMod.id("grand_line_crystal")) == ModItems.GRAND_LINE_CRYSTAL,
				"grandline:grand_line_crystal must resolve to the registered item");
		helper.succeed();
	}

	@GameTest
	public void newPlayersGetDefaultProfile(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerProfile profile = PlayerProfileService.get(player);
		helper.assertValueEqual(profile, PlayerProfile.createDefault(GrandLineMod.config().startingCurrency), "default profile");
		helper.succeed();
	}

	/** Saves a player through the vanilla player-data serialization path and loads it into a fresh player. */
	@GameTest
	public void profileSurvivesPlayerSaveAndLoad(GameTestHelper helper) {
		ServerPlayer original = helper.makeMockServerPlayerInLevel();
		PlayerProfileService.addExperience(original, 260);
		PlayerProfileService.addCurrency(original, 5_000);
		PlayerProfile expected = PlayerProfileService.get(original);
		helper.assertTrue(expected.level() > 1, "experience should have produced a level-up");

		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		original.saveWithoutId(output);
		CompoundTag saved = output.buildResult();

		CompoundTag attachments = saved.getCompoundOrEmpty(AttachmentTarget.NBT_ATTACHMENT_KEY);
		helper.assertTrue(attachments.contains(ModAttachments.PLAYER_PROFILE.identifier().toString()),
				"player NBT must contain the grandline:player_profile attachment, got keys " + attachments.keySet());

		ServerPlayer reloaded = helper.makeMockServerPlayerInLevel();
		// The mock player went through the normal join flow, so it already has a fresh default profile.
		helper.assertFalse(PlayerProfileService.get(reloaded).equals(expected), "fresh player starts with a different (default) profile");
		reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));

		helper.assertValueEqual(PlayerProfileService.get(reloaded), expected, "reloaded profile");
		helper.succeed();
	}

	@GameTest
	public void cooldownAttachmentIsNotPersisted(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		AbilityCooldownService.start(player, GrandLineMod.id("test_ability"), 200);
		helper.assertTrue(AbilityCooldownService.isOnCooldown(player, GrandLineMod.id("test_ability")), "cooldown active");

		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
		player.saveWithoutId(output);
		CompoundTag attachments = output.buildResult().getCompoundOrEmpty(AttachmentTarget.NBT_ATTACHMENT_KEY);
		helper.assertFalse(attachments.contains(ModAttachments.ABILITY_COOLDOWNS.identifier().toString()),
				"transient cooldowns must not be written to disk");
		helper.succeed();
	}

	/** A client can only send intents; malformed or spammed intents are rejected and never touch the profile. */
	@GameTest
	public void abilityRequestsAreValidatedAndCannotModifyProfile(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerProfile before = PlayerProfileService.get(player);
		long rejectedBefore = ServerRequestGuard.rejectedCount();

		helper.assertValueEqual(AbilityActivationHandler.handle(player, new AbilityActivationRequestPayload(-1)),
				AbilityActivationHandler.Outcome.INVALID_SLOT, "negative slot");

		// Immediately repeated request (same tick) hits the rate limit when requestCooldownTicks > 0.
		AbilityActivationHandler.Outcome spam = AbilityActivationHandler.handle(player, new AbilityActivationRequestPayload(0));
		if (GrandLineMod.config().requestCooldownTicks > 0) {
			helper.assertValueEqual(spam, AbilityActivationHandler.Outcome.REJECTED_BY_GUARD, "spammed request");
		}

		helper.assertTrue(ServerRequestGuard.rejectedCount() > rejectedBefore, "rejections are counted");
		helper.assertValueEqual(PlayerProfileService.get(player), before, "profile unchanged by client requests");

		helper.runAfterDelay(GrandLineMod.config().requestCooldownTicks + 1, () -> {
			helper.assertValueEqual(AbilityActivationHandler.handle(player, new AbilityActivationRequestPayload(0)),
					AbilityActivationHandler.Outcome.NO_ABILITY_BOUND, "valid slot after rate-limit window");
			helper.assertValueEqual(PlayerProfileService.get(player), before, "profile still unchanged");
			helper.succeed();
		});
	}
}
