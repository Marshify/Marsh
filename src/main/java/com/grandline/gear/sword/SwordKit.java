package com.grandline.gear.sword;

import com.grandline.combat.ability.AbilityContext;
import com.grandline.combat.ability.AbilityHelpers;
import com.grandline.combat.fx.FxEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Shared building blocks for sword moves: visual beats, layered sounds, hit shapes (swept capsules, arcs, spheres),
 * fresh hits for multi-hit moves, pulls and pushes. Every beat is named {@code sw:<shape>:<palette>:<variant>}; the
 * client's SwordFx draws the shape in the weapon's colours.
 */
public final class SwordKit {
	private SwordKit() {
	}

	// ------------------------------------------------------------------------------------------------ beats

	public static void fx(ServerPlayer player, String shape, String palette, int variant, Vec3 pos, Vec3 dir, double speed, double range, double size) {
		FxEvents.send(player, "sw:" + shape + ":" + palette + ":" + variant, pos, dir, speed, range, size);
	}

	public static void fx(ServerPlayer player, String shape, String palette, int variant, Vec3 pos, Vec3 dir, double size) {
		fx(player, shape, palette, variant, pos, dir, 0, 0, size);
	}

	// ------------------------------------------------------------------------------------------------ sounds

	/** One layer of a move's sound: a sound, its volume, pitch and delay in ticks. */
	public record Layer(SoundEvent sound, float volume, float pitch, int delay) {
	}

	public static Layer layer(SoundEvent sound, float volume, float pitch) {
		return new Layer(sound, volume, pitch, 0);
	}

	public static Layer layer(Holder.Reference<SoundEvent> sound, float volume, float pitch) {
		return new Layer(sound.value(), volume, pitch, 0);
	}

	public static Layer later(SoundEvent sound, float volume, float pitch, int delay) {
		return new Layer(sound, volume, pitch, delay);
	}

	public static Layer later(Holder.Reference<SoundEvent> sound, float volume, float pitch, int delay) {
		return new Layer(sound.value(), volume, pitch, delay);
	}

	/** Plays a layered cue at {@code at} (heard by everyone near, the caster included). */
	public static void sound(ServerPlayer player, Vec3 at, Layer... layers) {
		ServerLevel level = player.serverLevel();
		for (Layer l : layers) {
			if (l.delay() <= 0) {
				level.playSound(null, at.x, at.y, at.z, l.sound(), SoundSource.PLAYERS, l.volume(), l.pitch());
			} else {
				com.grandline.combat.ability.TimedEffects.later(l.delay(),
						() -> level.playSound(null, at.x, at.y, at.z, l.sound(), SoundSource.PLAYERS, l.volume(), l.pitch()));
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ geometry

	/** The look direction flattened to the ground. */
	public static Vec3 flat(Vec3 look) {
		Vec3 f = new Vec3(look.x, 0, look.z);
		return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
	}

	public static Vec3 forward(ServerPlayer player) {
		return flat(player.getLookAngle());
	}

	public static Vec3 right(Vec3 f) {
		return new Vec3(-f.z, 0, f.x);
	}

	/** Chest height in front of the player. */
	public static Vec3 chest(ServerPlayer player) {
		return player.position().add(0, 1.15, 0);
	}

	public static boolean solid(ServerLevel level, Vec3 at) {
		BlockPos pos = BlockPos.containing(at);
		return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	/** The ground under {@code at} (the top of the first solid block within 6 blocks down), or {@code at} itself. */
	public static Vec3 ground(ServerLevel level, Vec3 at) {
		BlockPos start = BlockPos.containing(at.add(0, 0.5, 0));
		for (int dy = 0; dy <= 7; dy++) {
			BlockPos pos = start.below(dy);
			var shape = level.getBlockState(pos).getCollisionShape(level, pos);
			if (!shape.isEmpty()) {
				return new Vec3(at.x, pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y), at.z);
			}
		}
		return at;
	}

	/** The farthest point along {@code dir} from {@code from} the caster can reach without passing through a wall. */
	public static Vec3 reach(ServerPlayer player, Vec3 from, Vec3 dir, double distance) {
		Vec3 to = from.add(dir.scale(distance));
		BlockHitResult hit = player.serverLevel().clip(new ClipContext(from.add(0, 0.6, 0), to.add(0, 0.6, 0), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, player));
		if (hit.getType() == HitResult.Type.MISS) {
			return to;
		}
		double d = Math.max(0, hit.getLocation().subtract(0, 0.6, 0).distanceTo(from) - 0.6);
		return from.add(dir.scale(d));
	}

	// ------------------------------------------------------------------------------------------------ hit shapes

	/** Living things whose box (grown by {@code radius}) the segment {@code from → to} passes through. */
	public static List<LivingEntity> swept(ServerPlayer player, Vec3 from, Vec3 to, double radius) {
		List<LivingEntity> out = new ArrayList<>();
		AABB area = new AABB(from, to).inflate(radius + 1.0);
		for (LivingEntity e : AbilityHelpers.inRadius(player, from.add(to).scale(0.5), from.distanceTo(to) / 2 + radius + 2.5)) {
			AABB box = e.getBoundingBox().inflate(radius);
			if (area.intersects(box) && (box.contains(from) || box.contains(to) || box.clip(from, to).isPresent())) {
				out.add(e);
			}
		}
		return out;
	}

	/**
	 * Living things in a horizontal arc in front of {@code center}: within {@code radius}, inside {@code halfAngle}
	 * degrees of {@code forward}, and between {@code below} and {@code above} blocks of height.
	 */
	public static List<LivingEntity> arc(ServerPlayer player, Vec3 center, Vec3 forward, double radius, double halfAngle, double below, double above) {
		double minDot = Math.cos(Math.toRadians(halfAngle));
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : AbilityHelpers.inRadius(player, center, radius + 1.5)) {
			Vec3 to = e.position().subtract(center);
			double dy = to.y;
			Vec3 flatTo = new Vec3(to.x, 0, to.z);
			double d = flatTo.length() - e.getBbWidth() / 2;
			if (d > radius || dy < -below - e.getBbHeight() || dy > above) {
				continue;
			}
			if (halfAngle >= 180 || flatTo.length() < 0.8 || flatTo.normalize().dot(forward) >= minDot) {
				out.add(e);
			}
		}
		return out;
	}

	/** Living things inside a sphere. */
	public static List<LivingEntity> sphere(ServerPlayer player, Vec3 center, double radius) {
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : AbilityHelpers.inRadius(player, center, radius + 2)) {
			Vec3 closest = new Vec3(
					clamp(center.x, e.getBoundingBox().minX, e.getBoundingBox().maxX),
					clamp(center.y, e.getBoundingBox().minY, e.getBoundingBox().maxY),
					clamp(center.z, e.getBoundingBox().minZ, e.getBoundingBox().maxZ));
			if (closest.distanceToSqr(center) <= radius * radius) {
				out.add(e);
			}
		}
		return out;
	}

	private static double clamp(double v, double lo, double hi) {
		return v < lo ? lo : Math.min(v, hi);
	}

	// ------------------------------------------------------------------------------------------------ hits

	/** A normal hit (subject to the target's hurt cooldown). */
	public static boolean hit(AbilityContext ctx, LivingEntity target, float damage) {
		return ctx.damage(target, damage);
	}

	/**
	 * One hit of a multi-hit move: lands even if the target was struck a moment ago (each hit of a barrage counts), with
	 * the target's hurt flash and sound as normal.
	 */
	public static boolean hitFresh(AbilityContext ctx, LivingEntity target, float damage) {
		target.invulnerableTime = 0;
		return ctx.damage(target, damage);
	}

	/** Hits every target once per move, tracked by {@code struck}. Returns those hit this call. */
	public static List<LivingEntity> hitOnce(AbilityContext ctx, List<LivingEntity> targets, Set<Integer> struck, float damage) {
		List<LivingEntity> landed = new ArrayList<>();
		for (LivingEntity t : targets) {
			if (struck.add(t.getId()) && hitFresh(ctx, t, damage)) {
				landed.add(t);
			}
		}
		return landed;
	}

	/** Sets {@code target}'s motion (a launch, a push), synced to clients. */
	public static void launch(LivingEntity target, Vec3 velocity) {
		if (com.grandline.technique.Dodges.justEvaded(target)) {
			return; // Instinct evaded the hit: nothing else lands either
		}
		target.setDeltaMovement(velocity);
		target.hurtMarked = true;
		target.fallDistance = 0;
	}

	/** Pushes {@code target} away from {@code from} horizontally with some lift. */
	public static void push(LivingEntity target, Vec3 from, double strength, double lift) {
		if (com.grandline.technique.Dodges.justEvaded(target)) {
			return; // Instinct evaded the hit: nothing else lands either
		}
		AbilityHelpers.knockAway(target, from, strength, lift);
	}

	/** Pulls {@code target} toward {@code to}: its motion becomes a step of {@code strength} toward the point. */
	public static void pull(LivingEntity target, Vec3 to, double strength) {
		if (com.grandline.technique.Dodges.justEvaded(target)) {
			return; // Instinct evaded the hit: nothing else lands either
		}
		Vec3 d = to.subtract(target.position());
		double len = d.length();
		if (len < 0.3) {
			launch(target, new Vec3(0, target.getDeltaMovement().y * 0.5, 0));
			return;
		}
		Vec3 v = d.scale(Math.min(strength, len * 0.6) / len);
		launch(target, new Vec3(v.x, Math.max(v.y, 0.04), v.z));
	}

	/** Moves {@code target} to {@code at} (carried by a vortex or a projectile), keeping it out of walls. */
	public static void carry(LivingEntity target, Vec3 at) {
		if (com.grandline.technique.Dodges.justEvaded(target)) {
			return; // Instinct evaded the hit: nothing else lands either
		}
		ServerLevel level = (ServerLevel) target.level();
		if (solid(level, at) || solid(level, at.add(0, target.getBbHeight() * 0.9, 0))) {
			return;
		}
		Vec3 v = at.subtract(target.position());
		target.setDeltaMovement(v.scale(0.8));
		target.hurtMarked = true;
		target.fallDistance = 0;
	}

	/** Moves the caster along with a dash: sets their motion toward {@code to} and resets the fall. */
	public static void steer(ServerPlayer player, Vec3 to, double maxStep) {
		Vec3 d = to.subtract(player.position());
		double len = d.length();
		Vec3 v = len < 1e-3 ? Vec3.ZERO : d.scale(Math.min(maxStep, len) / len);
		player.setDeltaMovement(v.x, Math.max(v.y, -0.05), v.z);
		player.hurtMarked = true;
		player.fallDistance = 0;
	}

	public static boolean gone(ServerPlayer player) {
		return !player.isAlive() || player.isRemoved();
	}
}
