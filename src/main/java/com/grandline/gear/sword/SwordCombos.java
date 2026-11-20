package com.grandline.gear.sword;

import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.Power;
import com.grandline.gear.Gear;
import com.grandline.gear.GearService;
import com.grandline.player.StatType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.grandline.gear.sword.SwordKit.layer;

/**
 * Normal-attack combos. Every swing with a sword advances its combo (three to five swings, see {@link Swords.Combo});
 * each swing draws its own blade trail on every client, and the last one is a finisher (a sweep, thrust, slam, dash...)
 * that hits with its own area on top of the normal melee hit. A pause of {@link #RESET_TICKS} starts the combo over.
 * Swings are paced by the sword's attack speed, so spamming the button cannot skip ahead.
 */
public final class SwordCombos {
	/** A pause longer than this resets the combo. */
	public static final int RESET_TICKS = 30;

	private record State(int step, long lastTick, net.minecraft.world.item.Item item) {
	}

	private static final Map<UUID, State> STATES = new HashMap<>();

	private SwordCombos() {
	}

	public static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> STATES.clear());
	}

	/** Ticks between swings for the held sword (its attack speed), with some slack for quick clicking. */
	static int interval(ServerPlayer player) {
		double speed = player.getAttributeValue(Attributes.ATTACK_SPEED);
		return (int) Math.max(3, Math.floor(20.0 / Math.max(0.5, speed) * 0.6));
	}

	/** The combo step the player is on (0-based; for tests and the HUD). */
	public static int step(ServerPlayer player) {
		State s = STATES.get(player.getUUID());
		return s == null ? 0 : s.step();
	}

	/** Handles one swing. Returns the step that was played, or -1 if the swing was ignored (too soon, no sword). */
	public static int swing(ServerPlayer player) {
		var item = player.getMainHandItem().getItem();
		Swords.Spec spec = Swords.spec(item);
		Power power = Gear.powerFor(item);
		if (spec == null || power == null || !player.isAlive() || player.isSpectator()) {
			return -1;
		}
		long now = player.serverLevel().getGameTime();
		State old = STATES.get(player.getUUID());
		if (old != null && old.item() == item && now - old.lastTick() < interval(player)) {
			return -1;
		}
		boolean fresh = old == null || old.item() != item || now - old.lastTick() > RESET_TICKS;
		int step = fresh ? 0 : (old.step() + 1) % spec.combo().steps();
		STATES.put(player.getUUID(), new State(step, now, item));

		boolean last = step == spec.combo().steps() - 1;
		int style = spec.combo().styles()[step];
		Vec3 f = SwordKit.forward(player);
		SwordKit.fx(player, "trail", spec.palette(), style, SwordKit.chest(player), f, last ? 3.6 : 3.0);
		SwordKit.sound(player, player.position(), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 0.45f, 1.25f + step * 0.08f));
		if (last) {
			finish(player, power, spec, f);
		}
		return step;
	}

	/** The finisher's damage: the sword's full melee damage (stats and gear included) times {@code factor}. */
	static float finisherDamage(ServerPlayer player, double factor) {
		double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		double gear = GearService.damageMultiplier(player, StatType.SWORD); // ctx.damage applies it again; take it out here
		return (float) (attack * factor / Math.max(0.01, gear));
	}

	private static void finish(ServerPlayer player, Power power, Swords.Spec spec, Vec3 f) {
		AbilityContext ctx = new AbilityContext(player, power, power.abilities().get(0), 1.0);
		String pal = spec.palette();
		Vec3 c = player.position();
		switch (spec.combo().finisher()) {
			case SWEEP -> {
				SwordKit.sound(player, c, layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f));
				hitAll(ctx, SwordKit.arc(player, c, f, 3.8, 85, 1, 2.6), finisherDamage(player, 1.3), c, 0.55, 0.25, pal, f);
			}
			case THRUST -> {
				SwordKit.sound(player, c, layer(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.2f));
				Vec3 from = SwordKit.chest(player);
				hitAll(ctx, SwordKit.swept(player, from, from.add(player.getLookAngle().scale(4.6)), 0.7), finisherDamage(player, 1.5), c, 0.7, 0.2, pal, f);
			}
			case CROSS -> {
				SwordKit.fx(player, "trail", pal, 2, SwordKit.chest(player), f, 3.6);
				SwordKit.sound(player, c, layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.0f), layer(SoundEvents.PLAYER_ATTACK_CRIT, 0.8f, 1.3f));
				hitAll(ctx, SwordKit.arc(player, c, f, 3.8, 55, 1, 2.6), finisherDamage(player, 1.5), c, 0.5, 0.3, pal, f);
			}
			case SLAM -> {
				Vec3 at = SwordKit.ground(player.serverLevel(), c.add(f.scale(1.8)));
				SwordKit.fx(player, "slam", pal, 0, at, f, 0, 3.0, 3.0);
				SwordKit.sound(player, at, layer(SoundEvents.ANVIL_LAND, 0.6f, 0.8f), layer(SoundEvents.GENERIC_EXPLODE, 0.3f, 1.4f));
				hitAll(ctx, SwordKit.arc(player, at, f, 3.2, 180, 1, 2), finisherDamage(player, 1.5), at, 0.45, 0.45, pal, f);
			}
			case SPIN -> {
				SwordKit.sound(player, c, layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.9f), layer(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.2f));
				hitAll(ctx, SwordKit.arc(player, c, f, 3.4, 180, 1, 2.6), finisherDamage(player, 1.3), c, 0.6, 0.25, pal, f);
			}
			case HEAVY -> {
				SwordKit.sound(player, c, layer(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.7f), layer(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f));
				hitAll(ctx, SwordKit.arc(player, c, f, 3.8, 50, 1, 2.6), finisherDamage(player, 1.6), c, 1.1, 0.35, pal, f);
			}
			case DASH, LUNGE -> {
				SwordKit.sound(player, c, layer(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.1f), layer(SoundEvents.PHANTOM_FLAP, 0.6f, 1.8f));
				float dmg = finisherDamage(player, 1.4);
				SwordMoveBase.dash(ctx, pal, 0, player.getLookAngle(), spec.combo().finisher() == Swords.Finisher.LUNGE ? 3.5 : 4.0, 3, 1.1, dmg,
						(t, at) -> {
							SwordKit.push(t, at, 0.5, 0.2);
							SwordKit.fx(player, "cut", pal, 0, t.position().add(0, t.getBbHeight() * 0.6, 0), f, 1.0);
						});
			}
		}
	}

	private static void hitAll(AbilityContext ctx, List<LivingEntity> targets, float damage, Vec3 from, double push, double lift, String pal, Vec3 f) {
		for (LivingEntity t : targets) {
			if (SwordKit.hitFresh(ctx, t, damage)) {
				SwordKit.push(t, from, push, lift);
				SwordKit.fx(ctx.player(), "cut", pal, 0, t.position().add(0, t.getBbHeight() * 0.6, 0), f, 1.0);
			}
		}
	}
}
