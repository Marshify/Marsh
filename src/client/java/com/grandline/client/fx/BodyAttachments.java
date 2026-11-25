package com.grandline.client.fx;

import com.grandline.GrandLineMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Retractable body attachments (reusable): metal grown from a limb for as long as a move needs it, extending and
 * retracting smoothly. Every attachment is shown for a limited time ({@link #show}'s hold) and retracts on its own
 * when that runs out, so nothing can stay out if an end beat is lost; dimension changes and new worlds clear all.
 *
 * <ul>
 *   <li>{@link Kind#FOREARM_BLADES}: steel blades along both forearms, past the hands (standard and slim arms).</li>
 *   <li>{@link Kind#ROTOR}: a two-blade steel rotor at each raised hand, spinning about the arm.</li>
 *   <li>{@link Kind#STEEL_SHEEN}: a brief steel surface over the body (a deflected hit).</li>
 * </ul>
 * Drawn in the arm's own model space (third person: {@link Layer}; first person: the hand mixin), so they follow every
 * pose and stay attached at the joints.
 */
public final class BodyAttachments {
	public enum Kind {
		FOREARM_BLADES, ROTOR, STEEL_SHEEN
	}

	static final float EXTEND = 3f, RETRACT = 4f;
	static final ResourceLocation SHEEN = GrandLineMod.id("textures/entity/blade_sheen.png");

	private record State(float start, float until) {
	}

	private static final Map<Integer, EnumMap<Kind, State>> STATES = new HashMap<>();
	private static final Map<Integer, Double> ROTOR_SPIN = new HashMap<>();
	private static final Map<Integer, Float> ROTOR_LAST = new HashMap<>();

	private BodyAttachments() {
	}

	/** Shows (or keeps showing) {@code kind} until {@code holdTicks} from now. */
	public static void show(int entity, Kind kind, float age, float holdTicks) {
		EnumMap<Kind, State> m = STATES.computeIfAbsent(entity, k -> new EnumMap<>(Kind.class));
		State s = m.get(kind);
		float start = s != null && progress(s, age) > 0 ? s.start() : age;
		m.put(kind, new State(start, age + holdTicks));
	}

	/** Starts retracting {@code kind} now. */
	public static void hide(int entity, Kind kind, float age) {
		EnumMap<Kind, State> m = STATES.get(entity);
		if (m != null && m.containsKey(kind)) {
			State s = m.get(kind);
			m.put(kind, new State(s.start(), Math.min(s.until(), age)));
		}
	}

	public static void clearAll() {
		STATES.clear();
		ROTOR_SPIN.clear();
		ROTOR_LAST.clear();
	}

	/** 0 (retracted) .. 1 (fully out). */
	public static float progress(int entity, Kind kind, float age) {
		EnumMap<Kind, State> m = STATES.get(entity);
		State s = m == null ? null : m.get(kind);
		if (s == null) {
			return 0;
		}
		float p = progress(s, age);
		if (p <= 0 && age > s.until()) {
			m.remove(kind);
			if (m.isEmpty()) {
				STATES.remove(entity);
			}
		}
		return p;
	}

	private static float progress(State s, float age) {
		float in = Mth.clamp((age - s.start()) / EXTEND, 0, 1);
		float out = age <= s.until() ? 1 : 1 - Mth.clamp((age - s.until()) / RETRACT, 0, 1);
		return (float) WindMesh.ease(Math.min(in, out));
	}

	/** Rotor angle for an entity (spun up and down with its BodyMotion rotor rate). */
	static double rotorAngle(int entity, float age) {
		float last = ROTOR_LAST.getOrDefault(entity, age);
		float dt = Mth.clamp(age - last, 0, 2);
		double rate = 1.4 * BodyMotion.intensity(entity, age) / 1.0;
		double a = ROTOR_SPIN.getOrDefault(entity, 0.0) + rate * dt;
		ROTOR_SPIN.put(entity, a);
		ROTOR_LAST.put(entity, age);
		return a;
	}

	// ------------------------------------------------------------------------------------------------ drawing

	/** Draws the arm's attachments in its model space (pose already at the arm, i.e. after arm.translateAndRotate). */
	public static void drawArm(PoseStack pose, MultiBufferSource buffers, int light, int entity, float age, boolean left, boolean slim) {
		float blades = progress(entity, Kind.FOREARM_BLADES, age);
		if (blades > 0) {
			pose.pushPose();
			// Along the outer side of the forearm, out past the hand; the edge faces outward.
			float outer = (slim ? 2f : 3f) / 16f;
			pose.translate(left ? outer : -outer, 4f / 16f, 0);
			if (left) {
				pose.mulPose(Axis.YP.rotationDegrees(180));
			}
			MetalMesh.blade(pose, buffers, light, (0.38f + 0.72f) * blades, 0.16f, 0.05f, 1f);
			pose.popPose();
		}
		float rotor = progress(entity, Kind.ROTOR, age);
		if (rotor > 0) {
			double angle = rotorAngle(entity, age) + (left ? Math.PI / 2 : 0);
			// The rotor hub at the hand; blades spin about the arm's own axis (straight up while flying).
			for (int ghost = 0; ghost < 3; ghost++) {
				float alpha = ghost == 0 ? 1f : ghost == 1 ? 0.35f : 0.15f; // motion-blur copies behind the blades
				double a = angle - ghost * 0.28 * BodyMotion.intensity(entity, age);
				for (int b = 0; b < 2; b++) {
					pose.pushPose();
					pose.translate(0, 11f / 16f, 0);
					pose.mulPose(Axis.YP.rotation((float) (a + b * Math.PI)));
					pose.mulPose(Axis.ZP.rotationDegrees(90)); // lay the blade out flat, radially
					pose.mulPose(Axis.YP.rotationDegrees(12)); // a little pitch, like a real rotor
					MetalMesh.blade(pose, buffers, light, 1.05f * rotor, 0.2f, 0.05f, alpha);
					pose.popPose();
				}
			}
		}
	}

	/** Third-person layer: the attachments on both arms, and the steel sheen over the body. */
	public static final class Layer extends net.minecraft.client.renderer.entity.layers.RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
		public Layer(net.minecraft.client.renderer.entity.RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
			super(parent);
		}

		@Override
		public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
				float partial, float ageInTicks, float netHeadYaw, float headPitch) {
			int id = player.getId();
			float age = player.tickCount + partial;
			PlayerModel<AbstractClientPlayer> model = getParentModel();
			if (SpringLimbs.any(id)) {
				for (boolean left : new boolean[]{false, true}) {
					pose.pushPose();
					(left ? model.leftArm : model.rightArm).translateAndRotate(pose);
					SpringLimbs.drawArm(pose, buffers, light, player, age, left);
					pose.popPose();
					pose.pushPose();
					(left ? model.leftLeg : model.rightLeg).translateAndRotate(pose);
					SpringLimbs.drawLeg(pose, buffers, light, player, age, left);
					pose.popPose();
				}
			}
			BombFx.drawLayer(pose, buffers, light, player, model, age);
			if (!STATES.containsKey(id)) {
				return;
			}
			boolean slim = "slim".equals(player.getModelName());
			for (boolean left : new boolean[]{false, true}) {
				ModelPart arm = left ? model.leftArm : model.rightArm;
				pose.pushPose();
				arm.translateAndRotate(pose);
				drawArm(pose, buffers, light, id, age, left, slim);
				pose.popPose();
			}
			float sheen = progress(id, Kind.STEEL_SHEEN, age);
			if (sheen > 0) {
				model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(SHEEN)), light, OverlayTexture.NO_OVERLAY,
						1f, 1f, 1f, 0.4f * sheen);
			}
		}
	}
}
