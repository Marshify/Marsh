package com.grandline.combat.style;

import com.grandline.GrandLineMod;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.AbilityHelpers;
import com.grandline.combat.ability.Power;
import com.grandline.combat.ability.PowerSource;
import com.grandline.combat.ability.Powers;
import com.grandline.player.StatType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Fighting styles. Their moves scale with the Melee stat. */
public final class FightingStyles {
	/** Starter style every player has: fast, close-range brawling. */
	public static final Power BRAWLER = Powers.register(new Power(GrandLineMod.id("brawler"), PowerSource.STYLE, StatType.MELEE,
			"style", 0xE8A33D, List.of(
			// Z: quick strike on the closest enemy in front.
			new AbilityDef("brawler_jab", 0, 40, 0.06, ctx -> {
				LivingEntity target = AbilityHelpers.firstInFront(ctx.player(), 4.0);
				AbilityHelpers.sound(ctx.player(), SoundEvents.PLAYER_ATTACK_STRONG, 1.2F);
				if (target == null || !ctx.damage(target, 6f)) {
					return 0;
				}
				AbilityHelpers.knockAway(target, ctx.player().position(), 0.5, 0.15);
				AbilityHelpers.burst(ctx.player(), ParticleTypes.CRIT, target.getBoundingBox().getCenter(), 0.6, 12);
				return 1;
			}),
			// X: dash forward, hitting everything along the way.
			new AbilityDef("brawler_rush", 10, 120, 0.12, ctx -> {
				List<LivingEntity> targets = AbilityHelpers.alongLine(ctx.player(), 7.0, 1.6);
				AbilityHelpers.dash(ctx.player(), 1.8, 0.2);
				AbilityHelpers.particleLine(ctx.player(), ParticleTypes.CLOUD, 7.0);
				AbilityHelpers.sound(ctx.player(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F);
				int hits = 0;
				for (LivingEntity target : targets) {
					if (ctx.damage(target, 8f)) {
						AbilityHelpers.knockAway(target, ctx.player().position(), 0.8, 0.2);
						hits++;
					}
				}
				return hits;
			}),
			// C: slam the ground, damaging and launching everything nearby.
			new AbilityDef("brawler_slam", 25, 200, 0.2, ctx -> {
				Vec3 center = ctx.player().position();
				AbilityHelpers.burst(ctx.player(), ParticleTypes.EXPLOSION, center, 4.0, 10);
				AbilityHelpers.sound(ctx.player(), SoundEvents.GENERIC_EXPLODE, 1.3F);
				int hits = 0;
				for (LivingEntity target : AbilityHelpers.inRadius(ctx.player(), center, 4.5)) {
					if (ctx.damage(target, 10f)) {
						AbilityHelpers.knockAway(target, center, 0.6, 0.6);
						hits++;
					}
				}
				return hits;
			}),
			// V: a heavy uppercut that launches one target into the air.
			new AbilityDef("brawler_uppercut", 50, 320, 0.3, ctx -> {
				LivingEntity target = AbilityHelpers.firstInFront(ctx.player(), 5.0);
				AbilityHelpers.sound(ctx.player(), SoundEvents.ANVIL_LAND, 0.6F);
				if (target == null || !ctx.damage(target, 18f)) {
					return 0;
				}
				target.setDeltaMovement(target.getDeltaMovement().add(0, 1.2, 0));
				target.hurtMarked = true;
				AbilityHelpers.burst(ctx.player(), ParticleTypes.SWEEP_ATTACK, target.getBoundingBox().getCenter(), 1.0, 6);
				return 1;
			})
	)));

	private FightingStyles() {
	}

	public static void register() {
		GrandLineMod.LOGGER.debug("Registered fighting style {}", BRAWLER.id());
	}
}
