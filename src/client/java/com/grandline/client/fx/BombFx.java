package com.grandline.client.fx;

import com.grandline.audio.GrandLineSounds;
import com.grandline.client.audio.GrandLineAudio;
import com.grandline.fruit.bomb.BombMoves;
import com.grandline.network.payload.FxEventPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Bomb Fruit visuals and sounds from the server's beats (see BombMoves): the procedural bomb and mine models
 * ({@link BombModel}), the layered explosions ({@link ExplosionFx}), the held/thrown bomb, the world-space targeting
 * indicator, bombs attached to enemies, deployed mines, the Self Destruct bomb head (the real head is hidden only while
 * it is shown) and Explosive Jump's HUD warning. Cosmetic only: everything gameplay is server-side.
 */
public final class BombFx {
	/** Charging bombs in hand: caster id → {game time the charge started, max charge ticks}. */
	private static final Map<Integer, long[]> HAND = new HashMap<>();
	/** Self Destruct bomb heads: caster id → state. */
	private static final Map<Integer, Head> HEADS = new HashMap<>();
	private static final Map<Integer, Thrown> THROWN = new HashMap<>();
	private static final Map<Integer, Target> TARGETS = new HashMap<>();
	private static final Map<Integer, Attached> ATTACHED = new HashMap<>();
	private static final Map<Integer, Mine> MINES = new HashMap<>();
	/** Explosion sound concurrency: game time of the last few explosion sounds. */
	private static long soundTick;
	private static int soundsThisTick;
	private static long jumpSafeAt;

	private BombFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("bomb_");
	}

	static long now() {
		ClientLevel l = Minecraft.getInstance().level;
		return l == null ? 0 : l.getGameTime();
	}

	/** Explosive Jump would cost health right now (its HUD name turns red). */
	public static boolean jumpWarning() {
		return now() < jumpSafeAt;
	}

	/** Whether the Self Destruct bomb head replaces this player's head now. */
	public static boolean headActive(int entity) {
		Head h = HEADS.get(entity);
		return h != null && now() - h.updated < 10;
	}

	static void clearAll() {
		HAND.clear();
		HEADS.clear();
		THROWN.clear();
		TARGETS.clear();
		ATTACHED.clear();
		MINES.clear();
		jumpSafeAt = 0;
	}

	/** Explosion sound with a concurrency limit (at most 3 per tick, later ones quieter). */
	static void boom(GrandLineSounds.Cue cue, Vec3 at, float volume, float pitch) {
		long t = now();
		if (t != soundTick) {
			soundTick = t;
			soundsThisTick = 0;
		}
		if (++soundsThisTick > 3) {
			return;
		}
		GrandLineAudio.play(cue, at, volume / soundsThisTick, pitch);
	}

	static void handle(FxEventPayload p, ClientLevel level, Entity caster, boolean own) {
		int id = p.caster();
		float age = caster == null ? 0 : caster.tickCount;
		RandomSource r = level.random;
		switch (p.effect()) {
			case "bomb_toss_hold" -> {
				BodyMotion.bomb(id, age, 0);
				HAND.put(id, new long[]{now(), (long) p.range()});
				GrandLineAudio.play(GrandLineSounds.BOMB_IGNITE, p.pos().add(0, 1.5, 0), 0.8f, 1f);
				if (caster != null) {
					GrandLineAudio.loop(GrandLineSounds.BOMB_FUSE_LOOP, () -> caster.position().add(0, 1.4, 0), () -> HAND.containsKey(id) && caster.isAlive(),
							0.5f, () -> 1.0, null);
				}
			}
			case "bomb_toss" -> {
				HAND.remove(id);
				BodyMotion.release(id, age);
				Thrown t = new Thrown(id, p.pos(), p.dir(), (float) p.size(), (float) p.speed());
				THROWN.put((int) p.range(), t);
				GrandLineFx.add(t);
				GrandLineAudio.play(GrandLineSounds.BOMB_THROW, p.pos(), 0.9f, 1.05f - 0.15f * (float) p.speed());
			}
			case "bomb_toss_hit" -> {
				Thrown t = THROWN.remove((int) p.range());
				if (t != null) {
					t.done = true;
				}
				HAND.remove(id);
				if (p.size() > 0) {
					float c = (float) p.speed();
					ExplosionFx.spawn(level, p.pos(), p.dir(), new ExplosionFx.Spec(ExplosionFx.Shape.RADIAL, p.size(), 0.3f + 0.45f * c, 1 + (c > 0.5 ? 1 : 0),
							true, false, 0.6f + 1.2f * c));
					boom(c > 0.5 ? GrandLineSounds.BOMB_EXPLODE_HEAVY : GrandLineSounds.BOMB_EXPLODE, p.pos(), 1f, 0.95f + r.nextFloat() * 0.1f);
				}
			}
			case "bomb_target_start" -> {
				BodyMotion.bomb(id, age, 1);
				Target old = TARGETS.remove(id);
				if (old != null) {
					old.done = true;
				}
				Target t = new Target(id, p.pos(), p.range());
				TARGETS.put(id, t);
				GrandLineFx.add(t);
				GrandLineAudio.loop(GrandLineSounds.BOMB_TARGET_CHARGE, () -> t.shown, () -> !t.done && !t.released, 0.7f, () -> 0.9 + 0.3 * t.charge, null);
			}
			case "bomb_target" -> {
				Target t = TARGETS.get(id);
				if (t != null) {
					t.goal = p.pos();
					t.charge = (float) p.speed();
					t.heard = 0;
				}
			}
			case "bomb_target_end" -> {
				BodyMotion.release(id, age);
				Target t = TARGETS.remove(id);
				if (t != null) {
					t.goal = p.pos();
					t.released = true;
				}
			}
			case "bomb_target_blast" -> {
				ExplosionFx.spawn(level, p.pos(), new Vec3(0, 1, 0), new ExplosionFx.Spec(ExplosionFx.Shape.VERTICAL, p.size(), 0.7f, 2, true, false, 1.4f));
				GrandLineFx.add(new PhysicsSparks(level, p.pos(), new Vec3(0, 1, 0), 10, 0.35, 0.7, 20, 0.07, ExplosionFx.CASING, 0x1E2024, false));
				GrandLineFx.add(new BladeFx.Flash(p.pos().add(0, 1, 0), 0.9, 2));
				boom(GrandLineSounds.BOMB_TARGET_BLAST, p.pos(), 1f, 0.95f + r.nextFloat() * 0.1f);
			}
			case "bomb_grab_launch" -> {
				BodyMotion.bomb(id, age, 2);
				Vec3 back = p.pos().subtract(p.dir().scale(0.9));
				ExplosionFx.spawn(level, back, p.dir().scale(-1), new ExplosionFx.Spec(ExplosionFx.Shape.DIRECTIONAL, 1.8, 0.35f, 2, false, false, own ? 0.8f : 0.4f));
				if (caster != null) {
					GrandLineFx.add(new SpringFx.Motion(id, p.speed(), 12));
				}
				GrandLineAudio.play(GrandLineSounds.BOMB_GRAB_LAUNCH, p.pos(), 1f, 1f);
			}
			case "bomb_grab_glance" -> {
				ExplosionFx.spawn(level, p.pos(), p.dir(), new ExplosionFx.Spec(ExplosionFx.Shape.RADIAL, 1.1, 0.12f, 1, false, false, 0));
				boom(GrandLineSounds.BOMB_EXPLODE, p.pos(), 0.5f, 1.3f);
			}
			case "bomb_grab_hit" -> {
				BodyMotion.release(id, age);
				ExplosionFx.spawn(level, p.pos(), p.dir(), new ExplosionFx.Spec(ExplosionFx.Shape.RADIAL, 1.4, 0.25f, 1, false, false, own ? 0.7f : 0.3f));
				GrandLineFx.add(new BladeFx.Flash(p.pos(), 0.6, 2));
				GrandLineAudio.play(GrandLineSounds.BOMB_GRAB_ATTACH, p.pos(), 1f, 1f);
			}
			case "bomb_attach" -> {
				int target = (int) p.range();
				Attached old = ATTACHED.remove(target);
				if (old != null) {
					old.done = true;
				}
				Attached a = new Attached(target, (int) p.speed(), (float) p.size());
				ATTACHED.put(target, a);
				GrandLineFx.add(a);
				Entity e = level.getEntity(target);
				if (e != null) {
					GrandLineAudio.loop(GrandLineSounds.BOMB_FUSE_LOOP, () -> e.position().add(0, e.getBbHeight(), 0), () -> !a.done && e.isAlive(), 0.55f,
							() -> 1.0 + 0.3 * a.progress(), null);
				}
			}
			case "bomb_attach_blast", "bomb_attach_lost" -> {
				Attached a = ATTACHED.remove((int) p.range());
				if (a != null) {
					a.done = true;
				}
				if (p.effect().equals("bomb_attach_blast")) {
					ExplosionFx.spawn(level, p.pos(), p.dir(), new ExplosionFx.Spec(ExplosionFx.Shape.DIRECTIONAL, p.size(), 0.6f, 3, false, false, 1.2f));
					GrandLineFx.add(new PhysicsSparks(level, p.pos(), new Vec3(0, 0.6, 0), 12, 0.9, 0.45, 20, 0.07, ExplosionFx.CASING, 0x1E2024, false));
					GrandLineFx.add(new BladeFx.Flash(p.pos(), 0.8, 2));
					boom(GrandLineSounds.BOMB_DELAYED_BLAST, p.pos(), 1f, 1f);
				} else {
					FxSprites.smoke(p.pos(), new Vec3(0, 0.04, 0), 0.5, 20, ExplosionFx.SMOKE);
				}
			}
			case "bomb_grab_end", "bomb_jump_land" -> {
				BodyMotion.stop(id, BodyMotion.Kind.BOMB, age);
				if (p.effect().equals("bomb_jump_land")) {
					SpringFx.ground(level, p.pos(), 1.2, 5);
				}
			}
			case "bomb_mines_hold" -> BodyMotion.bomb(id, age, 4);
			case "bomb_mines_deploy" -> {
				BodyMotion.release(id, age);
				GrandLineAudio.play(GrandLineSounds.BOMB_MINE_DEPLOY, p.pos(), 1f, 1f);
			}
			case "bomb_ring" -> {
				BodyMotion.bomb(id, age, 3);
				int n = (int) p.speed();
				for (int i = 0; i < n; i++) {
					double a = p.range() + i * Math.PI * 2 / n;
					Vec3 land = p.pos().add(Math.cos(a) * p.size(), -0.6, Math.sin(a) * p.size());
					GrandLineFx.add(new Lob(id, p.pos(), land, 6, 7 + i * 2));
				}
				GrandLineAudio.play(GrandLineSounds.BOMB_MINE_DEPLOY, p.pos(), 1f, 1.15f);
			}
			case "bomb_mine" -> {
				int trap = (int) p.range();
				Mine m = new Mine(trap, id, own, p.pos(), p.dir(), (int) p.speed(), (int) p.size());
				Mine old = MINES.put(trap, m);
				if (old != null) {
					old.done = true;
				}
				GrandLineFx.add(m);
			}
			case "bomb_mine_blast" -> {
				int trap = (int) p.range();
				Mine m = trap >= 0 ? MINES.remove(trap) : null;
				if (m != null) {
					m.done = true;
				}
				if (trap >= 0 && p.speed() == 0) {
					GrandLineAudio.play(GrandLineSounds.BOMB_MINE_TRIGGER, p.pos(), 0.8f, 1f);
				}
				ExplosionFx.spawn(level, p.pos().add(0, 0.25, 0), new Vec3(0, 1, 0), new ExplosionFx.Spec(ExplosionFx.Shape.RADIAL, p.size(), 0.42f, 1, true,
						false, 0.7f));
				boom(GrandLineSounds.BOMB_EXPLODE, p.pos(), 0.9f, 0.9f + r.nextFloat() * 0.25f);
			}
			case "bomb_mine_gone" -> {
				Mine m = MINES.remove((int) p.range());
				if (m != null) {
					m.fade();
				}
			}
			case "bomb_head_start" -> {
				BodyMotion.bomb(id, age, 5);
				Head h = new Head();
				h.updated = now();
				HEADS.put(id, h);
				GrandLineAudio.play(GrandLineSounds.BOMB_HEAD, p.pos().add(0, 1.7, 0), 1f, 1f);
				if (caster != null) {
					GrandLineAudio.loop(GrandLineSounds.BOMB_SELF_CHARGE, () -> caster.position().add(0, 1.7, 0), () -> headActive(id) && caster.isAlive(),
							0.9f, () -> 0.85 + 0.4 * h.charge, null);
					GrandLineAudio.loop(GrandLineSounds.BOMB_FUSE_LOOP, () -> caster.position().add(0, 2.2, 0), () -> headActive(id) && caster.isAlive(),
							0.5f, () -> 1.0, null);
					GrandLineFx.add(new HeadCharge(id));
				}
			}
			case "bomb_head" -> {
				Head h = HEADS.get(id);
				if (h != null) {
					h.charge = (float) p.speed();
					h.updated = now();
				}
			}
			case "bomb_head_max" -> {
				GrandLineAudio.play(GrandLineSounds.BOMB_SELF_MAX, p.pos().add(0, 1.8, 0), 1f, 1f);
				GrandLineFx.add(new SpringFx.Ring(p.pos().add(0, 1.9, 0), new Vec3(0, 1, 0), 1.6, 8, ExplosionFx.HOT, false));
			}
			case "bomb_self_destruct" -> {
				HEADS.remove(id);
				BodyMotion.stop(id, BodyMotion.Kind.BOMB, age);
				float c = (float) p.speed();
				ExplosionFx.spawn(level, p.pos(), new Vec3(0, 1, 0), new ExplosionFx.Spec(ExplosionFx.Shape.RADIAL, p.size(), 0.5f + 0.5f * c, 2 + Math.round(2 * c),
						true, c > 0.35f, 1.5f + 3f * c));
				GrandLineFx.add(new PhysicsSparks(level, p.pos(), new Vec3(0, 0.5, 0), (int) (10 + 20 * c), 1, 0.5 + 0.4 * c, 24, 0.08, ExplosionFx.CASING,
						0x1E2024, false));
				boom(GrandLineSounds.BOMB_SELF_BLAST, p.pos(), 1f, 1.05f - 0.12f * c);
				GrandLineAudio.play(GrandLineSounds.BOMB_RESIDUE, p.pos(), 0.4f + 0.4f * c, 1f);
			}
			case "bomb_jump" -> {
				BodyMotion.bomb(id, age, 6);
				Vec3 launch = p.dir();
				ExplosionFx.spawn(level, p.pos().add(0, 0.2, 0), launch.scale(-1), new ExplosionFx.Spec(ExplosionFx.Shape.DIRECTIONAL, p.size(), 0.5f, 2, true,
						false, own ? 0.9f : 0.5f));
				if (caster != null) {
					GrandLineFx.add(new SpringFx.Motion(id, 1.8, 24));
				}
				boom(GrandLineSounds.BOMB_JUMP, p.pos(), 1f, 1f);
			}
			case "bomb_jump_hurt" -> {
				GrandLineAudio.play(GrandLineSounds.BOMB_JUMP_HURT, p.pos(), own ? 0.8f : 0.4f, 1f);
				for (int i = FxSprites.count(4); i > 0; i--) {
					FxSprites.smoke(p.pos().add(FxSprites.randomDir(r).scale(0.3)), new Vec3(0, 0.05, 0), 0.35, 16, 0x5A4A44);
				}
			}
			case "bomb_jump_heat" -> {
				if (own) {
					jumpSafeAt = now() + (long) p.speed();
				}
			}
			default -> { }
		}
	}

	static int light(ClientLevel level, Vec3 p) {
		return LevelRenderer.getLightColor(level, BlockPos.containing(p));
	}

	/** Draws a bomb model at a world position (camera-relative pose). */
	static void drawBomb(FxDraw d, ClientLevel level, Vec3 at, float radius, float fuse, float heat, float burn, float yaw, float pitch) {
		PoseStack ps = d.pose;
		ps.pushPose();
		ps.translate(at.x - d.camPos.x, at.y - d.camPos.y, at.z - d.camPos.z);
		ps.mulPose(Axis.YP.rotationDegrees(yaw));
		ps.mulPose(Axis.XP.rotationDegrees(pitch));
		BombModel.bomb(ps, d.buffers, light(level, at), radius, fuse, heat, burn);
		ps.popPose();
	}

	/** World position of a bomb's fuse tip (for sparks) — approximate, above the bomb. */
	static Vec3 fuseTip(Vec3 centre, float radius) {
		return centre.add(radius * 0.4, radius * 1.6, 0);
	}

	static void fuseSparks(Vec3 tip, float intensity, RandomSource r) {
		for (int i = FxSprites.count(Math.max(1, Math.round(2 * intensity))); i > 0; i--) {
			FxSprites.spark(tip, FxSprites.randomDir(r).scale(0.05 + 0.08 * intensity).add(0, 0.04, 0), 0.03 + 0.02 * intensity, 5 + r.nextInt(4),
					ExplosionFx.CORE, ExplosionFx.OUTER);
		}
	}

	// ------------------------------------------------------------------------------------------------ body layer

	/** Third person: the charging bomb in the right hand and the Self Destruct bomb head. */
	public static void drawLayer(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model,
			float age) {
		int id = player.getId();
		long[] hand = HAND.get(id);
		if (hand != null && now() - hand[0] < hand[1] + 40) {
			pose.pushPose();
			model.rightArm.translateAndRotate(pose);
			drawHandBomb(pose, buffers, light, hand, age);
			pose.popPose();
		}
		Head h = HEADS.get(id);
		if (h != null && headActive(id)) {
			h.shown = Mth.lerp(0.25f, h.shown, h.charge); // smooth growth between beats
			float radius = 0.34f + 0.42f * h.shown;
			pose.pushPose();
			model.head.translateAndRotate(pose);
			pose.translate(0, -radius * 0.95, 0);
			pose.mulPose(Axis.XP.rotationDegrees(180));
			float shake = h.shown * 0.02f;
			pose.translate(Mth.sin(age * 3.7f) * shake, 0, Mth.cos(age * 4.3f) * shake);
			BombModel.bomb(pose, buffers, light, radius, 1 - 0.7f * h.shown, 0.15f + 0.85f * h.shown, age);
			pose.popPose();
		}
	}

	/** First person: the charging bomb in the visible right hand. */
	public static void drawArm(PoseStack pose, MultiBufferSource buffers, int light, int id, float age, boolean left) {
		long[] hand = HAND.get(id);
		if (left || hand == null || now() - hand[0] >= hand[1] + 40) {
			return;
		}
		drawHandBomb(pose, buffers, light, hand, age);
	}

	private static void drawHandBomb(PoseStack pose, MultiBufferSource buffers, int light, long[] hand, float age) {
		float charge = Mth.clamp((now() - hand[0]) / (float) hand[1], 0, 1);
		float radius = 0.16f + 0.13f * charge;
		pose.pushPose();
		pose.translate(-0.06, 0.62 + radius, 0);
		pose.mulPose(Axis.XP.rotationDegrees(180));
		BombModel.bomb(pose, buffers, light, radius, 1 - 0.3f * charge, 0.25f * charge, age);
		pose.popPose();
	}

	// ------------------------------------------------------------------------------------------------ effects

	static final class Head {
		float charge, shown;
		long updated;
	}

	/** Self Destruct charge aura: air drawn inward toward the head, heat embers, faint distortion around the bomb. */
	static final class HeadCharge extends GrandLineFx.Effect {
		HeadCharge(int caster) {
			super(caster);
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Entity c = caster(level);
			Head h = HEADS.get(caster);
			if (c == null || h == null || !headActive(caster)) {
				return false;
			}
			RandomSource r = level.random;
			Vec3 head = c.position().add(0, 1.9 + 0.3 * h.shown, 0);
			for (int i = FxSprites.count(1 + (int) (3 * h.shown)); i > 0; i--) {
				Vec3 from = head.add(FxSprites.randomDir(r).scale(2.5 + 1.5 * h.shown));
				FxSprites.sprite(FxDraw.STREAK, true, from, head.subtract(from).scale(0.12), 0.04 + 0.03 * h.shown, 0, 0, 0.92, 8,
						0xFFE6B0, ExplosionFx.FIRE, 0.35f + 0.3f * h.shown);
			}
			if (age % 3 == 0) {
				fuseSparks(head.add(0, 0.5 + 0.4 * h.shown, 0), 0.6f + h.shown, r);
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity c = caster(level);
			Head h = HEADS.get(caster);
			if (c == null || h == null) {
				return;
			}
			Vec3 head = c.getPosition(d.partial).add(0, 1.9 + 0.3 * h.shown, 0);
			float k = h.shown;
			// Charge-dependent distortion shell and pulsing heat glow.
			float pulse = 0.6f + 0.4f * Mth.sin((age + d.partial) * (0.4f + 0.6f * k));
			d.billboard(FxDraw.GLOW, true, head, 0.6 + 0.9 * k, 0, FxDraw.rgba(ExplosionFx.FIRE, 0.12f + 0.25f * k * pulse));
			// Air drawn in: a faint pressure ring tightening around the head as the charge builds.
			float ring = ((age + d.partial) % 10) / 10f;
			Vec3[] b = WindMesh.basis(new Vec3(0, 1, 0));
			double rr = (1.6 + k) * (1 - ring) + 0.5;
			WindRender.with(WindRender.Material.FIRE, () -> WindMesh.ring(d, true, head, b[0], b[1], rr - 0.06, rr, 0, 360, ExplosionFx.HOT,
					(0.15f + 0.25f * k) * ring, 0, 0));
		}
	}

	/** The thrown Blast Toss bomb: same flight as the server, spinning, fuse sparking, until the hit beat. */
	static final class Thrown extends GrandLineFx.Effect {
		Vec3 pos, prev, vel;
		final float radius, charge;

		Thrown(int caster, Vec3 pos, Vec3 vel, float radius, float charge) {
			super(-1);
			this.pos = pos;
			this.prev = pos;
			this.vel = vel;
			this.radius = radius;
			this.charge = charge;
			GrandLineAudio.loop(GrandLineSounds.BOMB_FUSE_LOOP, () -> this.pos, () -> !done, 0.45f, () -> 1.1, null);
		}

		@Override
		boolean tick(ClientLevel level) {
			if (++age > BombMoves.TOSS_LIFE + 6) {
				return false;
			}
			prev = pos;
			pos = pos.add(vel);
			vel = vel.scale(0.99).add(0, -BombMoves.TOSS_GRAVITY, 0);
			fuseSparks(fuseTip(pos, radius), 0.7f, level.random);
			if (age % 2 == 0) {
				FxSprites.smoke(prev, Vec3.ZERO, 0.12 + 0.08 * charge, 12, 0x8A8480);
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Vec3 p = prev.lerp(pos, d.partial);
			float spin = (age + d.partial) * 25;
			drawBomb(d, level, p, radius, 0.8f, 0.1f * charge, age + d.partial, 0, spin);
		}
	}

	/** A ring bomb thrown from the caster to its spot, sitting there until its detonation time. */
	static final class Lob extends GrandLineFx.Effect {
		final Vec3 from, to;
		final int flight, life;

		Lob(int caster, Vec3 from, Vec3 to, int flight, int life) {
			super(-1);
			this.from = from;
			this.to = to;
			this.flight = flight;
			this.life = life;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (age == flight) {
				SpringFx.ground(level, to, 0.4, 2);
			}
			return age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = Math.min(1, (age + d.partial) / flight);
			Vec3 p = from.lerp(to, t).add(0, Math.sin(t * Math.PI) * 1.6 + 0.18, 0);
			drawBomb(d, level, p, 0.18f, 0.5f, 0, age + d.partial, age * 30f, t < 1 ? age * 40f : 0);
		}
	}

	/** The Targeted Bomb indicator: a ground circle following the aim, a pulsing core and orbiting metal fragments. */
	static final class Target extends GrandLineFx.Effect {
		Vec3 shown, goal, prevShown;
		final double radius;
		float charge;
		boolean released;
		int heard, releaseAge = -1;

		Target(int caster, Vec3 at, double radius) {
			super(caster);
			this.shown = at;
			this.prevShown = at;
			this.goal = at;
			this.radius = radius;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			prevShown = shown;
			shown = shown.lerp(goal, 0.5);
			if (released && releaseAge < 0) {
				releaseAge = age;
			}
			if (releaseAge >= 0) {
				return age - releaseAge < 3;
			}
			return ++heard < 8; // the held beats stopped (interrupted): remove at once
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Vec3 c = prevShown.lerp(shown, d.partial);
			float a = age + d.partial;
			float fade = releaseAge >= 0 ? Math.max(0, 1 - (a - releaseAge) / 3f) : Math.min(1, a / 3f);
			Vec3[] b = WindMesh.basis(new Vec3(0, 1, 0));
			Vec3 g = c.add(0, 0.06, 0);
			WindRender.with(WindRender.Material.FIRE, () -> {
				// Outer boundary (explosion radius) and an inner ring tightening as the charge builds.
				WindMesh.ring(d, true, g, b[0], b[1], radius - 0.12, radius, a * 3, 360, ExplosionFx.OUTER, 0.5f * fade, 0, a * 0.05);
				double inner = radius * (0.85 - 0.55 * charge);
				WindMesh.ring(d, true, g, b[0], b[1], inner - 0.08, inner, -a * 6, 360, ExplosionFx.HOT, 0.45f * fade, 0, -a * 0.08);
				// Four rotating tick marks.
				for (int k = 0; k < 4; k++) {
					WindMesh.ring(d, true, g, b[0], b[1], radius * 0.55, radius * 0.95, a * 4 + k * 90, 14, ExplosionFx.FIRE, 0.4f * fade, 0, 0);
				}
			});
			// Pulsing explosive core above the spot and orbiting casing fragments.
			float pulse = 0.65f + 0.35f * Mth.sin(a * (0.5f + charge));
			Vec3 core = c.add(0, 0.7 + 0.2 * charge, 0);
			d.billboard(FxDraw.GLOW, true, core, (0.35 + 0.35 * charge) * pulse, 0, FxDraw.rgba(ExplosionFx.FIRE, 0.75f * fade));
			d.billboard(FxDraw.GLOW, true, core, (0.15 + 0.15 * charge) * pulse, 0, FxDraw.rgba(ExplosionFx.CORE, 0.9f * fade));
			int n = 3 + (int) (WindMesh.detail() * 3);
			for (int k = 0; k < n; k++) {
				double ang = a * (0.12 + 0.05 * k) + k * Math.PI * 2 / n;
				double rr = 0.55 + 0.15 * Math.sin(a * 0.2 + k);
				Vec3 fp = core.add(Math.cos(ang) * rr, Math.sin(a * 0.15 + k * 1.7) * 0.25, Math.sin(ang) * rr);
				d.billboard(FxDraw.SPARK, false, fp, 0.06, ang, FxDraw.rgba(0x6A6E76, 0.9f * fade));
			}
		}
	}

	/** A bomb stuck on an entity's head: follows it every frame, fuse burning down, sparks speeding up. */
	static final class Attached extends GrandLineFx.Effect {
		final int target, fuse;
		final float height;

		Attached(int target, int fuse, float height) {
			super(-1);
			this.target = target;
			this.fuse = fuse;
			this.height = height;
		}

		float progress() {
			return Mth.clamp(age / (float) fuse, 0, 1);
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Entity e = level.getEntity(target);
			if (e == null || !e.isAlive() || age > fuse + 20) {
				ATTACHED.remove(target);
				return false;
			}
			float k = progress();
			if (age % Math.max(1, 4 - (int) (k * 3)) == 0) {
				fuseSparks(fuseTip(e.position().add(0, e.getBbHeight() + 0.2, 0), 0.2f), 0.5f + 1.5f * k, level.random);
			}
			return true;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity e = level.getEntity(target);
			if (e == null) {
				return;
			}
			float k = progress();
			Vec3 at = e.getPosition(d.partial).add(0, e.getBbHeight() + 0.17, 0);
			float yaw = -Mth.rotLerp(d.partial, e.yRotO, e.getYRot());
			drawBomb(d, level, at, 0.2f, 1 - k, k > 0.75f ? (k - 0.75f) * 2.4f * (0.5f + 0.5f * Mth.sin((age + d.partial) * 1.6f)) : 0, age + d.partial,
					yaw, 0);
		}
	}

	/** A deployed land mine: tossed out, settles, arms (light), blinks softly until it goes off, expires or is removed. */
	static final class Mine extends GrandLineFx.Effect {
		final int trap, owner, delay, life;
		final boolean own;
		final Vec3 at, from;
		int fadeAge = -1;

		Mine(int trap, int owner, boolean own, Vec3 at, Vec3 from, int delay, int life) {
			super(-1);
			this.trap = trap;
			this.owner = owner;
			this.own = own;
			this.at = at;
			this.from = from;
			this.delay = delay;
			this.life = life;
		}

		void fade() {
			if (fadeAge < 0) {
				fadeAge = age;
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (age == delay + 6) {
				SpringFx.ground(level, at, 0.3, 2);
			}
			if (age == delay + 6 + BombMoves.MINE_ARM && own) {
				GrandLineAudio.play(GrandLineSounds.BOMB_MINE_ARM, at, 0.6f, 1f);
			}
			if (age > life + delay + 40) {
				MINES.remove(trap);
				return false;
			}
			return fadeAge < 0 || age - fadeAge < 10;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			if (at.distanceToSqr(d.cameraPosition()) > 48 * 48) {
				return;
			}
			float a = age + d.partial;
			float fly = Mth.clamp((a - delay) / 6f, 0, 1);
			if (a < delay) {
				return;
			}
			Vec3 p = from.lerp(at, fly).add(0, Math.sin(fly * Math.PI) * 1.2, 0);
			float sink = fadeAge >= 0 ? Math.min(1, (a - fadeAge) / 10f) : 0;
			float armed = Mth.clamp((a - delay - 6) / BombMoves.MINE_ARM, 0, 1);
			float blink = armed >= 1 ? (Mth.sin(a * 0.35f) > 0.6f ? 1 : 0.2f) : armed * (Mth.sin(a * 1.2f) > 0 ? 1 : 0);
			PoseStack ps = d.pose;
			ps.pushPose();
			ps.translate(p.x - d.camPos.x, p.y - d.camPos.y - sink * 0.12, p.z - d.camPos.z);
			ps.mulPose(Axis.YP.rotationDegrees(trap * 47 % 360));
			if (fly < 1) {
				ps.mulPose(Axis.XP.rotationDegrees(a * 40));
			}
			BombModel.mine(ps, d.buffers, light(level, p), armed, blink * (1 - sink), own);
			ps.popPose();
		}
	}
}
