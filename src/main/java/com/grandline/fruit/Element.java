package com.grandline.fruit;

import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityHelpers;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

/** Elemental flavour shared by a fruit's moves: particles, sound and an on-hit effect. */
public enum Element {
	WIND(ParticleTypes.CLOUD, SoundEvents.PHANTOM_FLAP),
	SAND(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), SoundEvents.SAND_BREAK),
	FIRE(ParticleTypes.FLAME, SoundEvents.FIRECHARGE_USE),
	ICE(ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK),
	LAVA(ParticleTypes.LAVA, SoundEvents.LAVA_POP),
	LIGHTNING(ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_IMPACT),
	WATER(ParticleTypes.SPLASH, SoundEvents.GENERIC_SPLASH),
	EXPLOSION(ParticleTypes.EXPLOSION, SoundEvents.GENERIC_EXPLODE),
	POISON(ParticleTypes.ITEM_SLIME, SoundEvents.SLIME_SQUISH),
	PLANT(ParticleTypes.HAPPY_VILLAGER, SoundEvents.GRASS_BREAK),
	SOUND(ParticleTypes.NOTE, SoundEvents.BELL_BLOCK),
	LIGHT(ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE),
	SHADOW(ParticleTypes.SQUID_INK, SoundEvents.WARDEN_SONIC_BOOM),
	GRAVITY(ParticleTypes.PORTAL, SoundEvents.END_PORTAL_FRAME_FILL),
	BEAST(ParticleTypes.SWEEP_ATTACK, SoundEvents.WOLF_GROWL),
	METAL(ParticleTypes.CRIT, SoundEvents.ANVIL_LAND),
	CRYSTAL(ParticleTypes.ENCHANTED_HIT, SoundEvents.AMETHYST_BLOCK_CHIME),
	MIST(ParticleTypes.POOF, SoundEvents.FIRE_EXTINGUISH),
	BOUNCE(ParticleTypes.ITEM_SLIME, SoundEvents.SLIME_JUMP),
	BARRIER(ParticleTypes.TOTEM_OF_UNDYING, SoundEvents.BEACON_POWER_SELECT),
	EARTH(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.DIRT.defaultBlockState()), SoundEvents.GRAVEL_BREAK);

	private final ParticleOptions particle;
	private final SoundEvent sound;

	Element(ParticleOptions particle, SoundEvent sound) {
		this.particle = particle;
		this.sound = sound;
	}

	public ParticleOptions particle() {
		return particle;
	}

	public SoundEvent sound() {
		return sound;
	}

	/** Applies the element's side effect to a target that was hit. */
	public void onHit(AbilityContext ctx, LivingEntity target) {
		switch (this) {
			case WIND -> AbilityHelpers.knockAway(target, ctx.player().position(), 1.1, 0.35);
			case SAND -> {
				target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), ctx.player());
				target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), ctx.player());
			}
			case FIRE -> target.setSecondsOnFire(4);
			case ICE -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), ctx.player());
			case LAVA -> target.setSecondsOnFire(6);
			case WATER -> {
				AbilityHelpers.knockAway(target, ctx.player().position(), 0.7, 0.2);
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), ctx.player());
			}
			case EXPLOSION -> AbilityHelpers.knockAway(target, ctx.player().position(), 1.0, 0.5);
			case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), ctx.player());
			case PLANT -> {
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4), ctx.player());
				target.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), ctx.player());
			}
			case SOUND -> {
				target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0), ctx.player());
				AbilityHelpers.knockAway(target, ctx.player().position(), 0.5, 0.1);
			}
			case LIGHT -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), ctx.player());
			case SHADOW -> {
				target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0), ctx.player());
				target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), ctx.player());
			}
			case GRAVITY -> {
				// Slam the target to the ground and pin it.
				target.setDeltaMovement(target.getDeltaMovement().x * 0.2, -1.0, target.getDeltaMovement().z * 0.2);
				target.hurtMarked = true;
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3), ctx.player());
			}
			case BEAST -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0), ctx.player());
			case METAL -> AbilityHelpers.knockAway(target, ctx.player().position(), 0.8, 0.2);
			case CRYSTAL -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), ctx.player());
			case MIST -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), ctx.player());
			case BOUNCE -> {
				target.setDeltaMovement(target.getDeltaMovement().add(0, 0.9, 0));
				target.hurtMarked = true;
			}
			case BARRIER -> AbilityHelpers.knockAway(target, ctx.player().position(), 0.9, 0.3);
			case EARTH -> {
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), ctx.player());
				target.setDeltaMovement(target.getDeltaMovement().add(0, 0.35, 0));
				target.hurtMarked = true;
			}
			case LIGHTNING -> {
				// Chain to up to two nearby enemies for half damage.
				int chained = 0;
				for (LivingEntity other : AbilityHelpers.inRadius(ctx.player(), target.position(), 4.0)) {
					if (other != target && chained < 2 && ctx.damage(other, 3f)) {
						AbilityHelpers.burst(ctx.player(), particle, other.getBoundingBox().getCenter(), 0.5, 8);
						chained++;
					}
				}
			}
		}
	}
}
