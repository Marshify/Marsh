package com.grandline.client.screen;

import com.grandline.GrandLineMod;
import com.grandline.client.technique.ClientTechniques;
import com.grandline.client.ui.DialogScreen;
import com.grandline.network.payload.NpcActionPayload;
import com.grandline.network.payload.OpenNpcPayload;
import com.grandline.technique.TechniqueRules;
import com.grandline.technique.Techniques;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The technique trainers' conversations (through the same dialog screen as every islander): the Movement Instructor
 * (Dash, free tutorial), the Flash Step Master, the Aura Master and the Instinct Sage. Progress shown here is the
 * server's own numbers (kept in sync on this client).
 */
final class TrainerScreens {
	private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.US);

	private TrainerScreens() {
	}

	private static net.minecraft.network.chat.MutableComponent t(String key, Object... args) {
		return Component.translatable(key, args);
	}

	private static DialogScreen.Option page(Component label, OpenNpcPayload data, String speaker, List<Component> lines) {
		return new DialogScreen.Option(label, () -> Minecraft.getInstance().setScreen(info(data, speaker, lines)), true);
	}

	/** A page of explanation with Back (to the trainer's first page) and Leave. */
	private static DialogScreen info(OpenNpcPayload data, String speaker, List<Component> lines) {
		return new DialogScreen(t(speaker), lines, List.of(
				new DialogScreen.Option(t("npc.grandline.trainer.back"), () -> NpcScreens.open(data), true),
				DialogScreen.Option.of(t("npc.grandline.trainer.leave"), () -> { })));
	}

	/** Back to the Ability Master's first page. */
	private static DialogScreen.Option backToMaster(OpenNpcPayload data) {
		return new DialogScreen.Option(t("npc.grandline.trainer.back"), () -> NpcScreens.open(data), true);
	}

	private static DialogScreen.Option leave() {
		return DialogScreen.Option.of(t("npc.grandline.trainer.leave"), () -> { });
	}

	// ------------------------------------------------------------------------------------------------ Movement Instructor

	static DialogScreen movementInstructor(OpenNpcPayload data) {
		String who = "entity.grandline.islander.movement_instructor";
		var cfg = GrandLineMod.config();
		List<DialogScreen.Option> options = new ArrayList<>();
		options.add(page(t("npc.grandline.movement.what"), data, who, List.of(
				t("npc.grandline.movement.what.1"),
				t("npc.grandline.movement.what.2", Math.round(cfg.dashDistance)))));
		options.add(page(t("npc.grandline.movement.controls"), data, who, List.of(
				t("npc.grandline.movement.controls.1"),
				t("npc.grandline.movement.controls.2"),
				t("npc.grandline.movement.controls.3"))));
		options.add(page(t("npc.grandline.movement.energy"), data, who, List.of(
				t("npc.grandline.movement.energy.1", Math.round(cfg.dashEnergy)),
				t("npc.grandline.movement.energy.2", String.format(Locale.ROOT, "%.1f", cfg.dashCooldownTicks / 20.0)),
				t("npc.grandline.movement.air"))));
		options.add(DialogScreen.Option.of(t("npc.grandline.movement.tutorial"), () -> NpcScreens.send(data, NpcActionPayload.TRAIN + 3)));
		options.add(leave());
		return new DialogScreen(t(who), List.of(t("npc.grandline.movement.greeting")), options);
	}

	// ------------------------------------------------------------------------------------------------ Flash Step Master

	/** The Ability Master on Frostpeak Isle: Flash Step and Aura, each on its own page. */
	static DialogScreen abilityMaster(OpenNpcPayload data) {
		return new DialogScreen(t("entity.grandline.islander.flash_step_master"), List.of(t("npc.grandline.ability.greeting")), List.of(
				new DialogScreen.Option(t("npc.grandline.ability.flash_step"), () -> Minecraft.getInstance().setScreen(flashStepMaster(data)), true),
				new DialogScreen.Option(t("npc.grandline.ability.aura"), () -> Minecraft.getInstance().setScreen(auraMaster(data)), true),
				leave()));
	}

	static DialogScreen flashStepMaster(OpenNpcPayload data) {
		String who = "entity.grandline.islander.flash_step_master";
		var cfg = GrandLineMod.config();
		Techniques own = ClientTechniques.own();
		List<DialogScreen.Option> options = new ArrayList<>();
		options.add(page(t("npc.grandline.flash.what"), data, who, List.of(
				t("npc.grandline.flash.what.1", Math.round(cfg.flashStepRange)),
				t("npc.grandline.flash.what.2"))));
		if (own.flashStep()) {
			options.add(new DialogScreen.Option(t("npc.grandline.trainer.owned", t("technique.grandline.flash_step")).withStyle(ChatFormatting.DARK_GRAY),
					() -> { }, true));
		} else {
			options.add(DialogScreen.Option.of(t("npc.grandline.flash.learn", NUMBERS.format(cfg.flashStepPrice), cfg.flashStepLevel),
					() -> NpcScreens.send(data, NpcActionPayload.TRAIN)));
		}
		options.add(page(t("npc.grandline.flash.how"), data, who, List.of(
				t("npc.grandline.flash.how.1"),
				t("npc.grandline.flash.how.2", String.format(Locale.ROOT, "%.0f", cfg.flashStepCooldownTicks / 20.0), Math.round(cfg.flashStepEnergy)))));
		options.add(backToMaster(data));
		options.add(leave());
		return new DialogScreen(t(who), List.of(t("npc.grandline.flash.greeting")), options);
	}

	// ------------------------------------------------------------------------------------------------ Aura Master

	static DialogScreen auraMaster(OpenNpcPayload data) {
		String who = "entity.grandline.islander.aura_master";
		var cfg = GrandLineMod.config();
		Techniques own = ClientTechniques.own();
		List<DialogScreen.Option> options = new ArrayList<>();
		options.add(page(t("npc.grandline.aura.what"), data, who, List.of(t("npc.grandline.aura.what.1"), t("npc.grandline.aura.what.2"))));
		if (own.aura()) {
			options.add(new DialogScreen.Option(t("npc.grandline.trainer.owned", t("technique.grandline.aura")).withStyle(ChatFormatting.DARK_GRAY),
					() -> { }, true));
		} else {
			options.add(DialogScreen.Option.of(t("npc.grandline.aura.learn", NUMBERS.format(cfg.auraPrice)), () -> NpcScreens.send(data, NpcActionPayload.TRAIN + 1)));
		}
		options.add(page(t("npc.grandline.aura.how"), data, who, List.of(t("npc.grandline.aura.how.1"), t("npc.grandline.aura.how.2"))));
		options.add(page(t("npc.grandline.aura.progress"), data, who, auraProgress(own)));
		options.add(backToMaster(data));
		options.add(leave());
		return new DialogScreen(t(who), List.of(t("npc.grandline.aura.greeting")), options);
	}

	static List<Component> auraProgress(Techniques own) {
		List<Component> lines = new ArrayList<>();
		if (!own.aura()) {
			lines.add(t("npc.grandline.aura.progress.none"));
			return lines;
		}
		int stage = TechniqueRules.auraStage(own.auraXp());
		long next = TechniqueRules.auraNextXp(own.auraXp());
		lines.add(t("npc.grandline.aura.progress.stage", stage, t("aura.grandline.coverage." + stage)).withStyle(ChatFormatting.AQUA));
		lines.add(next < 0 ? t("npc.grandline.aura.progress.max", NUMBERS.format(own.auraXp()))
				: t("npc.grandline.aura.progress.xp", NUMBERS.format(own.auraXp()), NUMBERS.format(next)));
		lines.add(t("npc.grandline.aura.progress.bonus", Math.round(TechniqueRules.auraDamageBonus(stage) * 100),
				Math.round(TechniqueRules.auraReduction(stage) * 100)));
		return lines;
	}

	// ------------------------------------------------------------------------------------------------ Instinct Sage

	static DialogScreen instinctSage(OpenNpcPayload data) {
		String who = "entity.grandline.islander.instinct_sage";
		var cfg = GrandLineMod.config();
		Techniques own = ClientTechniques.own();
		List<DialogScreen.Option> options = new ArrayList<>();
		options.add(page(t("npc.grandline.instinct.what"), data, who, List.of(t("npc.grandline.instinct.what.1"),
				t("npc.grandline.instinct.what.2", Math.round(cfg.instinctDetectRadius)))));
		if (own.instinct()) {
			options.add(new DialogScreen.Option(t("npc.grandline.trainer.owned", t("technique.grandline.instinct")).withStyle(ChatFormatting.DARK_GRAY),
					() -> { }, true));
		} else {
			options.add(DialogScreen.Option.of(t("npc.grandline.instinct.learn", NUMBERS.format(cfg.instinctPrice), cfg.instinctLevel),
					() -> NpcScreens.send(data, NpcActionPayload.TRAIN + 2)));
		}
		options.add(page(t("npc.grandline.instinct.how"), data, who, List.of(t("npc.grandline.instinct.how.1"),
				t("npc.grandline.instinct.how.2", cfg.instinctRegenSeconds))));
		options.add(page(t("npc.grandline.instinct.progress"), data, who, instinctProgress(own)));
		options.add(leave());
		return new DialogScreen(t(who), List.of(t("npc.grandline.instinct.greeting")), options);
	}

	static List<Component> instinctProgress(Techniques own) {
		List<Component> lines = new ArrayList<>();
		if (!own.instinct()) {
			lines.add(t("npc.grandline.instinct.progress.none"));
			return lines;
		}
		long next = TechniqueRules.instinctNextXp(own.instinctXp());
		lines.add(t("npc.grandline.instinct.progress.charges", own.dodges(), TechniqueRules.maxDodges(own.instinctXp())).withStyle(ChatFormatting.AQUA));
		lines.add(next < 0 ? t("npc.grandline.instinct.progress.max", NUMBERS.format(own.instinctXp()))
				: t("npc.grandline.instinct.progress.xp", NUMBERS.format(own.instinctXp()), NUMBERS.format(next)));
		return lines;
	}
}
