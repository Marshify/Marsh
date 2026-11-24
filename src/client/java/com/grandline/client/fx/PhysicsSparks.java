package com.grandline.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * One batch of physical sparks or fragments (reusable): gravity, drag, a damped bounce off block tops (cosmetic, client
 * only), drawn velocity-stretched (a short glowing streak along the motion, longer the faster it goes), cooling from
 * {@code hot} to {@code cool}. Count follows the quality preset; the batch ends when every spark has died.
 */
public final class PhysicsSparks extends GrandLineFx.Effect {
	private static final class Spark {
		Vec3 pos, prev, vel;
		int life, max;
		final double size;

		Spark(Vec3 pos, Vec3 vel, int life, double size) {
			this.pos = pos;
			this.prev = pos;
			this.vel = vel;
			this.life = life;
			this.max = life;
			this.size = size;
		}
	}

	private final List<Spark> sparks = new ArrayList<>();
	private final int hot, cool;
	private final boolean glow;

	/**
	 * {@code count} sparks from {@code at}, thrown round {@code dir} inside a cone of {@code spread} (0 straight, 1
	 * hemisphere) at {@code speed} blocks/tick; {@code glow}: additive hot sparks, otherwise solid fragments.
	 */
	public PhysicsSparks(ClientLevel level, Vec3 at, Vec3 dir, int count, double spread, double speed, int life, double size, int hot, int cool,
			boolean glow) {
		super(-1);
		this.hot = hot;
		this.cool = cool;
		this.glow = glow;
		RandomSource r = level.random;
		Vec3 d = dir.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : dir.normalize();
		for (int i = FxSprites.count(count); i > 0; i--) {
			Vec3 v = d.add(FxSprites.randomDir(r).scale(spread)).normalize().scale(speed * (0.5 + r.nextDouble()));
			sparks.add(new Spark(at, v, life / 2 + r.nextInt(life), size * (0.7 + r.nextDouble() * 0.6)));
		}
	}

	@Override
	boolean tick(ClientLevel level) {
		age++;
		for (Spark s : sparks) {
			s.prev = s.pos;
			s.vel = s.vel.add(0, -0.045, 0).scale(0.94);
			Vec3 next = s.pos.add(s.vel);
			BlockPos below = BlockPos.containing(next);
			var shape = level.getBlockState(below).getCollisionShape(level, below);
			if (s.vel.y < 0 && !shape.isEmpty()) {
				double top = below.getY() + shape.max(Direction.Axis.Y);
				if (next.y < top) {
					next = new Vec3(next.x, top + 0.01, next.z);
					s.vel = new Vec3(s.vel.x * 0.55, -s.vel.y * 0.35, s.vel.z * 0.55);
				}
			}
			s.pos = next;
			s.life--;
		}
		sparks.removeIf(s -> s.life <= 0);
		return !sparks.isEmpty();
	}

	@Override
	void render(FxDraw d, ClientLevel level) {
		for (Spark s : sparks) {
			Vec3 at = s.prev.lerp(s.pos, d.partial);
			if (at.distanceToSqr(d.cameraPosition()) > 64 * 64) {
				continue;
			}
			float t = 1 - s.life / (float) s.max;
			int c = FxSprites.lerpColor(hot, cool, t);
			Vec3 tail = at.subtract(s.vel.scale(1.6));
			float a = (1 - t) * 0.95f;
			d.ribbon(FxDraw.STREAK, List.of(at, tail), new double[]{s.size, s.size * 0.3}, FxDraw.rgba(c, 1f), new float[]{a, 0}, 0);
			if (!glow) {
				d.billboardSoft(FxDraw.CLOUD, at, s.size * 0.8, 0, FxDraw.rgba(c, a));
			}
		}
	}
}
