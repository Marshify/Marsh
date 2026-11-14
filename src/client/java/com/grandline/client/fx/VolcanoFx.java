package com.grandline.client.fx;

import com.grandline.network.payload.FxEventPayload;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Emberfall's volcano: on each server beat, a dark smoke ring forms at one of three heights over the crater (36 soft
 * puffs on a circle, drifting outward and up while they grow and fade), plus a plume of grey smoke from the crater and a
 * few rising embers. Sprites only: no blocks, no collision, capped by the sprite budget.
 */
final class VolcanoFx {
	private VolcanoFx() {
	}

	static boolean handles(String effect) {
		return effect.startsWith("volcano_");
	}

	static void handle(FxEventPayload p) {
		RandomSource r = RandomSource.create();
		Vec3 top = p.pos();
		int ring = (int) p.size();
		double height = 6 + ring * 9;
		double radius = 7 + ring * 3;
		int puffs = FxSprites.count(36);
		double spin = r.nextDouble() * Math.PI;
		for (int i = 0; i < puffs; i++) {
			double a = spin + i * Math.PI * 2 / puffs;
			Vec3 at = top.add(Math.cos(a) * radius, height + r.nextGaussian() * 0.4, Math.sin(a) * radius);
			Vec3 vel = new Vec3(Math.cos(a) * 0.05, 0.02, Math.sin(a) * 0.05);
			FxSprites.smoke(at, vel, 2.2 + r.nextDouble() * 0.6, 90 + r.nextInt(20), 0x3A3634);
		}
		for (int i = 0; i < FxSprites.count(10); i++) {
			Vec3 at = top.add(r.nextGaussian() * 2, r.nextDouble() * 3, r.nextGaussian() * 2);
			FxSprites.smoke(at, new Vec3(r.nextGaussian() * 0.02, 0.12 + r.nextDouble() * 0.06, r.nextGaussian() * 0.02), 2.6, 80, 0x55504C);
		}
		for (int i = 0; i < FxSprites.count(8); i++) {
			Vec3 at = top.add(r.nextGaussian() * 3, -2 + r.nextDouble() * 2, r.nextGaussian() * 3);
			FxSprites.ember(at, new Vec3(r.nextGaussian() * 0.05, 0.2 + r.nextDouble() * 0.15, r.nextGaussian() * 0.05), 0.25, 40, 0xFFD25A, 0xFF5A1E);
		}
	}
}
