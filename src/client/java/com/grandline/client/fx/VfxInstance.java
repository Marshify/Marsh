package com.grandline.client.fx;

import com.grandline.client.settings.CameraShake;
import com.grandline.client.settings.ClientSettings;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * One playing effect from the {@link VfxLibrary}: every layer runs on its own delay and life inside the instance,
 * so sprites, flipbooks, particle emitters, rings, beams, ribbons, slashes, meshes and the procedural built-ins of the
 * fruit reworks combine into one multilayered animation. Purely client-side and cosmetic.
 *
 * <p>Rendering uses Grand Line's existing render types: plain textured additive/translucent quads (vanilla
 * position-colour-texture shader, so Iris/BSL shader packs draw them normally) and, for mesh and slash layers with a
 * {@code material}, the WIND/METAL/FIRE/SMOKE surface shaders with their built-in shader-pack fallback. Quality
 * presets scale particle counts, mesh detail and a global particle budget; they never touch gameplay. Instances are
 * culled by render distance and behind the camera, and particles are pooled.
 */
public final class VfxInstance extends GrandLineFx.Effect {
	public static final Set<String> TYPES = Set.of("sprite", "particles", "ring", "beam", "ribbon", "mesh", "slash", "builtin", "distortion", "sound",
			"shake");

	/** How an effect is played: where, which way, how big, in which colours, attached to what. */
	public static final class Params {
		public Vec3 origin = Vec3.ZERO, direction = new Vec3(0, 0, 1), target;
		public float scale = 1, intensity = 1, speed = 1;
		public Integer primary, secondary;
		public int anchor = -1;

		public static Params at(Vec3 origin) {
			Params p = new Params();
			p.origin = origin;
			return p;
		}

		public Params dir(Vec3 d) {
			if (d != null && d.lengthSqr() > 1e-6) {
				direction = d.normalize();
			}
			return this;
		}

		public Params scale(float s) {
			scale = s;
			return this;
		}

		public Params colors(Integer a, Integer b) {
			primary = a;
			secondary = b;
			return this;
		}

		public Params anchor(int entityId) {
			anchor = entityId;
			return this;
		}

		public Params target(Vec3 t) {
			target = t;
			return this;
		}

		public Params intensity(float i) {
			intensity = i;
			return this;
		}

		public Params speed(float s) {
			speed = s;
			return this;
		}
	}

	private static final class P {
		Vec3 pos, prev, vel;
		int age, life, layer;
		float spin, roll, seed;
	}

	private static final ArrayDeque<P> POOL = new ArrayDeque<>();
	static int activeInstances, activeParticles;
	private static long soundTick;
	private static int soundsThisTick;

	final VfxLibrary.Definition def;
	final Params params;
	final int primary, secondary, length;
	final List<P> particles = new ArrayList<>();
	final List<List<Vec3>> trails = new ArrayList<>();
	Vec3 origin, prevOrigin;
	final double seed = Math.random() * 100;

	private VfxInstance(VfxLibrary.Definition def, Params params) {
		super(-1);
		this.def = def;
		this.params = params;
		this.primary = params.primary != null ? params.primary : def.primary();
		this.secondary = params.secondary != null ? params.secondary : def.secondary();
		this.length = Math.max(1, Math.round(def.length() / Math.max(0.1f, params.speed)));
		this.origin = params.origin;
		this.prevOrigin = params.origin;
		for (int i = 0; i < def.layers().size(); i++) {
			trails.add(new ArrayList<>());
		}
		activeInstances++;
	}

	/** Plays effect {@code id}; returns the instance, or null if unknown, out of range or over budget. */
	public static VfxInstance play(String id, Params params) {
		VfxLibrary.Definition def = VfxLibrary.get(id);
		Minecraft mc = Minecraft.getInstance();
		if (def == null || mc.level == null) {
			return null;
		}
		Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
		if (cam.distanceTo(params.origin) > def.renderDistance() || activeInstances >= maxInstances()) {
			return null;
		}
		VfxInstance v = new VfxInstance(def, params);
		GrandLineFx.add(v);
		return v;
	}

	/** Particle budget for the current preset (Low, Medium, High, Ultra). */
	public static int particleBudget() {
		return switch (ClientSettings.get().fxDensity) {
			case 0 -> 400;
			case 1 -> 900;
			case 2 -> 1600;
			default -> 3000;
		};
	}

	static int maxInstances() {
		return 48 + 32 * ClientSettings.get().fxDensity;
	}

	public static int activeInstances() {
		return activeInstances;
	}

	public static int activeParticles() {
		return activeParticles;
	}

	/** Normalised time of a layer (0..1), or -1 outside its window. */
	float layerTime(VfxLibrary.Layer l, float a) {
		float delay = l.delay() / Math.max(0.1f, params.speed), life = Math.max(1, l.life() / Math.max(0.1f, params.speed));
		float t = (a - delay) / life;
		return t < 0 || t > 1 ? -1 : t;
	}

	Vec3 place(VfxLibrary.Layer l, Vec3 base) {
		float[] o = l.offset();
		if (o[0] == 0 && o[1] == 0 && o[2] == 0) {
			return base;
		}
		Vec3 f = params.direction, up = new Vec3(0, 1, 0);
		Vec3 right = f.cross(up);
		right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
		Vec3 u = right.cross(f).normalize();
		return base.add(right.scale(o[0] * params.scale)).add(u.scale(o[1] * params.scale)).add(f.scale(o[2] * params.scale));
	}

	float[] rgba(VfxLibrary.Layer l, float t) {
		float[] c = l.colour().at(t, primary, secondary);
		c[3] = Mth.clamp(c[3] * l.alpha().at(t) * Math.min(1, params.intensity), 0, 1);
		return c;
	}

	// ------------------------------------------------------------------------------------------------ tick

	@Override
	boolean tick(ClientLevel level) {
		age++;
		prevOrigin = origin;
		if (params.anchor >= 0) {
			Entity e = level.getEntity(params.anchor);
			if (e != null) {
				origin = e.position().add(0, e.getBbHeight() * 0.5, 0);
			}
		}
		RandomSource r = level.random;
		for (int li = 0; li < def.layers().size(); li++) {
			VfxLibrary.Layer l = def.layers().get(li);
			int start = Math.round(l.delay() / Math.max(0.1f, params.speed));
			int end = start + Math.round(l.life() / Math.max(0.1f, params.speed));
			if (age == start + 1) {
				once(l, li, level);
			}
			if (l.type().equals("particles") && age > start && age <= end) {
				emit(l, li, age - start, r);
			}
			if (l.type().equals("ribbon") && age > start && age <= end) {
				List<Vec3> tr = trails.get(li);
				tr.add(0, place(l, origin));
				while (tr.size() > l.i("points", 12)) {
					tr.remove(tr.size() - 1);
				}
			}
		}
		for (int i = particles.size() - 1; i >= 0; i--) {
			P p = particles.get(i);
			VfxLibrary.Layer l = def.layers().get(p.layer);
			if (++p.age >= p.life) {
				particles.remove(i);
				POOL.push(p);
				activeParticles--;
				continue;
			}
			p.prev = p.pos;
			p.vel = p.vel.scale(l.f("drag", 0.96f)).add(0, -l.f("gravity", 0), 0);
			Vec3 next = p.pos.add(p.vel);
			if (l.b("collide", false) && !level.getBlockState(BlockPos.containing(next)).getCollisionShape(level, BlockPos.containing(next)).isEmpty()) {
				p.vel = new Vec3(p.vel.x * 0.5, -p.vel.y * l.f("bounce", 0.3f), p.vel.z * 0.5);
				next = p.pos;
			}
			p.pos = next;
			p.roll += p.spin;
		}
		boolean alive = age < length || !particles.isEmpty();
		if (!alive) {
			cleanup();
		}
		return alive;
	}

	@Override
	void cancel() {
		cleanup();
		done = true;
	}

	boolean cleaned;

	void cleanup() {
		if (cleaned) {
			return;
		}
		cleaned = true;
		activeInstances--;
		activeParticles -= particles.size();
		particles.forEach(POOL::push);
		particles.clear();
	}

	void emit(VfxLibrary.Layer l, int li, int t, RandomSource r) {
		int n = t == 1 ? l.i("count", 0) : 0;
		float rate = l.f("rate", 0);
		if (rate > 0) {
			n += (int) rate + (r.nextFloat() < rate - (int) rate ? 1 : 0);
		}
		n = FxSprites.count(n);
		float[] life = l.range("lifetime", 10, 20), speed = l.range("speed", 0.05f, 0.15f);
		float spread = l.f("spread", 1), radius = l.f("radius", 0) * params.scale;
		String shape = l.s("shape", "point");
		Vec3 base = place(l, origin), dir = params.direction;
		Vec3[] b = WindMesh.basis(l.s("emit_axis", "direction").equals("up") ? new Vec3(0, 1, 0) : dir);
		Vec3 axis = l.s("emit_axis", "direction").equals("up") ? new Vec3(0, 1, 0) : dir;
		for (int i = 0; i < n && activeParticles < particleBudget(); i++) {
			P p = POOL.isEmpty() ? new P() : POOL.pop();
			Vec3 rnd = FxSprites.randomDir(r);
			Vec3 start = base;
			Vec3 v;
			switch (shape) {
				case "ring", "disc" -> {
					double a = r.nextDouble() * Math.PI * 2;
					double rr = shape.equals("ring") ? radius : radius * Math.sqrt(r.nextDouble());
					Vec3 out = b[0].scale(Math.cos(a)).add(b[1].scale(Math.sin(a)));
					start = base.add(out.scale(rr));
					v = out.scale(1 - spread * 0.5).add(axis.scale(spread * 0.5)).add(rnd.scale(0.15)).normalize();
				}
				case "sphere" -> {
					start = base.add(rnd.scale(radius * Math.cbrt(r.nextDouble())));
					v = rnd;
				}
				default -> v = axis.add(rnd.scale(spread)).normalize();
			}
			if (l.b("inward", false)) {
				v = v.scale(-1);
			}
			p.pos = start;
			p.prev = start;
			p.vel = v.scale((speed[0] + r.nextFloat() * (speed[1] - speed[0])) * params.scale);
			p.age = 0;
			p.life = Math.max(2, Math.round(life[0] + r.nextFloat() * (life[1] - life[0])));
			p.layer = li;
			p.roll = r.nextFloat() * 6.28f;
			p.spin = (r.nextFloat() - 0.5f) * l.f("spin", 0) * 0.2f;
			p.seed = r.nextFloat();
			particles.add(p);
			activeParticles++;
		}
	}

	/** One-shot layers: procedural built-ins, sound and shake fire when their delay is reached. */
	void once(VfxLibrary.Layer l, int li, ClientLevel level) {
		Vec3 at = place(l, origin);
		float s = params.scale;
		switch (l.type()) {
			case "sound" -> {
				long t = level.getGameTime();
				if (t != soundTick) {
					soundTick = t;
					soundsThisTick = 0;
				}
				if (++soundsThisTick <= 3) {
					ResourceLocation id = ResourceLocation.tryParse(l.s("sound", "minecraft:entity.generic.explode"));
					if (id != null) {
						level.playLocalSound(at.x, at.y, at.z, SoundEvent.createVariableRangeEvent(id), SoundSource.PLAYERS, l.f("volume", 1) / soundsThisTick,
								l.f("pitch", 1) * (0.95f + level.random.nextFloat() * 0.1f), false);
					}
				}
			}
			case "shake" -> {
				Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
				CameraShake.impact(cam.distanceTo(at), l.f("degrees", 1) * params.intensity);
			}
			case "builtin" -> builtin(l, at, s, level);
			default -> { }
		}
	}

	void builtin(VfxLibrary.Layer l, Vec3 at, float s, ClientLevel level) {
		Vec3 dir = params.direction;
		int colour = l.colour().at(0, primary, secondary) != null ? rgb(l.colour().at(0, primary, secondary)) : 0xFFFFFF;
		switch (l.s("effect", "")) {
			case "explosion" -> ExplosionFx.spawn(level, at, l.s("shape", "radial").equals("vertical") ? new Vec3(0, 1, 0) : dir,
					new ExplosionFx.Spec(ExplosionFx.Shape.valueOf(l.s("shape", "radial").toUpperCase()), l.f("radius", 3) * s,
							l.f("power", 0.5f) * params.intensity, l.i("shockwaves", 1), l.b("scorch", false), l.b("cracks", false), l.f("shake", 0.6f)));
			case "ground" -> SpringFx.ground(level, at, l.f("radius", 1.5f) * s, l.i("count", 8));
			case "sparks" -> GrandLineFx.add(new PhysicsSparks(level, at, dir, l.i("count", 8), l.f("spread", 0.8f), l.f("speed", 0.3f) * s,
					l.i("lifetime", 12), l.f("size", 0.05f) * s, colour, rgb(l.colour().at(1, primary, secondary)), l.b("glow", true)));
			case "flash" -> GrandLineFx.add(new BladeFx.Flash(at, l.f("radius", 0.5f) * s, l.i("lifetime", 2)));
			case "crater" -> GrandLineFx.add(new SpringFx.Crater(level, at, l.f("radius", 2) * s));
			case "scorch" -> GrandLineFx.add(new ExplosionFx.Scorch(at.add(0, 0.02, 0), l.f("radius", 1.5f) * s, l.i("lifetime", 120)));
			case "crack" -> GrandLineFx.add(new SpikeFx.Crack(at.add(0, 0.02, 0), l.f("radius", 1.5f) * s, l.i("lifetime", 60)));
			case "spike" -> GrandLineFx.add(new SpikeFx.Rise(at, l.f("height", 3) * s, l.f("radius", 0.6f) * s, l.i("lifetime", 30),
					level.random.nextFloat() * 6.28f, 0));
			case "pressure_ring" -> GrandLineFx.add(new SpringFx.Ring(at, l.s("facing", "up").equals("up") ? new Vec3(0, 1, 0) : dir,
					l.f("radius", 2) * s, l.i("lifetime", 10), colour, l.b("wall", false)));
			default -> { } // smoke_volume / vortex are continuous and drawn in render()
		}
	}

	static int rgb(float[] c) {
		return (int) (c[0] * 255) << 16 | (int) (c[1] * 255) << 8 | (int) (c[2] * 255);
	}

	// ------------------------------------------------------------------------------------------------ render

	@Override
	void render(FxDraw d, ClientLevel level) {
		float a = age + d.partial;
		Vec3 o = prevOrigin.lerp(origin, d.partial);
		Vec3 cam = d.cameraPosition();
		double dist = cam.distanceTo(o);
		if (dist > def.renderDistance()) {
			return;
		}
		// Behind-the-camera culling (the effect's reach is about 6 blocks × scale).
		Vec3 fwd = d.cameraUp().cross(d.cameraRight()).normalize().scale(-1);
		double reach = 6 * params.scale + 4;
		if (o.subtract(cam).dot(fwd) < -reach && dist > reach) {
			return;
		}
		for (int li = 0; li < def.layers().size(); li++) {
			VfxLibrary.Layer l = def.layers().get(li);
			float t = layerTime(l, a);
			if (t < 0 && !l.type().equals("particles")) {
				continue;
			}
			Vec3 at = place(l, o);
			switch (l.type()) {
				case "sprite" -> sprite(d, l, at, t, a);
				case "ring" -> ring(d, l, at, t, a);
				case "beam" -> beam(d, l, at, t, a);
				case "ribbon" -> ribbon(d, l, li, t, a);
				case "mesh" -> mesh(d, l, at, t, a);
				case "slash" -> slash(d, l, at, t, a);
				case "distortion" -> distortion(d, l, at, t, a);
				case "builtin" -> continuous(d, l, at, t, a);
				default -> { }
			}
		}
		for (P p : particles) {
			particle(d, def.layers().get(p.layer), p);
		}
	}

	float[] frameUv(VfxLibrary.Layer l, float t, float ticks, float seed) {
		int frames = l.columns() * l.rows();
		if (frames <= 1) {
			return new float[]{0, 1, 0, 1};
		}
		int f = l.frameTime() > 0 ? (int) (ticks / l.frameTime() + seed * frames) % frames : Math.min(frames - 1, (int) (t * frames));
		int cx = f % l.columns(), cy = f / l.columns();
		return new float[]{cx / (float) l.columns(), (cx + 1) / (float) l.columns(), cy / (float) l.rows(), (cy + 1) / (float) l.rows()};
	}

	Vec3[] facingBasis(VfxLibrary.Layer l, FxDraw d) {
		return switch (l.facing()) {
			case "up" -> WindMesh.basis(new Vec3(0, 1, 0));
			case "direction" -> WindMesh.basis(params.direction);
			default -> new Vec3[]{d.cameraRight(), d.cameraUp()};
		};
	}

	void sprite(FxDraw d, VfxLibrary.Layer l, Vec3 at, float t, float a) {
		if (l.texture() == null) {
			return;
		}
		double half = l.size().at(t) * params.scale * 0.5;
		float[] uv = frameUv(l, t, a - l.delay(), 0);
		float[] c = rgba(l, t);
		double roll = Math.toRadians(l.rotation() + l.spin() * a);
		Vec3[] b = facingBasis(l, d);
		Vec3 x = b[0].scale(Math.cos(roll)).add(b[1].scale(Math.sin(roll))).scale(half);
		Vec3 y = b[1].scale(Math.cos(roll)).subtract(b[0].scale(Math.sin(roll))).scale(half);
		d.quad(l.texture(), l.additive(), at.subtract(x).subtract(y), at.add(x).subtract(y), at.add(x).add(y), at.subtract(x).add(y), c, uv[0], uv[1], uv[2],
				uv[3]);
	}

	void particle(FxDraw d, VfxLibrary.Layer l, P p) {
		if (l.texture() == null) {
			return;
		}
		float t = Math.min(1, (p.age + d.partial) / p.life);
		Vec3 pos = p.prev.lerp(p.pos, d.partial);
		double half = l.size().at(t) * params.scale * 0.5;
		float[] c = rgba(l, t);
		float[] uv = frameUv(l, t, p.age + d.partial, p.seed);
		float stretch = l.f("stretch", 0);
		if (stretch > 0 && p.vel.lengthSqr() > 1e-5) {
			// Velocity-stretched: a short camera-facing streak along the motion.
			List<Vec3> pts = List.of(pos, pos.subtract(p.vel.scale(stretch)));
			if (l.additive()) {
				d.ribbon(l.texture(), pts, new double[]{half, half * 0.4}, new float[]{c[0], c[1], c[2]}, new float[]{c[3], 0}, 0);
			} else {
				d.ribbonSoft(l.texture(), pts, new double[]{half, half * 0.4}, new float[]{c[0], c[1], c[2]}, new float[]{c[3], 0}, 0);
			}
			return;
		}
		Vec3 r = d.cameraRight(), u = d.cameraUp();
		double cr = Math.cos(p.roll), sr = Math.sin(p.roll);
		Vec3 x = r.scale(cr).add(u.scale(sr)).scale(half), y = u.scale(cr).subtract(r.scale(sr)).scale(half);
		d.quad(l.texture(), l.additive(), pos.subtract(x).subtract(y), pos.add(x).subtract(y), pos.add(x).add(y), pos.subtract(x).add(y), c, uv[0], uv[1],
				uv[2], uv[3]);
	}

	void ring(FxDraw d, VfxLibrary.Layer l, Vec3 at, float t, float a) {
		if (l.texture() == null) {
			return;
		}
		double rad = l.curve("radius", 2).at(t) * params.scale, w = l.curve("width", 0.4f).at(t) * params.scale;
		float[] c = rgba(l, t);
		Vec3[] b = l.facing().equals("camera") ? new Vec3[]{d.cameraRight(), d.cameraUp()} : facingBasis(l, d).length == 2 && l.facing().equals("direction")
				? WindMesh.basis(params.direction) : WindMesh.basis(new Vec3(0, 1, 0));
		int seg = Math.max(12, (int) (l.i("segments", 40) * WindMesh.detail()));
		VertexConsumer vc = d.buffer(l.additive() ? FxRenderTypes.glow(l.texture()) : FxRenderTypes.soft(l.texture()));
		float scroll = l.f("scroll", 0) * a * 0.05f;
		double inner = Math.max(0, rad - w);
		for (int i = 0; i < seg; i++) {
			double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
			Vec3 d0 = b[0].scale(Math.cos(a0)).add(b[1].scale(Math.sin(a0))), d1 = b[0].scale(Math.cos(a1)).add(b[1].scale(Math.sin(a1)));
			float u0 = (float) i / seg * l.f("repeat", 4) + scroll, u1 = (float) (i + 1) / seg * l.f("repeat", 4) + scroll;
			d.vertex(vc, at.add(d0.scale(inner)), c[0], c[1], c[2], c[3], u0, 1);
			d.vertex(vc, at.add(d1.scale(inner)), c[0], c[1], c[2], c[3], u1, 1);
			d.vertex(vc, at.add(d1.scale(rad)), c[0], c[1], c[2], c[3], u1, 0);
			d.vertex(vc, at.add(d0.scale(rad)), c[0], c[1], c[2], c[3], u0, 0);
		}
	}

	void beam(FxDraw d, VfxLibrary.Layer l, Vec3 at, float t, float a) {
		if (l.texture() == null) {
			return;
		}
		Vec3 end = params.target != null ? params.target : at.add(params.direction.scale(l.curve("length", 8).at(t) * params.scale));
		int seg = Math.max(2, (int) (l.i("segments", 12) * WindMesh.detail()));
		float wob = l.f("wobble", 0) * params.scale;
		List<Vec3> pts = new ArrayList<>();
		Vec3[] b = WindMesh.basis(end.subtract(at).normalize());
		for (int i = 0; i <= seg; i++) {
			double k = i / (double) seg;
			double env = Math.sin(Math.PI * k);
			pts.add(at.lerp(end, k).add(b[0].scale(Math.sin(a * 0.7 + k * 9 + seed) * wob * env)).add(b[1].scale(Math.cos(a * 0.9 + k * 7) * wob * env)));
		}
		double w = l.size().at(t) * params.scale * 0.5;
		double[] ws = new double[pts.size()];
		float[] al = new float[pts.size()];
		float[] c = rgba(l, t);
		for (int i = 0; i < ws.length; i++) {
			ws[i] = w * (l.b("taper", false) ? 1 - 0.7 * i / (ws.length - 1.0) : 1);
			al[i] = c[3];
		}
		float scroll = -l.f("scroll", 1) * a * 0.05f;
		if (l.additive()) {
			d.ribbon(l.texture(), pts, ws, new float[]{c[0], c[1], c[2]}, al, scroll);
		} else {
			d.ribbonSoft(l.texture(), pts, ws, new float[]{c[0], c[1], c[2]}, al, scroll);
		}
	}

	void ribbon(FxDraw d, VfxLibrary.Layer l, int li, float t, float a) {
		List<Vec3> tr = trails.get(li);
		if (l.texture() == null || tr.size() < 2) {
			return;
		}
		double w = l.size().at(t) * params.scale * 0.5;
		double[] ws = new double[tr.size()];
		float[] al = new float[tr.size()];
		float[] c = rgba(l, t);
		for (int i = 0; i < ws.length; i++) {
			double k = i / (ws.length - 1.0);
			ws[i] = w * (1 - 0.8 * k);
			al[i] = (float) (c[3] * (1 - k));
		}
		if (l.additive()) {
			d.ribbon(l.texture(), tr, ws, new float[]{c[0], c[1], c[2]}, al, a * 0.05f);
		} else {
			d.ribbonSoft(l.texture(), tr, ws, new float[]{c[0], c[1], c[2]}, al, a * 0.05f);
		}
	}

	void mesh(FxDraw d, VfxLibrary.Layer l, Vec3 at, float t, float a) {
		ResourceLocation id = ResourceLocation.tryParse(l.s("mesh", ""));
		if (id == null) {
			return;
		}
		VfxMeshes.Mesh m = VfxMeshes.get(id);
		if (m.positions().isEmpty()) {
			return;
		}
		double s = l.size().at(t) * params.scale;
		float[] c = rgba(l, t);
		Vec3[] b = WindMesh.basis(l.facing().equals("up") ? new Vec3(0, 1, 0) : params.direction);
		Vec3 fwd = l.facing().equals("up") ? new Vec3(0, 1, 0) : params.direction;
		double yaw = Math.toRadians(l.rotation() + l.spin() * a);
		String material = l.s("material", "none");
		WindRender.Material mat = switch (material) {
			case "wind" -> WindRender.Material.WIND;
			case "metal" -> WindRender.Material.METAL;
			case "fire" -> WindRender.Material.FIRE;
			case "smoke" -> WindRender.Material.SMOKE;
			default -> null;
		};
		Runnable draw = () -> {
			VertexConsumer vc = mat == null ? d.buffer(l.additive() ? FxRenderTypes.glow(l.texture()) : FxRenderTypes.soft(l.texture())) : null;
			for (int f = 0; f < m.positions().size(); f++) {
				float[] pos = m.positions().get(f), uv = m.uvs().get(f);
				Vec3[] q = new Vec3[4];
				for (int k = 0; k < 4; k++) {
					// Mesh space: +Y along the facing axis, X/Z across it; spun about the axis.
					double x = pos[k * 3] * Math.cos(yaw) - pos[k * 3 + 2] * Math.sin(yaw), z = pos[k * 3] * Math.sin(yaw) + pos[k * 3 + 2] * Math.cos(yaw);
					q[k] = at.add(b[0].scale(x * s)).add(fwd.scale(pos[k * 3 + 1] * s)).add(b[1].scale(z * s));
				}
				if (mat != null) {
					Vec3 n = q[1].subtract(q[0]).cross(q[2].subtract(q[0]));
					float fr = n.lengthSqr() < 1e-9 ? 0.5f : WindRender.fresnel(d, q[0], n);
					WindRender.quad(d, l.additive(), q, new float[]{uv[0], uv[2], uv[4], uv[6]}, new float[]{uv[1], uv[3], uv[5], uv[7]}, rgb(c),
							new float[]{c[3], c[3], c[3], c[3]}, l.f("dissolve_from", 2) < t ? (t - l.f("dissolve_from", 2)) / (1 - l.f("dissolve_from", 2)) : 0,
							new float[]{fr, fr, fr, fr});
				} else if (l.texture() != null) {
					for (int k = 0; k < 4; k++) {
						d.vertex(vc, q[k], c[0], c[1], c[2], c[3], uv[k * 2], uv[k * 2 + 1]);
					}
				}
			}
		};
		if (mat != null) {
			WindRender.with(mat, draw);
		} else {
			draw.run();
		}
	}

	void slash(FxDraw d, VfxLibrary.Layer l, Vec3 at, float t, float a) {
		float[] c = rgba(l, t);
		double sweep = Math.toRadians(l.curve("sweep", 0).at(t));
		Vec3 f = params.direction;
		Vec3[] b = WindMesh.basis(f);
		double tilt = Math.toRadians(l.f("tilt", 0));
		Vec3 up = b[1].scale(Math.cos(tilt)).add(b[0].scale(Math.sin(tilt)));
		Vec3 side = f.cross(up).normalize();
		Vec3 fwd = f.scale(Math.cos(sweep)).add(side.scale(Math.sin(sweep)));
		Vec3 sd = fwd.cross(up).normalize().scale(-1);
		WindRender.Material mat = switch (l.s("material", "metal")) {
			case "wind" -> WindRender.Material.WIND;
			case "fire" -> WindRender.Material.FIRE;
			case "smoke" -> WindRender.Material.SMOKE;
			default -> WindRender.Material.METAL;
		};
		WindRender.with(mat, () -> WindMesh.crescent(d, l.additive(), at, fwd, sd, up, l.curve("radius", 2).at(t) * params.scale,
				l.curve("width", 0.5f).at(t) * params.scale, l.f("arc", 150), rgb(c), c[3], Math.max(0, (t - 0.55f) / 0.45f), t));
	}

	/** Heat-haze stand-in: true screen-space distortion needs a framebuffer pass Iris would replace, so a faint shimmer. */
	void distortion(FxDraw d, VfxLibrary.Layer l, Vec3 at, float t, float a) {
		double r = l.size().at(t) * params.scale;
		float[] c = rgba(l, t);
		Vec3[] b = WindMesh.basis(new Vec3(0, 1, 0));
		WindRender.with(WindRender.Material.WIND, () -> {
			for (int k = 0; k < 2; k++) {
				double rr = r * (0.5 + 0.5 * ((t + k * 0.5) % 1));
				WindMesh.ring(d, false, at, b[0], b[1], rr * 0.7, rr, a * 6 + k * 90, 360, rgb(c), c[3] * 0.25f, 0.3f, a * 0.08);
			}
		});
	}

	void continuous(FxDraw d, VfxLibrary.Layer l, Vec3 at, float t, float a) {
		float s = params.scale;
		switch (l.s("effect", "")) {
			case "smoke_volume" -> SmokeFx.volume(d, at, l.curve("radius", 1).at(t) * s, seed, a, rgba(l, t)[3], Math.max(0, (t - 0.6f) / 0.4f),
					l.i("layers", 2), rgb(rgba(l, t)));
			case "vortex" -> SmokeFx.vortex(d, at, l.f("height", 6) * s, l.f("bottom", 1) * s, l.curve("radius", 3).at(t) * s, a, l.f("speed", 0.2f), seed,
					rgb(rgba(l, t)), rgba(l, t)[3], Math.max(0, (t - 0.7f) / 0.3f), 0.3f);
			default -> { }
		}
	}
}
