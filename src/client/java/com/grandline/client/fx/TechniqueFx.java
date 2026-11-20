package com.grandline.client.fx;

import com.grandline.network.payload.FxEventPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.grandline.client.fx.SwordShapes.RANDOM;
import static com.grandline.client.fx.SwordShapes.UP;
import static com.grandline.client.fx.SwordShapes.rgb;

/**
 * Technique visuals: Dash (white wind streaks, a fading afterimage, small air rings, dust on the ground), Flash Step
 * (a flash, ring and afterimage where you left; a burst of wind ribbons, a ripple and a ring where you arrive), Aura
 * (a dark pulse on and a fading wisp off; the coating itself is the AuraLayer), Instinct (a translucent ring on, the
 * evasion afterimage and streaks on a dodge). Light and short-lived.
 */
final class TechniqueFx {
	static final SwordPalette WIND = new SwordPalette(0xFFFFFF, 0xDDEBFF, 0x2A3442, false, 0xFFFFFF, 0xC8D8F0);
	static final SwordPalette DARK = new SwordPalette(0x8E96A8, 0x3A4050, 0x07080B, true, 0xA0A8B8, 0x16181E);
	static final SwordPalette SENSE = new SwordPalette(0xF4FBFF, 0x9FD4FF, 0x10202E, false, 0xFFFFFF, 0x9FD4FF);

	private TechniqueFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("tq:");
	}

	static void handle(FxEventPayload p, ClientLevel level, Entity caster, boolean own) {
		Vec3 pos = p.pos();
		Vec3 dir = p.dir().lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : p.dir().normalize();
		switch (p.effect()) {
			case "tq:dash" -> {
				GrandLineFx.add(new SwordFx.DashLines(p.caster(), pos, dir, Math.max(0.5, p.range()), p.speed(), 1.0, 1, WIND));
				if (caster instanceof AbstractClientPlayer pl) {
					GrandLineFx.add(new SwordFx.Afterimage(pl, pos.subtract(0, 0.9, 0), pl.getYRot(), WIND, 8));
				}
				GrandLineFx.add(new AirRing(pos, dir, 0.5, 1.4, 7, WIND));
				GrandLineFx.add(new AirRing(pos.add(dir.scale(0.9)), dir, 0.3, 1.0, 6, WIND));
				if (p.size() > 0.5) {
					SwordShapes.dust(pos.subtract(0, 0.85, 0), 0.3, 4, 0xB8B0A0);
				}
			}
			case "tq:flash_out" -> {
				GrandLineFx.add(new SwordShapes.Flash(pos, 1.3, 5, 0xFFFFFF, 0xCFE3FF));
				GrandLineFx.add(new SwordShapes.Shockwave(pos.subtract(0, 0.85, 0), 0.45, 2.4, 0.5, WIND));
				GrandLineFx.add(new AirRing(pos, dir, 0.4, 2.2, 8, WIND));
				if (caster instanceof AbstractClientPlayer pl) {
					GrandLineFx.add(new SwordFx.Afterimage(pl, pos.subtract(0, 0.9, 0), p.size(), WIND, 10));
				}
				Vec3 side = SwordShapes.side(dir);
				for (int i = 0; i < 5; i++) {
					Vec3 off = side.scale(RANDOM.nextGaussian() * 0.35).add(0, RANDOM.nextGaussian() * 0.45, 0);
					Vec3 a = pos.add(off).add(dir.scale(-0.4));
					GrandLineFx.add(new SwordFx.Streak(a, a.add(dir.scale(2.2 + RANDOM.nextDouble() * 1.2)), 0.08, WIND, 7));
				}
			}
			case "tq:flash_in" -> {
				GrandLineFx.add(new WindBurst(pos, 0.9, 9, WIND));
				GrandLineFx.add(new AirRing(pos, UP, 0.3, 1.8, 7, WIND));
				GrandLineFx.add(new SwordShapes.Shockwave(pos.subtract(0, 0.85, 0), 0.35, 1.6, 0.35, WIND));
				GrandLineFx.add(new SwordShapes.Flash(pos, 0.7, 4, 0xFFFFFF, 0xCFE3FF));
			}
			case "tq:aura_on" -> {
				GrandLineFx.add(new DarkPulse(pos, 1.6, 9));
				for (int i = 0; i < FxSprites.count(10); i++) {
					Vec3 v = new Vec3(RANDOM.nextGaussian(), RANDOM.nextDouble() * 0.8, RANDOM.nextGaussian()).normalize().scale(0.12);
					FxSprites.smoke(pos.add(v.scale(3)), v, 0.35, 14, 0x0B0C10);
				}
			}
			case "tq:aura_off" -> {
				for (int i = 0; i < FxSprites.count(6); i++) {
					FxSprites.smoke(pos.add(RANDOM.nextGaussian() * 0.35, RANDOM.nextGaussian() * 0.5, RANDOM.nextGaussian() * 0.35),
							new Vec3(0, 0.03, 0), 0.25, 12, 0x2A2E36);
				}
			}
			case "tq:instinct_on" -> {
				GrandLineFx.add(new SwordShapes.Shockwave(pos, 0.9, 7, 0.9, SENSE));
				if (own) {
					com.grandline.client.technique.InstinctSense.pulse();
				}
			}
			case "tq:instinct_off" -> GrandLineFx.add(new SwordShapes.Shockwave(pos, 0.5, 2.5, 0.4, SENSE));
			case "tq:dodge" -> {
				if (caster instanceof AbstractClientPlayer pl) {
					Vec3 feet = pos.subtract(0, 0.9, 0);
					Vec3 side = SwordShapes.side(dir.lengthSqr() < 1e-6 ? pl.getLookAngle() : dir);
					GrandLineFx.add(new SwordFx.Afterimage(pl, feet.add(side.scale(0.45)), p.size(), WIND, 6));
					GrandLineFx.add(new SwordFx.Afterimage(pl, feet.add(side.scale(-0.45)), p.size(), WIND, 4));
					for (int i = 0; i < 4; i++) {
						Vec3 a = pos.add(side.scale(RANDOM.nextGaussian() * 0.5)).add(0, RANDOM.nextGaussian() * 0.5, 0);
						GrandLineFx.add(new SwordFx.Streak(a, a.add(side.scale(i % 2 == 0 ? 1.4 : -1.4)), 0.06, WIND, 5));
					}
				}
			}
			case "tq:intangible" -> {
				for (int i = 0; i < FxSprites.count(8); i++) {
					Vec3 v = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian() * 0.5, RANDOM.nextGaussian()).normalize().scale(0.08);
					FxSprites.sprite(FxDraw.CLOUD, false, pos.add(v.scale(4)), v, 0.3, 0.03, -0.002, 0.9, 12, 0xE8ECF2, 0xB8C0CC, 0.6f);
				}
			}
			default -> { }
		}
	}

	/** A thin ring of air expanding around the travel direction. */
	static final class AirRing extends GrandLineFx.Effect {
		final Vec3 c;
		final Vec3 axis;
		final double r0;
		final double r1;
		final int life;
		final SwordPalette pal;

		AirRing(Vec3 c, Vec3 axis, double r0, double r1, int life, SwordPalette pal) {
			super(-1);
			this.c = c;
			this.axis = axis.normalize();
			this.r0 = r0;
			this.r1 = r1;
			this.life = life;
			this.pal = pal;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / life;
			double r = r0 + (r1 - r0) * (1 - (1 - t) * (1 - t));
			float a = 0.55f * (1 - t);
			Vec3 u = SwordShapes.side(axis);
			Vec3 v = axis.cross(u).normalize();
			int seg = 20;
			for (int i = 0; i < seg; i++) {
				double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
				Vec3 p0 = c.add(u.scale(Math.cos(a0) * r)).add(v.scale(Math.sin(a0) * r));
				Vec3 p1 = c.add(u.scale(Math.cos(a1) * r)).add(v.scale(Math.sin(a1) * r));
				d.ribbon(FxDraw.STREAK, List.of(p0, p1), new double[]{0.07, 0.07}, rgb(pal.edge()), new float[]{a, a}, 0);
			}
		}
	}

	/** A compact burst of white wind ribbons curling outward (Flash Step's arrival). */
	static final class WindBurst extends GrandLineFx.Effect {
		final Vec3 c;
		final double radius;
		final int life;
		final SwordPalette pal;

		WindBurst(Vec3 c, double radius, int life, SwordPalette pal) {
			super(-1);
			this.c = c;
			this.radius = radius;
			this.life = life;
			this.pal = pal;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / life;
			float fade = (float) Math.pow(1 - t, 1.3);
			SwordShapes.ribbonBall(d, c, radius * (0.6 + t * 1.2), (age + d.partial) * 0.7, 6, pal, fade);
		}
	}

	/** A dark pulse swelling from the body (Aura switching on), drawn with ordinary transparency so it darkens. */
	static final class DarkPulse extends GrandLineFx.Effect {
		final Vec3 c;
		final double radius;
		final int life;

		DarkPulse(Vec3 c, double radius, int life) {
			super(-1);
			this.c = c;
			this.radius = radius;
			this.life = life;
		}

		@Override
		boolean tick(ClientLevel level) {
			return ++age < life;
		}

		@Override
		void render(FxDraw d, ClientLevel level) {
			float t = (age + d.partial) / life;
			double r = radius * (0.4 + 0.8 * t);
			float a = 0.6f * (1 - t);
			d.billboardSoft(FxDraw.SHELL, c, r, 0, FxDraw.rgba(0x07080B, a));
			d.billboardSoft(FxDraw.GLOW, c, r * 0.7, 0, FxDraw.rgba(0x101218, a * 0.6f));
			d.groundRing(c.subtract(0, 0.95, 0), r, 0.3, 24, FxDraw.rgba(0x6E7688, a));
		}
	}
}
