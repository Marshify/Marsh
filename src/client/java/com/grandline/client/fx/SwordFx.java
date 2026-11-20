package com.grandline.client.fx;

import com.grandline.client.settings.CameraShake;
import com.grandline.network.payload.FxEventPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.grandline.client.fx.SwordShapes.RANDOM;
import static com.grandline.client.fx.SwordShapes.UP;
import static com.grandline.client.fx.SwordShapes.flat;
import static com.grandline.client.fx.SwordShapes.rgb;
import static com.grandline.client.fx.SwordShapes.right;

/**
 * Sword move visuals. The server sends beats named {@code sw:<shape>:<palette>:<variant>} on the tick each step of a
 * move happens (see SwordKit); this turns each into shaped effects in the sword's palette. Travelling shapes use the
 * same start, direction, speed and range as the server's hit volume and stop on its impact beat.
 */
final class SwordFx {
	private SwordFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("sw:");
	}

	static void handle(FxEventPayload p, ClientLevel level, Entity caster, boolean own) {
		String[] parts = p.effect().split(":");
		if (parts.length < 4) {
			return;
		}
		String shape = parts[1];
		SwordPalette pal = SwordPalette.of(parts[2]);
		int v;
		try {
			v = Integer.parseInt(parts[3]);
		} catch (NumberFormatException e) {
			v = 0;
		}
		Vec3 pos = p.pos();
		Vec3 dir = p.dir().lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : p.dir().normalize();
		int id = p.caster();
		double speed = p.speed(), range = p.range(), size = p.size();
		Vec3 casterPos = caster == null ? pos : caster.position();
		switch (shape) {
			case "pose" -> pose(id, caster, v, (int) size);
			case "trail" -> GrandLineFx.add(new SwordShapes.Trail(id, pos, casterPos, dir, size * 0.82, v, pal, size / 3.0));
			case "cut" -> cut(pos, dir, v, size, pal);
			case "end" -> {
				GrandLineFx.endProjectile(id, pos);
				if (size > 0.2) {
					GrandLineFx.add(new SwordShapes.Flash(pos, size, 6, pal.edge(), pal.core()));
					SwordShapes.sparks(pos, Vec3.ZERO, 8, 0.3, pal);
				}
			}
			case "flash" -> {
				GrandLineFx.add(new SwordShapes.Flash(pos, size, v == 2 ? 9 : 6, pal.edge(), pal.core()));
				SwordShapes.sparks(pos, dir, v == 2 ? 22 : 10, 0.4, pal);
				if (v == 2) {
					GrandLineFx.add(new SwordShapes.Shockwave(pos.subtract(0, 1.0, 0), 0.6, size * 1.2, 0.6, pal));
				}
			}
			case "sparks" -> {
				SwordShapes.sparks(pos, dir, 14, 0.35, pal);
				GrandLineFx.add(new SwordShapes.Flash(pos, size * 0.5, 4, pal.edge(), pal.core()));
			}
			case "dash" -> {
				GrandLineFx.add(new DashLines(id, pos, dir, range, speed, size, v, pal));
				if (v == 1 && caster instanceof AbstractClientPlayer pl) {
					GrandLineFx.add(new Afterimage(pl, pos.subtract(0, 0.9, 0), pl.getYRot(), pal, 14));
				}
			}
			case "crescent" -> {
				double roll = switch (v) {
					case 1 -> -0.3;
					case 2 -> 0.45;
					case 3 -> -0.45;
					case 4 -> 0.6;
					case 5 -> -0.6;
					default -> 0.12;
				};
				GrandLineFx.add(new SwordShapes.Crescent(id, pos, dir, speed, range, size, roll, 1, pal, v >= 2));
			}
			case "crossfire" -> {
				GrandLineFx.add(new SwordShapes.Crescent(id, pos, dir, speed, range, size, Math.PI / 4, 4, pal, true));
				GrandLineFx.add(new SwordShapes.Flash(pos, 1.6, 6, pal.edge(), pal.core()));
			}
			case "cleaver" -> {
				GrandLineFx.add(new SwordShapes.Crescent(id, pos, dir, speed, range, size, Math.PI / 2, 1, pal, true));
				GrandLineFx.add(new Wisps(id, pos, dir, speed, range, pal));
			}
			case "pressure" -> {
				GrandLineFx.add(new SwordShapes.Crescent(id, pos, dir, speed, range, size, Math.PI / 2, 1, pal, true));
				GrandLineFx.add(new Distortion(id, pos, dir, speed, range, size, pal));
			}
			case "orb" -> GrandLineFx.add(new SwordShapes.RibbonOrb(id, pos, dir, speed, range, size, pal));
			case "spinner" -> GrandLineFx.add(new MovingVortex(id, pos, dir, speed, range, size * 0.8, 2.2, pal, false));
			case "tornado" -> GrandLineFx.add(new MovingVortex(id, pos, dir, speed, range, size, 5.5, pal, false));
			case "vortex" -> GrandLineFx.add(new SwordShapes.Vortex(-1, (vx, lv, pt) -> follow(lv, id, pos, pt), range, size, 0.25,
					(int) speed, pal, 7, false));
			case "surge" -> GrandLineFx.add(new SwordShapes.Vortex(-1, (vx, lv, pt) -> follow(lv, id, pos, pt), range * 0.85, size, 0.1,
					(int) speed, pal, 9, true));
			case "grinder" -> GrandLineFx.add(new Grinder(id, pos, (int) speed, range, pal));
			case "pursuit" -> GrandLineFx.add(new Pursuit(id, (int) speed, size, pal));
			case "burst" -> {
				GrandLineFx.add(new SwordShapes.Shockwave(pos.subtract(0, 1, 0), 0.9, size + 1, 1.6, pal));
				GrandLineFx.add(new SwordShapes.Flash(pos, size * 0.6, 7, pal.edge(), pal.core()));
				SwordShapes.sparks(pos, Vec3.ZERO, 26, 0.6, pal);
			}
			case "slam" -> slam(level, pos, dir, range, v, pal);
			case "shockwave" -> {
				GrandLineFx.add(new SwordShapes.Shockwave(pos, speed, range, size, pal));
				GrandLineFx.add(new SwordShapes.Fractures(pos, dir, 6, range * 0.5, pal, 40, true));
				GrandLineFx.add(new SwordShapes.Flash(pos.add(0, 0.6, 0), 1.6, 7, pal.edge(), pal.core()));
				debris(level, pos, 1.5, 14);
			}
			case "core" -> {
				GrandLineFx.add(new SwordShapes.Shell(pos.add(0, 1, 0), range, 8, 22, pal));
				GrandLineFx.add(new SwordShapes.Shockwave(pos, 0.7, range + 2, 1.2, pal));
				GrandLineFx.add(new SwordShapes.Shockwave(pos, 0.45, range, 0.6, pal));
				GrandLineFx.add(new SwordShapes.Fractures(pos, dir, 7, range * 0.75, pal, 60, true));
				GrandLineFx.add(new SwordShapes.Flash(pos.add(0, 1, 0), 3.2, 8, pal.edge(), pal.core()));
				debris(level, pos, range * 0.4, 30);
				SwordShapes.dust(pos, range * 0.4, 18, 0x8A8478);
				shake(pos, 2.5f);
			}
			case "orb_form" -> GrandLineFx.add(new TipOrb(id, (int) size, pal));
			case "energy_orb" -> GrandLineFx.add(new EnergyOrb(id, pos, dir, speed, range, size, pal));
			case "core_orb" -> GrandLineFx.add(new CoreOrb(pos, dir, (int) speed, range, size, pal));
			case "dragon" -> GrandLineFx.add(new DragonBurst(pos, dir, (int) size, pal));
			case "blast" -> {
				GrandLineFx.add(new SwordShapes.Flash(pos, size, 9, 0xFFF2B0, 0xFF6A10));
				GrandLineFx.add(new SwordShapes.Shockwave(pos.subtract(0, 0.8, 0), 0.6, size + 0.5, 1.4, pal));
				for (int i = 0; i < FxSprites.count(30); i++) {
					Vec3 vel = SwordShapes.side(UP).scale(0).add(RANDOM.nextGaussian(), RANDOM.nextDouble() * 0.8, RANDOM.nextGaussian()).normalize().scale(0.25);
					FxSprites.ember(pos, vel, 0.35 + RANDOM.nextDouble() * 0.3, 14 + RANDOM.nextInt(8), 0xFFE07A, 0xD52A0C);
				}
				for (int i = 0; i < FxSprites.count(8); i++) {
					FxSprites.smoke(pos.add(RANDOM.nextGaussian() * 0.6, 0.3, RANDOM.nextGaussian() * 0.6), new Vec3(0, 0.04, 0), 0.7, 30, 0x2C2522);
				}
				shake(pos, v == 1 ? 1.6f : 1.0f);
			}
			case "embers" -> {
				for (int i = 0; i < FxSprites.count(8); i++) {
					FxSprites.ember(pos.add(RANDOM.nextGaussian() * 0.3, RANDOM.nextDouble() * 0.5, RANDOM.nextGaussian() * 0.3),
							new Vec3(0, 0.05, 0), 0.25, 12, 0xFFE07A, 0xFF4A0A);
				}
			}
			case "serpent" -> GrandLineFx.add(new Serpent(id, pos, dir, speed, range, size, pal));
			case "beam" -> {
				Vec3 to = pos.add(dir.scale(range));
				GrandLineFx.add(new SwordShapes.Beam(pos, to, size, pal));
				for (double d = 0; d < range; d += 1.5) {
					SwordShapes.motes(pos.add(dir.scale(d)), 0.25, 1, pal, 18);
				}
			}
			case "spirit_burst" -> {
				GrandLineFx.add(new SwordShapes.Shell(pos, size, 5, 14, pal));
				GrandLineFx.add(new SwordShapes.Flash(pos, size * 0.8, 8, pal.edge(), pal.core()));
				SwordShapes.motes(pos, size * 0.5, 22, pal, 30);
				GrandLineFx.add(new SwordShapes.Shockwave(pos.subtract(0, 0.8, 0), 0.5, size, 0.8, pal));
			}
			case "spirit_arc" -> {
				GrandLineFx.add(new SwordShapes.Trail(-1, pos, pos, dir, size * 0.6, v % 2 == 0 ? 1 : 2, pal, 1.6));
				GrandLineFx.add(new SwordShapes.Trail(-1, pos.add(0, 0.25, 0), pos, dir, size * 0.5, v % 2 == 0 ? 0 : 6, pal, 1.2));
				SwordShapes.motes(pos.add(dir.scale(size * 0.5)), 1.2, 8, pal, 22);
			}
			case "grapple" -> GrandLineFx.add(new Grapple(id, pos, dir, speed, range));
			case "grapple_hit" -> {
				Grapple g = GrandLineFx.find(Grapple.class, id);
				if (g != null) {
					g.stick(pos, v);
				}
				splash(level, pos, 10, pal);
			}
			case "tether" -> {
				Grapple g = GrandLineFx.find(Grapple.class, id);
				if (g != null) {
					g.follow(v, (int) size);
				}
			}
			case "grapple_return" -> {
				Grapple g = GrandLineFx.find(Grapple.class, id);
				if (g != null) {
					g.recall();
				}
			}
			case "water_burst" -> {
				GrandLineFx.add(new SwordShapes.Shockwave(pos.subtract(0, 0.5, 0), 0.8, size + 1, 2.2, pal));
				GrandLineFx.add(new SwordShapes.Shockwave(pos.subtract(0, 0.5, 0), 0.5, size, 1.0, pal));
				GrandLineFx.add(new SwordShapes.Flash(pos, size * 0.5, 8, pal.edge(), pal.core()));
				GrandLineFx.add(new WaterRibbons(pos, size, pal));
				splash(level, pos, 40, pal);
				shake(pos, 1.2f);
			}
			case "storm_cloud" -> GrandLineFx.add(new SwordShapes.Cloud(id, pos, dir, speed, range, size, 200, pal));
			case "storm_burst" -> {
				for (int i = 0; i < 7; i++) {
					Vec3 out = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian() * 0.5, RANDOM.nextGaussian()).normalize().scale(size);
					GrandLineFx.add(new SwordShapes.Lightning(pos, pos.add(out), 0.35, 1, 0.07, 7, pal));
				}
				GrandLineFx.add(new SwordShapes.Flash(pos, size * 0.8, 7, pal.edge(), pal.core()));
				GrandLineFx.add(new SwordShapes.Shockwave(pos.subtract(0, 0.8, 0), 0.7, size, 1.0, pal));
				SwordShapes.sparks(pos, Vec3.ZERO, 24, 0.5, pal);
			}
			case "thundercloud" -> GrandLineFx.add(new SwordShapes.Cloud(id, pos.add(0, 9, 0), Vec3.ZERO, 0, 0, size, (int) speed, pal));
			case "bolt" -> {
				Vec3 top = pos.add(RANDOM.nextGaussian() * 0.6, range, RANDOM.nextGaussian() * 0.6);
				GrandLineFx.add(new SwordShapes.Lightning(top, pos, 0.7, 3, 0.16, 8, pal));
				GrandLineFx.add(new SwordShapes.Lightning(top, pos, 0.5, 1, 0.08, 6, pal));
				GrandLineFx.add(new SwordShapes.Flash(pos.add(0, 0.6, 0), 2.4, 7, pal.edge(), pal.core()));
				GrandLineFx.add(new SwordShapes.Shockwave(pos.add(0, 0.05, 0), 0.6, size, 0.7, pal));
				SwordShapes.sparks(pos.add(0, 0.2, 0), UP, 18, 0.5, pal);
				shake(pos, 0.9f);
			}
			case "charge" -> GrandLineFx.add(new BladeCharge(id, (int) size, pal, v == 1));
			case "blade_storm" -> GrandLineFx.add(new BladeStorm(id, dir, (int) speed, range, size, pal));
			case "eclipse_charge" -> {
				BladeCharge c = GrandLineFx.find(BladeCharge.class, id);
				if (c == null) {
					GrandLineFx.add(c = new BladeCharge(id, 60, pal, v == 1));
				}
				c.refresh(size);
			}
			default -> { }
		}
	}

	/** Where a following effect is: the caster's feet if they are still here, else where the beat said. */
	static Vec3 follow(ClientLevel level, int id, Vec3 fallback, float partial) {
		Entity e = level.getEntity(id);
		return e == null || !e.isAlive() ? fallback : e.getPosition(partial);
	}

	static void pose(int id, Entity caster, int kind, int releaseIn) {
		if (caster == null) {
			return;
		}
		MovePoses.Kind k = switch (kind) {
			case 1 -> MovePoses.Kind.SLASH_L;
			case 2 -> MovePoses.Kind.OVERHEAD;
			case 3 -> MovePoses.Kind.THRUST;
			case 4 -> MovePoses.Kind.SPIN;
			case 5 -> MovePoses.Kind.RAISE;
			case 6 -> MovePoses.Kind.GROUND;
			case 7 -> MovePoses.Kind.THROW;
			default -> MovePoses.Kind.SLASH_R;
		};
		float age = caster.tickCount;
		if (releaseIn <= 0) {
			MovePoses.release(id, k, age, false);
		} else {
			MovePoses.windup(id, k, age);
			int[] fired = {0};
			GrandLineFx.add(new GrandLineFx.Effect(id) {
				@Override
				boolean tick(ClientLevel level) {
					if (++age >= releaseIn && fired[0]++ == 0) {
						Entity e = caster(level);
						if (e != null) {
							MovePoses.release(id, k, e.tickCount, false);
						}
						return false;
					}
					return age < releaseIn + 1;
				}

				@Override
				void render(FxDraw d, ClientLevel level) {
				}
			});
		}
	}

	static void cut(Vec3 at, Vec3 dir, int variant, double size, SwordPalette pal) {
		Vec3 f = flat(dir);
		Vec3 s = right(f);
		double a = variant % 2 == 0 ? 0.6 : -0.6;
		Vec3 axis = s.scale(Math.cos(a)).add(UP.scale(Math.sin(a)));
		GrandLineFx.add(new Streak(at.subtract(axis.scale(size)), at.add(axis.scale(size)), 0.18 * size, pal, 6));
		if (variant >= 2) {
			Vec3 axis2 = s.scale(Math.cos(-a)).add(UP.scale(Math.sin(-a)));
			GrandLineFx.add(new Streak(at.subtract(axis2.scale(size)), at.add(axis2.scale(size)), 0.16 * size, pal, 6));
		}
		GrandLineFx.add(new SwordShapes.Flash(at, size * 0.6, 4, pal.edge(), pal.core()));
		SwordShapes.sparks(at, f, 6, 0.3, pal);
	}

	static void slam(ClientLevel level, Vec3 at, Vec3 dir, double radius, int fractures, SwordPalette pal) {
		GrandLineFx.add(new SwordShapes.Shockwave(at, 0.7, radius, 0.9, pal));
		if (fractures > 0) {
			GrandLineFx.add(new SwordShapes.Fractures(at, dir, fractures, radius * 0.9, pal, 50, false));
		}
		GrandLineFx.add(new SwordShapes.Flash(at.add(0, 0.4, 0), radius * 0.35, 6, pal.edge(), pal.core()));
		debris(level, at, radius * 0.35, fractures > 0 ? 26 : 10);
		SwordShapes.dust(at, radius * 0.3, fractures > 0 ? 14 : 6, 0x8A8070);
		shake(at, fractures > 0 ? 1.6f : 0.6f);
	}

	/** Bits of the ground block thrown up (vanilla block particles of whatever is underfoot) - nothing is broken. */
	static void debris(ClientLevel level, Vec3 at, double spread, int base) {
		BlockState ground = level.getBlockState(net.minecraft.core.BlockPos.containing(at.subtract(0, 0.2, 0)));
		if (ground.isAir()) {
			ground = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
		}
		for (int i = 0; i < FxSprites.count(base); i++) {
			level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x + RANDOM.nextGaussian() * spread, at.y + 0.2,
					at.z + RANDOM.nextGaussian() * spread, RANDOM.nextGaussian() * 0.25, 0.35 + RANDOM.nextDouble() * 0.45, RANDOM.nextGaussian() * 0.25);
		}
	}

	static void splash(ClientLevel level, Vec3 at, int base, SwordPalette pal) {
		for (int i = 0; i < FxSprites.count(base); i++) {
			Vec3 v = new Vec3(RANDOM.nextGaussian() * 0.25, 0.2 + RANDOM.nextDouble() * 0.35, RANDOM.nextGaussian() * 0.25);
			FxSprites.sprite(FxDraw.SHELL, true, at, v, 0.08 + RANDOM.nextDouble() * 0.1, 0, 0.03, 0.97, 18 + RANDOM.nextInt(10), 0xF0FBFF, pal.core(), 0.9f);
			level.addParticle(ParticleTypes.SPLASH, at.x + RANDOM.nextGaussian() * 0.6, at.y, at.z + RANDOM.nextGaussian() * 0.6, 0, 0.3, 0);
		}
	}

	static void shake(Vec3 at, float strength) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			CameraShake.impact(mc.player.position().distanceTo(at), strength);
		}
	}

	// ============================================================================================ small pieces

	/** A single straight cutting streak that flashes and fades. */
	static final class Streak extends GrandLineFx.Effect {
		final Vec3 a;
		final Vec3 b;
		final double width;
		final SwordPalette pal;
		final int life;

		Streak(Vec3 a, Vec3 b, double width, SwordPalette pal, int life) {
			super(-1);
			this.a = a;
			this.b = b;
			this.width = width;
			this.pal = pal;
			this.life = life;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / life;
			float alpha = (float) Math.pow(1 - t, 1.3);
			double reveal = Math.min(1, t * 4);
			Vec3 end = a.add(b.subtract(a).scale(reveal));
			List<Vec3> pts = List.of(a, a.add(end.subtract(a).scale(0.5)), end);
			if (pal.darkCore()) {
				d.ribbonSoft(FxDraw.GLOW, pts, new double[]{width * 2.2, width * 2.6, width * 2.2}, rgb(pal.dark()),
						new float[]{0.5f * alpha, 0.7f * alpha, 0.5f * alpha}, 0);
			}
			d.ribbon(FxDraw.GLOW, pts, new double[]{width * 2.5, width * 3, width * 2.5}, rgb(pal.core()), new float[]{0.4f * alpha, 0.6f * alpha, 0.4f * alpha}, 0);
			d.ribbon(FxDraw.STREAK, pts, new double[]{width * 0.3, width, width * 0.3}, rgb(pal.edge()), new float[]{alpha, alpha, alpha}, 0);
		}
	}

	/**
	 * A dash's cutting lines: thin bright streaks drawn along the path as the caster passes. Variants: 0 straight cuts,
	 * 1 thin white lines (with an afterimage), 2 lingering blue trails, 3 circular blade rings along the path, 4 crossing
	 * red cuts.
	 */
	static final class DashLines extends GrandLineFx.Effect {
		final Vec3 start;
		final Vec3 dir;
		final double length;
		final double speed;
		final double width;
		final int variant;
		final SwordPalette pal;
		final double[][] lanes;

		DashLines(int caster, Vec3 start, Vec3 dir, double length, double speed, double width, int variant, SwordPalette pal) {
			super(caster);
			this.start = start;
			this.dir = flat(dir);
			this.length = length;
			this.speed = Math.max(0.5, speed);
			this.width = width;
			this.variant = variant;
			this.pal = pal;
			lanes = new double[variant == 1 ? 5 : 4][2];
			for (double[] l : lanes) {
				l[0] = RANDOM.nextGaussian() * width * 0.35;
				l[1] = RANDOM.nextGaussian() * 0.35;
			}
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		int life() {
			return (int) (length / speed) + (variant == 2 ? 26 : 12);
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (variant == 3 && age * speed <= length) {
				Vec3 at = start.add(dir.scale(age * speed));
				GrandLineFx.add(new SwordShapes.Trail(-1, at, at, rotateY(dir, age * 1.3), width * 0.9, 5, pal, 0.8));
				SwordShapes.motes(at, 0.6, 3, pal, 16);
			}
			if (variant == 4 && age % 1 == 0 && age * speed <= length) {
				Vec3 at = start.add(dir.scale(age * speed));
				cut(at.add(dir.scale(0.6)), dir, 2 + age % 2, 0.9, pal);
			}
			return age < life();
		}

		static Vec3 rotateY(Vec3 v, double a) {
			return SwordShapes.rotate(v, UP, a);
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			double head = Math.min(length, time * speed);
			double travel = length / speed;
			float fade = time <= travel ? 1 : Math.max(0, 1 - (float) ((time - travel) / (life() - travel)));
			Vec3 s = right(dir);
			for (double[] lane : lanes) {
				Vec3 off = s.scale(lane[0]).add(0, lane[1], 0);
				double tail = Math.max(0, head - length * (variant == 2 ? 1.0 : 0.85));
				Vec3 a = start.add(off).add(dir.scale(tail));
				Vec3 b = start.add(off).add(dir.scale(head));
				List<Vec3> pts = List.of(b, b.add(a.subtract(b).scale(0.5)), a);
				double w = variant == 1 ? 0.05 : variant == 2 ? 0.16 : 0.09;
				d.ribbon(FxDraw.GLOW, pts, new double[]{w * 4, w * 3, w}, rgb(pal.core()), new float[]{0.5f * fade, 0.35f * fade, 0}, 0);
				d.ribbon(FxDraw.STREAK, pts, new double[]{w, w * 0.8, w * 0.2}, rgb(pal.edge()), new float[]{fade, 0.7f * fade, 0}, 0);
			}
		}
	}

	/** A translucent copy of the caster's body left where the dash began, fading out. */
	static final class Afterimage extends GrandLineFx.Effect {
		final AbstractClientPlayer player;
		final Vec3 at;
		final float yaw;
		final SwordPalette pal;
		final int life;

		Afterimage(AbstractClientPlayer player, Vec3 at, float yaw, SwordPalette pal, int life) {
			super(-1);
			this.player = player;
			this.at = at;
			this.yaw = yaw;
			this.pal = pal;
			this.life = life;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Minecraft mc = Minecraft.getInstance();
			if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
				return;
			}
			float alpha = 0.55f * (1 - (age + d.partial) / life);
			PlayerModel<AbstractClientPlayer> model = renderer.getModel();
			PoseStack pose = d.pose;
			pose.pushPose();
			pose.translate(at.x - d.camPos.x, at.y - d.camPos.y, at.z - d.camPos.z);
			pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
			pose.scale(-0.9375f, -0.9375f, 0.9375f);
			pose.translate(0, -1.501f, 0);
			model.attackTime = 0;
			model.crouching = false;
			model.riding = false;
			model.young = false;
			model.setupAnim(player, 0.6f, 0.9f, player.tickCount, 0, 0);
			float[] c = FxDraw.rgba(pal.core(), 1);
			model.renderToBuffer(pose, d.buffers.getBuffer(RenderType.entityTranslucentEmissive(player.getSkinTextureLocation())),
					LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.55f + c[0] * 0.45f, 0.55f + c[1] * 0.45f, 0.55f + c[2] * 0.45f, alpha);
			pose.popPose();
		}
	}

	/** A small wind vortex travelling along a line (Triple Vortex shots, the Rising Vortex tornado). */
	static final class MovingVortex extends SwordShapes.Vortex implements GrandLineFx.Projectile {
		final Vec3 start;
		final Vec3 dir;
		final double speed;
		final double range;
		Vec3 stopAt;
		int stopAge = -1;

		MovingVortex(int caster, Vec3 start, Vec3 dir, double speed, double range, double radius, double height, SwordPalette pal, boolean water) {
			super(caster, null, radius, height, 0.55, 400, pal, height > 3 ? 8 : 5, water);
			this.start = start.subtract(0, height > 3 ? 0 : height * 0.4, 0);
			this.dir = dir.normalize();
			this.speed = speed;
			this.range = range;
			this.fadeOut = 6;
		}

		Vec3 centreAt(float time) {
			return stopAt != null ? stopAt.subtract(0, 0.6, 0) : start.add(dir.scale(Math.min(range, speed * time)));
		}

		@Override
		public Vec3 position() {
			return centreAt(age).add(0, 0.6, 0);
		}

		@Override
		public void end(Vec3 at) {
			if (stopAt == null) {
				stopAt = at;
				stopAge = age;
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (stopAt == null && speed * age >= range) {
				end(position());
			}
			if (age % 2 == 0) {
				SwordShapes.motes(centreAt(age).add(0, 0.5, 0), radius * 0.6, 2, pal, 10);
			}
			return stopAge < 0 ? age < 400 : age - stopAge < 6;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			Vec3 c = centreAt(time);
			float fade = stopAge < 0 ? Math.min(1, time / 3f) : Math.max(0, 1 - (time - stopAge) / 6f);
			for (int k = 0; k < strands; k++) {
				int n = 14;
				List<Vec3> pts = new ArrayList<>();
				double[] w = new double[n], thin = new double[n];
				float[] al = new float[n], al2 = new float[n];
				double base = k * Math.PI * 2 / strands + time * 0.8;
				for (int i = 0; i < n; i++) {
					double t = i / (double) (n - 1);
					double y = t * height;
					double rr = radius * (0.35 + flare * 1.3 * t);
					double a = base - t * 4.0;
					pts.add(c.add(Math.cos(a) * rr, y, Math.sin(a) * rr));
					double taper = Math.sin(Math.PI * Math.min(1, (1 - t) * 1.05 + 0.03));
					w[i] = 0.4 * taper * Math.max(0.6, rr / radius);
					thin[i] = 0.09 * taper;
					al[i] = (float) (0.5 * fade * (1 - t * 0.5));
					al2[i] = (float) (0.9 * fade * (1 - t * 0.6));
				}
				d.ribbon(FxDraw.GLOW, pts, w, rgb(pal.core()), al, (float) (time * 0.05));
				d.ribbon(FxDraw.STREAK, pts, thin, rgb(pal.edge()), al2, 0);
			}
			d.billboard(FxDraw.GLOW, true, c.add(0, height * 0.35, 0), radius * 0.8, 0, FxDraw.rgba(pal.core(), 0.25f * fade));
		}
	}

	/** Crimson Grinder: a storm of dark-red cutting arcs wheeling round the caster as they advance. */
	static final class Grinder extends GrandLineFx.Effect {
		final Vec3 fallback;
		final int life;
		final double radius;
		final SwordPalette pal;

		Grinder(int caster, Vec3 fallback, int life, double radius, SwordPalette pal) {
			super(caster);
			this.fallback = fallback;
			this.life = life;
			this.radius = radius;
			this.pal = pal;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Vec3 c = follow(level, caster, fallback, 1);
			if (age < life && age % 2 == 0) {
				double a = RANDOM.nextDouble() * Math.PI * 2;
				Vec3 at = c.add(Math.cos(a) * radius * 0.8, 0.6 + RANDOM.nextDouble() * 1.2, Math.sin(a) * radius * 0.8);
				SwordShapes.sparks(at, new Vec3(-Math.sin(a), 0, Math.cos(a)), 3, 0.35, pal);
			}
			return age < life + 6;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			Vec3 c = follow(level, caster, fallback, d.partial).add(0, 1.0, 0);
			float fade = Math.min(1, time / 3f) * (time < life ? 1 : Math.max(0, 1 - (time - life) / 6f));
			for (int k = 0; k < 6; k++) {
				double tilt = (k - 2.5) * 0.18;
				double base = time * 0.9 + k * 1.05;
				int n = 12;
				List<Vec3> pts = new ArrayList<>();
				double[] w = new double[n], thin = new double[n], dark = new double[n];
				float[] al = new float[n], al2 = new float[n], al3 = new float[n];
				double r = radius * (0.7 + 0.08 * k);
				for (int i = 0; i < n; i++) {
					double t = i / (double) (n - 1);
					double a = base - t * 2.0;
					pts.add(c.add(Math.cos(a) * r, Math.sin(a * 2 + k) * 0.25 + tilt * Math.sin(a) * r, Math.sin(a) * r));
					double taper = Math.sin(Math.PI * Math.min(1, (1 - t) * 1.1 + 0.02));
					w[i] = 0.55 * taper;
					dark[i] = 0.5 * taper;
					thin[i] = 0.1 * taper;
					al[i] = (float) (0.5 * fade * (1 - t));
					al2[i] = (float) (0.95 * fade * (1 - t));
					al3[i] = (float) (0.7 * fade * (1 - t * 0.8));
				}
				d.ribbonSoft(FxDraw.GLOW, pts, dark, rgb(pal.dark()), al3, 0);
				d.ribbon(FxDraw.GLOW, pts, w, rgb(pal.core()), al, 0);
				d.ribbon(FxDraw.STREAK, pts, thin, rgb(pal.edge()), al2, 0);
			}
			d.groundRing(c.subtract(0, 0.95, 0), radius, 0.4, 32, FxDraw.rgba(pal.core(), 0.4f * fade));
		}
	}

	/** Azure Pursuit: blue cutting arcs flicking round the charging caster, and blue trails left along the path. */
	static final class Pursuit extends GrandLineFx.Effect {
		final int life;
		final double size;
		final SwordPalette pal;
		final Deque<Vec3> path = new ArrayDeque<>();

		Pursuit(int caster, int life, double size, SwordPalette pal) {
			super(caster);
			this.life = life;
			this.size = size;
			this.pal = pal;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Entity e = caster(level);
			if (e != null && age < life) {
				path.addFirst(e.position().add(0, 1.0, 0));
				if (age % 1 == 0) {
					Vec3 f = flat(e.getLookAngle());
					double a = RANDOM.nextDouble() * Math.PI * 2;
					Vec3 off = SwordShapes.rotate(right(f), f, a).scale(size * 0.4);
					GrandLineFx.add(new SwordShapes.Trail(-1, e.position().add(0, 1.0, 0).add(off), e.position().add(off), SwordShapes.rotate(f, UP, RANDOM.nextGaussian() * 0.6),
							size * 0.7, RANDOM.nextInt(3), pal, 0.7));
				}
			}
			while (path.size() > 18) {
				path.removeLast();
			}
			return age < life + 26;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			if (path.size() < 2) {
				return;
			}
			float time = age + d.partial;
			float fade = time < life ? 1 : Math.max(0, 1 - (time - life) / 26f);
			List<Vec3> pts = new ArrayList<>(path);
			int n = pts.size();
			for (int lane = -1; lane <= 1; lane++) {
				List<Vec3> lanePts = new ArrayList<>();
				for (Vec3 p : pts) {
					lanePts.add(p.add(0, lane * 0.45, 0));
				}
				double[] w = new double[n], thin = new double[n];
				float[] al = new float[n], al2 = new float[n];
				for (int i = 0; i < n; i++) {
					double t = i / (double) (n - 1);
					w[i] = 0.5 * (1 - t);
					thin[i] = 0.12 * (1 - t);
					al[i] = (float) (0.45 * fade * (1 - t));
					al2[i] = (float) (0.9 * fade * (1 - t));
				}
				d.ribbon(FxDraw.GLOW, lanePts, w, rgb(pal.core()), al, 0);
				d.ribbon(FxDraw.STREAK, lanePts, thin, rgb(pal.edge()), al2, 0);
			}
		}
	}

	/** Ignition Strike: a serpentine dragon of orange fire rising and coiling round the struck enemy, then gone in the blast. */
	static final class DragonBurst extends GrandLineFx.Effect {
		final Vec3 c;
		final Vec3 forward;
		final int life;
		final SwordPalette pal;

		DragonBurst(Vec3 c, Vec3 forward, int life, SwordPalette pal) {
			super(-1);
			this.c = c;
			this.forward = flat(forward);
			this.life = Math.max(6, life);
			this.pal = pal;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (age % 2 == 0) {
				FxSprites.ember(c.add(RANDOM.nextGaussian() * 0.5, RANDOM.nextDouble() * 1.5 - 0.5, RANDOM.nextGaussian() * 0.5), new Vec3(0, 0.06, 0),
						0.3, 10, 0xFFE07A, 0xFF4A0A);
			}
			return age < life;
		}

		Vec3 spine(double s, float time) {
			// The head leads; the body coils twice round the target as it climbs.
			double a = s * Math.PI * 4 + time * 0.25;
			double r = 1.1 - 0.3 * s;
			return c.add(Math.cos(a) * r, -0.9 + (1 - s) * 2.4, Math.sin(a) * r);
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			float grow = Math.min(1, time / (life * 0.6f));
			float fade = time < life - 3 ? 1 : Math.max(0, (life - time) / 3f);
			int n = 22;
			List<Vec3> pts = new ArrayList<>();
			double[] w = new double[n], core = new double[n];
			float[] al = new float[n], al2 = new float[n];
			for (int i = 0; i < n; i++) {
				double s = i / (double) (n - 1);
				double along = 1 - grow + s * grow; // body unfurls from the tail end
				pts.add(spine(1 - along, time));
				double body = 0.55 * Math.sin(Math.PI * Math.min(1, s * 1.25 + 0.05)) + 0.2 * (1 - s);
				w[i] = body * 1.4;
				core[i] = body * 0.5;
				al[i] = (float) (0.7 * fade * (1 - s * 0.6));
				al2[i] = (float) (0.95 * fade * (1 - s * 0.5));
			}
			d.ribbon(FxDraw.GLOW, pts, w, rgb(pal.core()), al, (float) (time * 0.1));
			d.ribbon(FxDraw.STREAK, pts, core, rgb(pal.edge()), al2, 0);
			// The head: a bright flame mass with two horn tongues.
			Vec3 head = pts.get(0);
			d.flame(head, 0.6, 0, age, FxDraw.rgba(pal.core(), fade));
			d.flame(head.add(0, 0.3, 0), 0.45, 0.6, age + 1, FxDraw.rgba(pal.edge(), fade));
			d.flame(head.add(0, 0.3, 0), 0.45, -0.6, age + 2, FxDraw.rgba(pal.edge(), fade));
			d.billboard(FxDraw.GLOW, true, c, 1.4, 0, FxDraw.rgba(pal.core(), 0.3f * fade));
		}
	}

	/** Ember Serpent: a winding flame serpent flying toward the target; ended by the server. */
	static final class Serpent extends GrandLineFx.Effect implements GrandLineFx.Projectile {
		final Vec3 start;
		final Vec3 dir;
		final double speed;
		final double range;
		final double size;
		final SwordPalette pal;
		Vec3 stopAt;
		int stopAge = -1;

		Serpent(int caster, Vec3 start, Vec3 dir, double speed, double range, double size, SwordPalette pal) {
			super(caster);
			this.start = start;
			this.dir = dir.normalize();
			this.speed = speed;
			this.range = range;
			this.size = size;
			this.pal = pal;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		double along(float time) {
			return Math.min(range, speed * time);
		}

		Vec3 at(double s, float time) {
			Vec3 side = SwordShapes.side(dir);
			Vec3 up = side.cross(dir).normalize();
			double wave = Math.sin(s * 1.6 - time * 0.9) * 0.55 * size;
			double wave2 = Math.cos(s * 1.1 - time * 0.7) * 0.3 * size;
			return start.add(dir.scale(s)).add(side.scale(wave)).add(up.scale(wave2));
		}

		@Override
		public Vec3 position() {
			return stopAt != null ? stopAt : at(along(age), age);
		}

		@Override
		public void end(Vec3 at) {
			if (stopAt == null) {
				stopAt = at;
				stopAge = age;
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (stopAt == null && speed * age >= range) {
				end(position());
			}
			if (stopAt == null) {
				FxSprites.ember(position(), dir.scale(-0.05).add(0, 0.04, 0), 0.35, 10, 0xFFE07A, 0xFF4A0A);
			}
			return stopAge < 0 ? age < 300 : age - stopAge < 6;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			double head = stopAt != null ? along(stopAge) : along(time);
			float fade = stopAge < 0 ? Math.min(1, time / 2f) : Math.max(0, 1 - (time - stopAge) / 6f);
			int n = 18;
			double length = Math.min(head, 6.5);
			List<Vec3> pts = new ArrayList<>();
			double[] w = new double[n], core = new double[n];
			float[] al = new float[n], al2 = new float[n];
			for (int i = 0; i < n; i++) {
				double t = i / (double) (n - 1);
				pts.add(at(head - t * length, time));
				double body = size * (0.75 * Math.sin(Math.PI * Math.min(1, t * 1.3 + 0.08)) + 0.15);
				w[i] = body * 1.5;
				core[i] = body * 0.45;
				al[i] = (float) (0.75 * fade * (1 - t * 0.7));
				al2[i] = (float) (fade * (1 - t * 0.8));
			}
			d.ribbon(FxDraw.GLOW, pts, w, rgb(pal.core()), al, (float) (time * 0.15));
			d.ribbon(FxDraw.STREAK, pts, core, rgb(pal.edge()), al2, 0);
			Vec3 h = pts.get(0);
			d.flame(h, 0.7 * size, FlameFx.rollToward(d, dir), age, FxDraw.rgba(pal.core(), fade));
			d.billboard(FxDraw.GLOW, true, h, 0.8 * size, 0, FxDraw.rgba(pal.edge(), 0.6f * fade));
		}
	}

	/** Fading green wisps trailing the Eclipse Cleaver. */
	static final class Wisps extends GrandLineFx.Effect {
		final Vec3 start;
		final Vec3 dir;
		final double speed;
		final double range;
		final SwordPalette pal;

		Wisps(int caster, Vec3 start, Vec3 dir, double speed, double range, SwordPalette pal) {
			super(-1);
			this.start = start;
			this.dir = dir.normalize();
			this.speed = speed;
			this.range = range;
			this.pal = pal;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (speed * age <= range) {
				Vec3 at = start.add(dir.scale(speed * age));
				for (int i = 0; i < FxSprites.count(4); i++) {
					Vec3 p = at.add(RANDOM.nextGaussian() * 0.4, RANDOM.nextGaussian() * 2.0, RANDOM.nextGaussian() * 0.4);
					FxSprites.sprite(FxDraw.GLOW, true, p, dir.scale(-0.05).add(0, 0.03, 0), 0.25, -0.01, -0.002, 0.93, 16, pal.edge(), pal.dark(), 0.8f);
				}
				SwordShapes.sparks(at, dir, 2, 0.2, pal);
			}
			return speed * age <= range + speed * 4;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
		}
	}

	/** The air rippling round the Bisento's pressure wave: faint rings travelling with it. */
	static final class Distortion extends GrandLineFx.Effect {
		final Vec3 start;
		final Vec3 dir;
		final double speed;
		final double range;
		final double size;
		final SwordPalette pal;

		Distortion(int caster, Vec3 start, Vec3 dir, double speed, double range, double size, SwordPalette pal) {
			super(-1);
			this.start = start;
			this.dir = dir.normalize();
			this.speed = speed;
			this.range = range;
			this.size = size;
			this.pal = pal;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age * speed < range + speed * 3;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float time = age + d.partial;
			double along = Math.min(range, time * speed);
			float fade = along < range ? 1 : Math.max(0, 1 - (float) ((time * speed - range) / (speed * 3)));
			Vec3 side = SwordShapes.side(dir);
			for (int k = 0; k < 3; k++) {
				double back = k * 1.1;
				if (along - back < 0) {
					continue;
				}
				Vec3 c = start.add(dir.scale(along - back));
				double r = size * (0.7 + k * 0.25);
				float a = 0.28f * fade * (1 - k * 0.28f);
				int seg = 20;
				for (int i = 0; i < seg; i++) {
					double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
					Vec3 p0 = c.add(side.scale(Math.cos(a0) * r)).add(0, Math.sin(a0) * r, 0);
					Vec3 p1 = c.add(side.scale(Math.cos(a1) * r)).add(0, Math.sin(a1) * r, 0);
					d.ribbon(FxDraw.GLOW, List.of(p0, p1), new double[]{0.18, 0.18}, rgb(pal.edge()), new float[]{a, a}, 0);
				}
			}
		}
	}

	/** Upgraded Seismic Core: a white energy sphere gathering at the weapon's tip above the caster. */
	static final class TipOrb extends GrandLineFx.Effect {
		final int life;
		final SwordPalette pal;

		TipOrb(int caster, int life, SwordPalette pal) {
			super(caster);
			this.life = life;
			this.pal = pal;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Entity e = caster(level);
			if (e != null) {
				Vec3 c = tip(e, 1);
				for (int i = 0; i < FxSprites.count(3); i++) {
					Vec3 from = c.add(new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextGaussian()).normalize().scale(2.2));
					FxSprites.sprite(FxDraw.GLOW, true, from, c.subtract(from).scale(0.18), 0.12, -0.004, 0, 1.0, 6, pal.edge(), pal.core(), 0.9f);
				}
			}
			return age < life + 2;
		}

		static Vec3 tip(Entity e, float partial) {
			return e.getPosition(partial).add(0, e.getBbHeight() + 0.9, 0).add(flat(e.getLookAngle()).scale(0.9));
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity e = caster(level);
			if (e == null) {
				return;
			}
			float t = Math.min(1, (age + d.partial) / life);
			Vec3 c = tip(e, d.partial);
			d.billboard(FxDraw.SHELL, true, c, 0.25 + t * 1.0, (age + d.partial) * 0.1, FxDraw.rgba(pal.edge(), 0.9f));
			d.billboard(FxDraw.GLOW, true, c, 0.4 + t * 1.3, 0, FxDraw.rgba(pal.core(), 0.7f));
		}
	}

	/** Upgraded Seismic Core: the glowing sphere in flight. */
	static final class EnergyOrb extends GrandLineFx.Effect implements GrandLineFx.Projectile {
		final Vec3 start;
		final Vec3 dir;
		final double speed;
		final double range;
		final double size;
		final SwordPalette pal;
		Vec3 stopAt;

		EnergyOrb(int caster, Vec3 start, Vec3 dir, double speed, double range, double size, SwordPalette pal) {
			super(caster);
			this.start = start;
			this.dir = dir.normalize();
			this.speed = speed;
			this.range = range;
			this.size = size;
			this.pal = pal;
		}

		@Override
		boolean followsCaster() {
			return false;
		}

		@Override
		public Vec3 position() {
			return stopAt != null ? stopAt : start.add(dir.scale(Math.min(range, speed * age)));
		}

		@Override
		public void end(Vec3 at) {
			stopAt = at;
			done = true;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			SwordShapes.motes(position(), 0.4, 2, pal, 10);
			return stopAt == null && age < 200;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Vec3 c = start.add(dir.scale(Math.min(range, speed * (age + d.partial))));
			d.billboard(FxDraw.SHELL, true, c, size, (age + d.partial) * 0.1, FxDraw.rgba(pal.edge(), 0.95f));
			d.billboard(FxDraw.GLOW, true, c, size * 1.5, 0, FxDraw.rgba(pal.core(), 0.7f));
			d.ribbon(FxDraw.GLOW, List.of(c, c.subtract(dir.scale(2.5))), new double[]{size * 1.2, 0.05}, rgb(pal.core()), new float[]{0.5f, 0}, 0);
		}
	}

	/** Upgraded Seismic Core's impact: the sphere drags everything in, swells, then detonates in rings, cracks and shells. */
	static final class CoreOrb extends GrandLineFx.Effect {
		final Vec3 c;
		final Vec3 dir;
		final int pull;
		final double radius;
		final double blast;
		final SwordPalette pal;

		CoreOrb(Vec3 c, Vec3 dir, int pull, double radius, double blast, SwordPalette pal) {
			super(-1);
			this.c = c;
			this.dir = dir;
			this.pull = pull;
			this.radius = radius;
			this.blast = blast;
			this.pal = pal;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (age < pull) {
				for (int i = 0; i < FxSprites.count(5); i++) {
					Vec3 from = c.add(new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian() * 0.5, RANDOM.nextGaussian()).normalize().scale(radius));
					FxSprites.sprite(FxDraw.STREAK, true, from, c.subtract(from).scale(0.15), 0.18, -0.005, 0, 1.0, 7, pal.edge(), pal.core(), 0.8f);
				}
			}
			if (age == pull) {
				GrandLineFx.add(new SwordShapes.Shell(c, blast, 6, 20, pal));
				GrandLineFx.add(new SwordShapes.Shockwave(SwordShapes.flat(UP).scale(0).add(c).subtract(0, 1, 0), 0.9, blast + 3, 1.6, pal));
				GrandLineFx.add(new SwordShapes.Shockwave(c.subtract(0, 1, 0), 0.5, blast + 1, 0.8, pal));
				GrandLineFx.add(new SwordShapes.Fractures(c.subtract(0, 1, 0), dir, 8, blast * 0.8, pal, 60, true));
				GrandLineFx.add(new SwordShapes.Flash(c, blast * 0.6, 9, pal.edge(), pal.core()));
				SwordShapes.sparks(c, Vec3.ZERO, 30, 0.7, pal);
				shake(c, 2.8f);
			}
			return age < pull + 1;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = Math.min(1, (age + d.partial) / pull);
			double r = 1.2 + t * t * 2.2;
			d.billboard(FxDraw.SHELL, true, c, r, (age + d.partial) * 0.15, FxDraw.rgba(pal.edge(), 0.95f));
			d.billboard(FxDraw.GLOW, true, c, r * 1.6, 0, FxDraw.rgba(pal.core(), 0.6f));
			// The air bends toward it: rings closing in.
			for (int k = 0; k < 3; k++) {
				double rr = radius * (1 - ((t * 2 + k / 3.0) % 1));
				d.groundRing(c.add(0, (k - 1) * 0.4, 0), rr, 0.2, 32, FxDraw.rgba(pal.edge(), 0.35f));
			}
		}
	}

	/** Tidal Grapple: the thrown trident on a dark tether, sticking, reeling and flying back. */
	static final class Grapple extends GrandLineFx.Effect {
		final Vec3 start;
		final Vec3 dir;
		final double speed;
		final double range;
		Vec3 stuck;
		int target = -1;
		int targetUntil;
		boolean returning;
		int returnAge;
		Vec3 returnFrom;

		Grapple(int caster, Vec3 start, Vec3 dir, double speed, double range) {
			super(caster);
			this.start = start;
			this.dir = dir.normalize();
			this.speed = speed;
			this.range = range;
		}

		void stick(Vec3 at, int kind) {
			stuck = at;
		}

		void follow(int entity, int ticks) {
			target = entity;
			targetUntil = age + ticks;
		}

		void recall() {
			returning = true;
			returnAge = age;
			returnFrom = stuck;
		}

		Vec3 tipAt(ClientLevel level, float partial) {
			float time = age + partial;
			if (returning) {
				Entity e = caster(level);
				Vec3 hand = e == null ? start : GrandLineFx.handPosition(e, partial, false);
				Vec3 from = returnFrom == null ? hand : returnFrom;
				float t = Math.min(1, (time - returnAge) / 5f);
				return from.add(hand.subtract(from).scale(t * t));
			}
			if (target >= 0 && age < targetUntil) {
				Entity t = level.getEntity(target);
				if (t != null) {
					stuck = t.getPosition(partial).add(0, t.getBbHeight() * 0.55, 0);
				}
			}
			return stuck != null ? stuck : start.add(dir.scale(Math.min(range, speed * time)));
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			if (returning) {
				return age - returnAge < 6;
			}
			return age < 80;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity e = caster(level);
			if (e == null) {
				return;
			}
			Vec3 hand = GrandLineFx.handPosition(e, d.partial, false);
			Vec3 tip = tipAt(level, d.partial);
			// The tether: a dark rope with a faint watery sheen, sagging slightly.
			int n = 10;
			List<Vec3> pts = new ArrayList<>();
			double[] w = new double[n], sheen = new double[n];
			float[] al = new float[n], al2 = new float[n];
			double sag = Math.min(0.8, hand.distanceTo(tip) * 0.04);
			for (int i = 0; i < n; i++) {
				double t = i / (double) (n - 1);
				pts.add(hand.add(tip.subtract(hand).scale(t)).add(0, -Math.sin(Math.PI * t) * sag, 0));
				w[i] = 0.09;
				sheen[i] = 0.03;
				al[i] = 0.9f;
				al2[i] = 0.5f;
			}
			d.ribbonSoft(FxDraw.STREAK, pts, w, rgb(0x061826), al, 0);
			d.ribbon(FxDraw.STREAK, pts, sheen, rgb(0x2F9AF0), al2, 0);
			// The trident itself, pointing along its flight (or back toward the hand on return).
			Vec3 point = returning ? hand.subtract(tip) : stuck != null ? tip.subtract(hand) : dir;
			renderTrident(d, tip, point.lengthSqr() < 1e-6 ? dir : point.normalize());
		}

		static void renderTrident(FxDraw d, Vec3 at, Vec3 pointing) {
			Minecraft mc = Minecraft.getInstance();
			PoseStack pose = d.pose;
			pose.pushPose();
			pose.translate(at.x - d.camPos.x, at.y - d.camPos.y, at.z - d.camPos.z);
			float yaw = (float) Math.toDegrees(Math.atan2(pointing.x, pointing.z));
			float pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(pointing.y, -1, 1)));
			pose.mulPose(Axis.YP.rotationDegrees(yaw));
			pose.mulPose(Axis.XP.rotationDegrees(-pitch + 90));
			pose.mulPose(Axis.ZP.rotationDegrees(-45)); // the icon's blade runs corner to corner
			pose.scale(1.6f, 1.6f, 1.6f);
			mc.getItemRenderer().renderStatic(new ItemStack(com.grandline.gear.Gear.TRIDENT), ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
					OverlayTexture.NO_OVERLAY, pose, d.buffers, mc.level, 0);
			pose.popPose();
		}
	}

	/** Abyssal Surge's burst: water ribbons flung outward and up. */
	static final class WaterRibbons extends GrandLineFx.Effect {
		final Vec3 c;
		final double radius;
		final SwordPalette pal;
		final double[] angles = new double[10];

		WaterRibbons(Vec3 c, double radius, SwordPalette pal) {
			super(-1);
			this.c = c;
			this.radius = radius;
			this.pal = pal;
			for (int i = 0; i < angles.length; i++) {
				angles[i] = i * Math.PI * 2 / angles.length + RANDOM.nextGaussian() * 0.2;
			}
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < 16;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / 16f;
			float fade = 1 - t;
			for (double a : angles) {
				int n = 10;
				List<Vec3> pts = new ArrayList<>();
				double[] w = new double[n], thin = new double[n];
				float[] al = new float[n], al2 = new float[n];
				for (int i = 0; i < n; i++) {
					double s = i / (double) (n - 1);
					double r = radius * t * (1 - s * 0.6);
					double y = Math.sin(Math.PI * (1 - s)) * 2.0 * t;
					double twist = a + s * 0.6;
					pts.add(c.add(Math.cos(twist) * r, y, Math.sin(twist) * r));
					w[i] = 0.5 * (1 - s);
					thin[i] = 0.12 * (1 - s);
					al[i] = (float) (0.55 * fade * (1 - s));
					al2[i] = (float) (0.9 * fade * (1 - s));
				}
				d.ribbon(FxDraw.GLOW, pts, w, rgb(pal.core()), al, 0);
				d.ribbon(FxDraw.STREAK, pts, thin, rgb(pal.edge()), al2, 0);
			}
		}
	}

	/** A charging glow on the blade (Saber's crimson charge, the Eclipse Blade's gathering aura while held). */
	static final class BladeCharge extends GrandLineFx.Effect {
		int life;
		final SwordPalette pal;
		final boolean big;
		double level = 1;

		BladeCharge(int caster, int life, SwordPalette pal, boolean big) {
			super(caster);
			this.life = life;
			this.pal = pal;
			this.big = big;
		}

		void refresh(double charge) {
			level = 0.3 + charge * 0.7;
			life = age + 6;
		}

		@Override
		boolean tick(ClientLevel lv) {
			age++;
			Entity e = caster(lv);
			if (e != null) {
				Vec3 hand = GrandLineFx.handPosition(e, 1, false);
				for (int i = 0; i < FxSprites.count(big ? 4 : 2); i++) {
					Vec3 from = hand.add(new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextGaussian()).normalize().scale(1.4 * level));
					FxSprites.sprite(FxDraw.GLOW, true, from, hand.subtract(from).scale(0.2), 0.1, -0.003, 0, 1.0, 5, pal.edge(), pal.core(), 0.9f);
				}
			}
			return age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel lv) {
			Entity e = caster(lv);
			if (e == null) {
				return;
			}
			Vec3 hand = GrandLineFx.handPosition(e, d.partial, false);
			float pulse = 0.75f + 0.25f * Mth.sin((age + d.partial) * 1.4f);
			d.billboard(FxDraw.GLOW, true, hand, 0.55 * level * (big ? 1.4 : 1), 0, FxDraw.rgba(pal.core(), 0.8f * pulse));
			d.billboard(FxDraw.SPARK, true, hand, 0.4 * level, (age + d.partial) * 0.2, FxDraw.rgba(pal.edge(), 0.9f * pulse));
			if (pal.darkCore()) {
				d.billboardSoft(FxDraw.GLOW, e.getPosition(d.partial).add(0, 1, 0), 1.2 * level, 0, FxDraw.rgba(pal.dark(), 0.25f * pulse));
			}
		}
	}

	/** Emerald Blade Storm: crossing emerald blades flashing in front of the caster, then the finishing flash. */
	static final class BladeStorm extends GrandLineFx.Effect {
		final Vec3 forward;
		final int life;
		final double reach;
		final double width;
		final SwordPalette pal;

		BladeStorm(int caster, Vec3 forward, int life, double reach, double width, SwordPalette pal) {
			super(caster);
			this.forward = flat(forward);
			this.life = life;
			this.reach = reach;
			this.width = width;
			this.pal = pal;
		}

		@Override
		boolean tick(ClientLevel level) {
			age++;
			Entity e = caster(level);
			if (e != null && age <= life) {
				Vec3 f = flat(e.getLookAngle());
				Vec3 base = e.position().add(0, 1.1, 0);
				for (int i = 0; i < 3; i++) {
					Vec3 at = base.add(f.scale(1.2 + RANDOM.nextDouble() * (reach - 1.2))).add(right(f).scale(RANDOM.nextGaussian() * width * 0.3))
							.add(0, RANDOM.nextGaussian() * 0.5, 0);
					double a = RANDOM.nextDouble() * Math.PI;
					Vec3 axis = right(f).scale(Math.cos(a)).add(UP.scale(Math.sin(a)));
					GrandLineFx.add(new Streak(at.subtract(axis.scale(1.4)), at.add(axis.scale(1.4)), 0.2, pal, 5));
				}
				SwordShapes.sparks(base.add(f.scale(reach * 0.5)), f, 4, 0.35, pal);
			}
			return age < life + 2;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			Entity e = caster(level);
			if (e == null) {
				return;
			}
			Vec3 f = flat(e.getLookAngle());
			Vec3 c = e.getPosition(d.partial).add(0, 1.1, 0).add(f.scale(reach * 0.5));
			float fade = Math.max(0, 1 - (age + d.partial) / (life + 2f));
			d.billboardSoft(FxDraw.GLOW, c, reach * 0.55, 0, FxDraw.rgba(pal.dark(), 0.35f * fade));
			d.billboard(FxDraw.GLOW, true, c, reach * 0.4, 0, FxDraw.rgba(pal.core(), 0.3f * fade));
		}
	}
}
