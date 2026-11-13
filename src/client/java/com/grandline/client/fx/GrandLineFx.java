package com.grandline.client.fx;

import com.grandline.network.payload.FxEventPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client side of move visuals. The server sends each beat ({@link FxEventPayload}) at the tick it happens; this class
 * plays it. With Photon installed and an effect file present (assets/grandline/fx/&lt;name&gt;.fx) Photon renders the
 * beat; otherwise Grand Line's built-in chunky effects play: block-built fist, ember particles, flash and ring.
 */
public final class GrandLineFx {
	/** Ticks the windup embers gather for (matches the server's windup before launch). */
	private static final int WINDUP_TICKS = 6;
	private static final int FULL_BRIGHT = 0xF000F0;

	private static Boolean photon;

	/** A flaming fist in flight (client simulation of the server's projectile, for drawing only). */
	private static final class Fist {
		final int caster;
		final Vec3 dir;
		final double speed;
		final double range;
		Vec3 pos;
		Vec3 prev;
		double travelled;
		Entity photonCarrier; // invisible marker the Photon fist effect follows, if Photon plays it
		boolean done;

		Fist(int caster, Vec3 pos, Vec3 dir, double speed, double range) {
			this.caster = caster;
			this.pos = pos;
			this.prev = pos;
			this.dir = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
			this.speed = speed;
			this.range = range;
		}
	}

	private record Windup(int caster, int startTick) {
	}

	private static final List<Fist> FISTS = new ArrayList<>();
	private static final List<Windup> WINDUPS = new ArrayList<>();
	private static int clientTicks;

	private GrandLineFx() {
	}

	public static boolean photonLoaded() {
		if (photon == null) {
			photon = FabricLoader.getInstance().isModLoaded("photon");
		}
		return photon;
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(FxEventPayload.TYPE, (packet, player, sender) -> handle(packet));
		ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
		WorldRenderEvents.AFTER_ENTITIES.register(GrandLineFx::render);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			FISTS.clear();
			WINDUPS.clear();
			ArmAnimations.clear();
		});
	}

	// ------------------------------------------------------------------------------------------------ beats

	private static void handle(FxEventPayload packet) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null) {
			return;
		}
		Entity caster = level.getEntity(packet.caster());
		switch (packet.effect()) {
			case "flame_blast_windup" -> {
				if (caster != null) {
					ArmAnimations.windup(caster.getId(), caster.tickCount);
					boolean photonPlayed = photonLoaded() && PhotonBridge.onEntity(packet.effect(), level, caster, new Vec3(-0.35, 1.3, 0.2));
					if (!photonPlayed) {
						WINDUPS.add(new Windup(caster.getId(), clientTicks));
					}
				}
			}
			case "flame_blast_launch" -> {
				WINDUPS.removeIf(w -> w.caster() == packet.caster());
				if (caster != null) {
					ArmAnimations.release(caster.getId(), caster.tickCount);
					if (caster == mc.player) {
						mc.player.swing(InteractionHand.MAIN_HAND); // first-person punch on the launch frame
					}
				}
				Fist fist = new Fist(packet.caster(), packet.pos(), packet.dir(), packet.speed(), packet.range());
				if (photonLoaded()) {
					Entity marker = EntityType.MARKER.create(level);
					if (marker != null) {
						marker.setPos(fist.pos);
						if (PhotonBridge.onEntity("flame_blast_fist", level, marker, Vec3.ZERO)) {
							fist.photonCarrier = marker;
						}
					}
				}
				FISTS.add(fist);
				launchPuff(level, packet.pos(), fist.dir);
			}
			case "flame_blast_impact" -> {
				// End the caster's nearest fist at the real collision point.
				Fist nearest = null;
				for (Fist f : FISTS) {
					if (f.caster == packet.caster() && !f.done
							&& (nearest == null || f.pos.distanceToSqr(packet.pos()) < nearest.pos.distanceToSqr(packet.pos()))) {
						nearest = f;
					}
				}
				if (nearest != null) {
					finish(nearest);
				}
				if (!(photonLoaded() && PhotonBridge.atPoint(packet.effect(), level, packet.pos()))) {
					impactBurst(level, packet.pos());
				}
			}
			default -> {
			}
		}
	}

	private static void tick() {
		clientTicks++;
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.isPaused()) {
			return;
		}
		// Windup: embers spiral in towards the casting hand.
		for (Iterator<Windup> it = WINDUPS.iterator(); it.hasNext(); ) {
			Windup w = it.next();
			Entity caster = level.getEntity(w.caster());
			int age = clientTicks - w.startTick();
			if (caster == null || age > WINDUP_TICKS + 4) {
				it.remove();
				continue;
			}
			gatherEmbers(level, handPosition(caster, 1f), age);
		}
		// Fists: advance, draw the tapered flame trail, expire at their range.
		for (Iterator<Fist> it = FISTS.iterator(); it.hasNext(); ) {
			Fist f = it.next();
			if (f.done) {
				it.remove();
				continue;
			}
			f.prev = f.pos;
			f.pos = f.pos.add(f.dir.scale(f.speed));
			f.travelled += f.speed;
			if (f.photonCarrier != null) {
				f.photonCarrier.setPos(f.pos);
			} else {
				trail(level, f);
			}
			if (f.travelled > f.range + f.speed * 3) {
				finish(f); // the server's impact event normally arrives first
			}
		}
	}

	private static void finish(Fist f) {
		f.done = true;
		if (f.photonCarrier != null) {
			f.photonCarrier.discard();
		}
	}

	// ------------------------------------------------------------------------------------------------ built-in visuals

	/** The casting hand: in first person in front of the camera, otherwise beside the right shoulder. */
	private static Vec3 handPosition(Entity caster, float partial) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 look = caster.getViewVector(partial);
		float yaw = Mth.lerp(partial, caster.yRotO, caster.getYRot()) * Mth.DEG_TO_RAD;
		Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
		if (caster == mc.player && mc.options.getCameraType().isFirstPerson()) {
			return caster.getEyePosition(partial).add(look.scale(0.6)).add(right.scale(0.32)).add(0, -0.25, 0);
		}
		Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
		return caster.getPosition(partial).add(0, 1.3, 0).add(right.scale(0.4)).add(forward.scale(-0.15));
	}

	private static void gatherEmbers(ClientLevel level, Vec3 hand, int age) {
		RandomSource random = level.random;
		int count = 3 + age;
		for (int i = 0; i < count; i++) {
			double r = 0.9 - age * 0.08;
			Vec3 offset = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.7, random.nextGaussian()).normalize().scale(r);
			Vec3 p = hand.add(offset);
			Vec3 v = offset.scale(-0.16);
			level.addParticle(i % 3 == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, p.x, p.y, p.z, v.x, v.y, v.z);
		}
		if (age >= WINDUP_TICKS - 1) {
			level.addParticle(ParticleTypes.LAVA, hand.x, hand.y, hand.z, 0, 0, 0);
		}
	}

	private static void launchPuff(ClientLevel level, Vec3 pos, Vec3 dir) {
		for (int i = 0; i < 6; i++) {
			Vec3 v = dir.scale(0.15).add(level.random.nextGaussian() * 0.04, level.random.nextGaussian() * 0.04, level.random.nextGaussian() * 0.04);
			level.addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, v.x, v.y, v.z);
		}
		level.addParticle(ParticleTypes.POOF, pos.x, pos.y, pos.z, dir.x * 0.05, dir.y * 0.05, dir.z * 0.05);
	}

	/** Tapered trail: dense big flames right behind the fist, thinning to small embers further back. */
	private static void trail(ClientLevel level, Fist f) {
		RandomSource random = level.random;
		for (int i = 0; i < 4; i++) {
			double back = i * 0.35;
			double spread = 0.22 - i * 0.05;
			Vec3 p = f.pos.subtract(f.dir.scale(back + 0.3));
			ParticleOptions type = i < 2 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME;
			level.addParticle(type, p.x + random.nextGaussian() * spread, p.y + random.nextGaussian() * spread,
					p.z + random.nextGaussian() * spread, -f.dir.x * 0.03, -f.dir.y * 0.03 + 0.01, -f.dir.z * 0.03);
		}
	}

	/** Impact: bright flash, two expanding flame rings, and a short burst of embers. Kept low so the target stays visible. */
	private static void impactBurst(ClientLevel level, Vec3 pos) {
		RandomSource random = level.random;
		// Bright pop: a small, short spark burst (vanilla's FLASH quad is too big and hides the target).
		for (int i = 0; i < 10; i++) {
			level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y + 0.4, pos.z,
					random.nextGaussian() * 0.12, random.nextGaussian() * 0.12, random.nextGaussian() * 0.12);
		}
		for (int ring = 0; ring < 2; ring++) {
			int count = 22;
			double speed = ring == 0 ? 0.42 : 0.26;
			for (int i = 0; i < count; i++) {
				double a = i * Math.PI * 2 / count + ring * 0.14;
				level.addParticle(ring == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, pos.x, pos.y + 0.15 + ring * 0.25, pos.z,
						Math.cos(a) * speed, 0.01, Math.sin(a) * speed);
			}
		}
		for (int i = 0; i < 12; i++) {
			level.addParticle(i % 2 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, pos.x, pos.y + 0.3, pos.z,
					random.nextGaussian() * 0.15, 0.1 + random.nextDouble() * 0.2, random.nextGaussian() * 0.15);
		}
	}

	/** Draws each in-flight fist as a chunky block-built fist (magma knuckle block, glowing knuckles, tapering tail). */
	private static void render(WorldRenderContext context) {
		if (FISTS.isEmpty() || context.consumers() == null) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 camera = context.camera().getPosition();
		PoseStack pose = context.matrixStack();
		float partial = context.tickDelta();
		BlockState core = Blocks.MAGMA_BLOCK.defaultBlockState();
		BlockState knuckle = Blocks.SHROOMLIGHT.defaultBlockState();
		BlockState[] tail = {Blocks.ORANGE_CONCRETE.defaultBlockState(), Blocks.ORANGE_CONCRETE.defaultBlockState(),
				Blocks.RED_CONCRETE.defaultBlockState(), Blocks.RED_NETHER_BRICKS.defaultBlockState()};
		for (Fist f : FISTS) {
			if (f.done || f.photonCarrier != null) {
				continue;
			}
			Vec3 p = f.prev.lerp(f.pos, partial);
			float yaw = (float) Math.toDegrees(Math.atan2(f.dir.x, f.dir.z));
			float pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(f.dir.y, -1, 1)));
			pose.pushPose();
			pose.translate(p.x - camera.x, p.y - camera.y, p.z - camera.z);
			pose.mulPose(Axis.YP.rotationDegrees(yaw));
			pose.mulPose(Axis.XP.rotationDegrees(-pitch));
			// Fist body (forward is +Z in this frame).
			cube(pose, context, core, 0, 0, 0, 0.55f);
			for (int k = 0; k < 4; k++) {
				cube(pose, context, knuckle, -0.2f + k * 0.135f, 0.12f, 0.3f, 0.13f);
			}
			cube(pose, context, knuckle, -0.24f, -0.08f, 0.22f, 0.14f); // thumb
			// Tapering tail of shrinking blocks.
			for (int t = 0; t < tail.length; t++) {
				float size = 0.42f - t * 0.09f;
				cube(pose, context, tail[t], 0, 0, -0.45f - t * 0.32f, size);
			}
			pose.popPose();
		}
	}

	private static void cube(PoseStack pose, WorldRenderContext context, BlockState state, float x, float y, float z, float size) {
		pose.pushPose();
		pose.translate(x - size / 2, y - size / 2, z - size / 2);
		pose.scale(size, size, size);
		Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, context.consumers(), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
		pose.popPose();
	}
}
