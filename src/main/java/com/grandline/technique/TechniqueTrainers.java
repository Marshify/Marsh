package com.grandline.technique;

import com.grandline.player.PlayerProfileService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * What the technique trainers sell and teach: Flash Step (Flash Step Master), Aura (Aura Master), Instinct (Instinct
 * Sage), and the Movement Instructor's free Dash tutorial. Every requirement is checked before any money is taken.
 */
public final class TechniqueTrainers {
	public static final int FLASH_STEP = 0, AURA = 1, INSTINCT = 2;

	private TechniqueTrainers() {
	}

	/** Buys a technique. Returns a result key: technique_learned, technique_owned, technique_level or technique_money. */
	public static String buy(ServerPlayer player, int which) {
		var config = TechniqueService.config();
		Techniques t = TechniqueService.get(player);
		boolean owned;
		int level;
		long price;
		String id;
		switch (which) {
			case FLASH_STEP -> {
				owned = t.flashStep();
				level = config.flashStepLevel;
				price = config.flashStepPrice;
				id = "flash_step";
			}
			case AURA -> {
				owned = t.aura();
				level = 1;
				price = config.auraPrice;
				id = "aura";
			}
			case INSTINCT -> {
				owned = t.instinct();
				level = config.instinctLevel;
				price = config.instinctPrice;
				id = "instinct";
			}
			default -> {
				return "too_far";
			}
		}
		if (owned) {
			return "technique_owned";
		}
		var profile = PlayerProfileService.get(player);
		if (profile.level() < level) {
			player.displayClientMessage(Component.translatable("message.grandline.technique.need_level", level).withStyle(ChatFormatting.RED), true);
			return "technique_level";
		}
		if (profile.currency() < price) {
			return "technique_money";
		}
		PlayerProfileService.update(player, p -> p.withCurrency(p.currency() - price));
		TechniqueService.grant(player, id);
		player.sendSystemMessage(Component.translatable("message.grandline.technique.learned",
				Component.translatable("technique.grandline." + id)).withStyle(ChatFormatting.GOLD));
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
		return "technique_learned";
	}

	// ------------------------------------------------------------------------------------------------ Dash tutorial

	/** Tutorial steps: dash forward, back, left, right, and once in the air. */
	static final int STEP_FORWARD = 1, STEP_BACK = 2, STEP_LEFT = 4, STEP_RIGHT = 8, STEP_AIR = 16, ALL = 31;

	public static String startTutorial(ServerPlayer player) {
		TechniqueService.Runtime r = TechniqueService.runtime(player);
		r.tutorial = 0;
		r.inTutorial = true;
		player.sendSystemMessage(Component.translatable("message.grandline.dash_tutorial.start").withStyle(ChatFormatting.AQUA));
		player.sendSystemMessage(Component.translatable("message.grandline.dash_tutorial.steps").withStyle(ChatFormatting.GRAY));
		return "tutorial_started";
	}

	/** Called after every dash: ticks off the tutorial's steps and congratulates the player at the end. */
	static void dashed(ServerPlayer player, float forward, float strafe, boolean airborne) {
		TechniqueService.Runtime r = TechniqueService.runtime(player);
		if (!r.inTutorial) {
			return;
		}
		int before = r.tutorial;
		if (forward > 0.5f || Math.abs(forward) < 0.01f && Math.abs(strafe) < 0.01f) {
			r.tutorial |= STEP_FORWARD;
		}
		if (forward < -0.5f) {
			r.tutorial |= STEP_BACK;
		}
		if (strafe > 0.5f) {
			r.tutorial |= STEP_LEFT;
		}
		if (strafe < -0.5f) {
			r.tutorial |= STEP_RIGHT;
		}
		if (airborne) {
			r.tutorial |= STEP_AIR;
		}
		if (r.tutorial != before) {
			player.displayClientMessage(Component.translatable("message.grandline.dash_tutorial.progress", Integer.bitCount(r.tutorial), 5)
					.withStyle(ChatFormatting.AQUA), true);
		}
		if (r.tutorial == ALL) {
			r.inTutorial = false;
			player.sendSystemMessage(Component.translatable("message.grandline.dash_tutorial.done").withStyle(ChatFormatting.GREEN));
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.6f);
		}
	}
}
