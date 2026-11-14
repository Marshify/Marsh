package com.grandline.race;

import com.grandline.combat.CombatCredit;
import com.grandline.config.RaceBalance;
import com.grandline.player.rules.HealthRegen;
import com.grandline.player.rules.OceanWeakness;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Race passives, by race and evolution stage (values in RaceBalance): walking speed (an attribute modifier), jump
 * height and swimming (hidden effects, so the client moves smoothly), damage dealt and taken, lifesteal, regeneration,
 * move energy and cooldowns, and sea weakness. Active V3 abilities and V4 awakenings add to these here, and stop the
 * moment they end: nothing is stored on the player, so nothing can stack or linger.
 */
public final class RacePassives {
	static final UUID SPEED = UUID.fromString("5c1a7e2b-3d4f-4a8b-9c0d-1e2f3a4b5c01");
	static final UUID ATTACK_SPEED = UUID.fromString("5c1a7e2b-3d4f-4a8b-9c0d-1e2f3a4b5c02");
	/** No mix of race effects ever takes more than this share of a hit away (no invulnerability). */
	static final double MAX_REDUCTION = 0.75;
	/** Lifesteal heals at most this share of the attacker's max health per hit. */
	static final double MAX_HEAL_PER_HIT = 0.06;

	private RacePassives() {
	}

	/** Night by the world clock (isDay() only updates on the next tick). */
	public static boolean isNight(net.minecraft.world.level.Level level) {
		long t = level.getDayTime() % 24000;
		return level.dimensionType().hasSkyLight() && t >= 13000 && t < 23000;
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(RacePassives::tick);
		HealthRegen.addModifier(player -> {
			// Race regeneration never cancels out sea weakness: while the sea is draining a player, only the base applies.
			if (OceanWeakness.affected(player)) {
				return 1;
			}
			RaceData d = RaceService.get(player);
			return 1 + RaceBalance.at(RaceService.balance(d.kind()).regen, d.stage());
		});
		OceanWeakness.addModifier(RacePassives::oceanMultiplier);
	}

	/** Sea weakness multiplier: Shark protection grows with evolution and its abilities; never below 15%. */
	public static double oceanMultiplier(ServerPlayer player) {
		RaceData d = RaceService.get(player);
		double m = RaceBalance.at(RaceService.balance(d.kind()).ocean, d.stage());
		if (d.kind() == Race.SHARK && RaceService.v3Active(player)) {
			m *= 0.6;
		}
		if (d.kind() == Race.SHARK && RaceService.v4Active(player)) {
			m *= 0.5;
		}
		return Math.max(0.15, m);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10 != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			apply(player);
		}
	}

	/** Refreshes the player's race modifiers and effects (every half second). */
	public static void apply(ServerPlayer player) {
		RaceData d = RaceService.get(player);
		Race race = d.kind();
		RaceBalance b = RaceService.balance(race);
		int stage = d.stage();
		double speed = RaceBalance.at(b.speed, stage);
		if (race == Race.RABBIT && RaceService.v3Active(player)) {
			speed += b.v3Power;
		}
		if (race == Race.RABBIT && RaceService.v4Active(player)) {
			speed += b.v4Power;
		}
		if (race == Race.GHOUL && (RaceService.v3Active(player) || RaceService.v4Active(player))) {
			speed += 0.25;
		}
		if (race == Race.HUMAN && RaceService.v4Active(player)) {
			speed += 0.15;
		}
		set(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED, "Race speed", speed, AttributeModifier.Operation.MULTIPLY_BASE);
		set(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED, "Race attack speed",
				race == Race.HUMAN && RaceService.v4Active(player) ? 0.3 : 0, AttributeModifier.Operation.MULTIPLY_BASE);
		// Jump: Angels and Rabbits leap higher (hidden Jump Boost; level from the configured bonus).
		double jump = RaceBalance.at(b.jump, stage) + (race == Race.ANGEL && RaceService.v4Active(player) ? 0.3 : 0)
				+ (race == Race.RABBIT && (RaceService.v3Active(player) || RaceService.v4Active(player)) ? 0.2 : 0);
		if (jump > 0.05) {
			hidden(player, MobEffects.JUMP, (int) Math.min(3, Math.floor(jump / 0.2)));
		}
		// Angels glide down from V2 (and always while awakened).
		if (race == Race.ANGEL && (stage >= 2 || RaceService.v4Active(player)) && player.fallDistance > 3 && !player.isShiftKeyDown()) {
			hidden(player, MobEffects.SLOW_FALLING, 0);
		}
		// Sharks swim fast (Dolphin's Grace while in water; Conduit Power from V2 to breathe and see).
		if (race == Race.SHARK && player.isInWater()) {
			double swim = RaceBalance.at(b.swim, stage);
			if (swim > 0) {
				hidden(player, MobEffects.DOLPHINS_GRACE, 0);
			}
			if (stage >= 2) {
				hidden(player, MobEffects.CONDUIT_POWER, 0);
			}
		}
		// Ghouls see in the dark from V2.
		if (race == Race.GHOUL && stage >= 2 && isNight(player.level())) {
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
		}
	}

	private static void hidden(ServerPlayer player, net.minecraft.world.effect.MobEffect effect, int amplifier) {
		player.addEffect(new MobEffectInstance(effect, 25, amplifier, true, false, false));
	}

	private static void set(AttributeInstance attr, UUID id, String name, double amount, AttributeModifier.Operation op) {
		if (attr == null) {
			return;
		}
		AttributeModifier existing = attr.getModifier(id);
		if (existing != null && existing.getAmount() == amount) {
			return;
		}
		attr.removeModifier(id);
		if (amount != 0) {
			attr.addTransientModifier(new AttributeModifier(id, name, amount, op));
		}
	}

	// ---- combat hooks (from RaceDamageMixin) ----

	/** Adjusts a hit for the attacker's and the victim's races. */
	public static float modifyDamage(LivingEntity victim, DamageSource source, float amount) {
		if (victim.level().isClientSide || amount <= 0) {
			return amount;
		}
		double out = amount;
		ServerPlayer attacker = CombatCredit.playerBehind(source);
		if (attacker != null && attacker != victim) {
			RaceData d = RaceService.get(attacker);
			Race race = d.kind();
			RaceBalance b = RaceService.balance(race);
			double bonus = RaceBalance.at(b.damage, d.stage());
			boolean night = isNight(attacker.level());
			if (night) {
				bonus += RaceBalance.at(b.night, d.stage());
			}
			if (race == Race.HUMAN && RaceService.v3Active(attacker)) {
				bonus += b.v3Power * (1 - attacker.getHealth() / attacker.getMaxHealth()); // Last Stand: stronger when hurt
			}
			if (race == Race.HUMAN && RaceService.v4Active(attacker)) {
				bonus += b.v4Power + RaceUpgrades.bonus(attacker, "damage");
			}
			bonus += RaceUpgrades.bonus(attacker, "always_damage");
			out *= 1 + bonus;
		}
		if (victim instanceof ServerPlayer player && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
				&& !source.is(OceanWeakness.DAMAGE)) {
			RaceData d = RaceService.get(player);
			Race race = d.kind();
			RaceBalance b = RaceService.balance(race);
			double red = RaceBalance.at(b.reduction, d.stage());
			if (race == Race.SHARK && RaceService.v3Active(player)) {
				red += b.v3Power;
			}
			if (race == Race.SHARK && RaceService.v4Active(player)) {
				red += b.v4Power + RaceUpgrades.bonus(player, "reduction");
			}
			if (race == Race.CYBORG && (RaceService.v3Active(player) || RaceService.v4Active(player))) {
				red += 0.2 + RaceUpgrades.bonus(player, "reduction");
			}
			if (race == Race.ANGEL && RaceService.v4Active(player)) {
				red += 0.15 + RaceUpgrades.bonus(player, "reduction");
			}
			out *= 1 - Math.min(MAX_REDUCTION, red);
		}
		return (float) out;
	}

	/** Lifesteal share of damage dealt for this attacker right now (0 if none). */
	public static double lifesteal(ServerPlayer attacker) {
		RaceData d = RaceService.get(attacker);
		Race race = d.kind();
		RaceBalance b = RaceService.balance(race);
		double ls = RaceBalance.at(b.lifesteal, d.stage());
		if (race == Race.GHOUL && d.stage() == 1 && !isNight(attacker.level())) {
			ls = 0; // V1 Ghouls only feed at night
		}
		if (race == Race.GHOUL && RaceService.v3Active(attacker)) {
			ls += b.v3Power;
		}
		if (race == Race.GHOUL && RaceService.v4Active(attacker)) {
			ls += b.v4Power + RaceUpgrades.bonus(attacker, "lifesteal");
		}
		return ls;
	}

	/** After a hit landed: the attacker's lifesteal (capped per hit). Blocked or cancelled hits heal nothing. */
	public static void afterHurt(LivingEntity victim, DamageSource source, float dealt, boolean landed) {
		if (!landed || victim.level().isClientSide || dealt <= 0) {
			return;
		}
		ServerPlayer attacker = CombatCredit.playerBehind(source);
		if (attacker == null || attacker == victim || !attacker.isAlive()) {
			return;
		}
		double ls = lifesteal(attacker);
		float heal = (float) Math.min(dealt * ls, attacker.getMaxHealth() * MAX_HEAL_PER_HIT);
		if (heal > 0) {
			attacker.heal(heal);
		}
		RaceAwakening.onDealt(attacker, dealt);
	}

	/** Move energy cost multiplier (Angel, Cyborg; Cyborg Overdrive). */
	public static double energyMultiplier(ServerPlayer player) {
		RaceData d = RaceService.get(player);
		double cut = RaceBalance.at(RaceService.balance(d.kind()).energy, d.stage());
		if (d.kind() == Race.CYBORG && RaceService.v4Active(player)) {
			cut += 0.3 + RaceUpgrades.bonus(player, "energy");
		}
		return Math.max(0.3, 1 - cut);
	}

	/** Move cooldown multiplier (Ghoul; Heightened Senses and Blood Eclipse). */
	public static double cooldownMultiplier(ServerPlayer player) {
		RaceData d = RaceService.get(player);
		RaceBalance b = RaceService.balance(d.kind());
		double cut = RaceBalance.at(b.cooldown, d.stage());
		if (d.kind() == Race.GHOUL && RaceService.v3Active(player)) {
			cut += b.v3Power;
		}
		if (d.kind() == Race.GHOUL && RaceService.v4Active(player)) {
			cut += 0.2 + RaceUpgrades.bonus(player, "cooldown");
		}
		return Math.max(0.4, 1 - cut);
	}
}
