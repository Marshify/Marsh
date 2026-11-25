package com.grandline.client.fx;

import com.grandline.GrandLineMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Translucent "compressed air" surfaces for meshes (vortices, crescent blades, rotor discs, shockwaves).
 *
 * <p>Primary path: Grand Line's own GLSL core shader ({@code assets/grandline/shaders/core/wind.*}) adds procedural
 * fbm airflow streaks scrolling along the surface, a bright leading edge, per-vertex fresnel edge light and a noise
 * dissolve (with a bright burning rim) for fading out. Per-vertex dissolve and fresnel ride in the UV2 channel.
 *
 * <p>Fallback path (the shader failed to load, or an Iris shader pack is active, which replaces the core shader
 * pipeline): the same geometry through Grand Line's plain translucent/additive render types with a pre-baked airflow
 * texture; dissolve folds into alpha and fresnel into brightness. Both paths use vanilla blending and depth test
 * (no depth write), so nothing turns opaque or draws through walls.
 */
public final class WindRender {
	public static final ResourceLocation MASK = GrandLineMod.id("textures/fx/wind_mask.png");
	public static final ResourceLocation FLOW = GrandLineMod.id("textures/fx/wind_flow.png");
	/** Same mask, own texture key so the metal render types are cached apart from the wind ones. */
	public static final ResourceLocation METAL_MASK = GrandLineMod.id("textures/fx/metal_mask.png");

	// Spin palette (docs/SPIN.md): core, edge, airflow, vortex shading, haze, debris.
	public static final int CORE = 0xF1FCFF;
	public static final int EDGE = 0xC5E6F5;
	public static final int AIRFLOW = 0x91BED8;
	public static final int VORTEX = 0x648FA8;
	public static final int HAZE = 0xB8C1C9;
	public static final int DEBRIS = 0xA49B87;

	/** Surface material: compressed air (Spin) or sharpened steel (Blade), each its own GLSL program. */
	public enum Material {
		WIND, METAL, FIRE, SMOKE
	}

	private static ShaderInstance shader;
	private static ShaderInstance metal;
	private static ShaderInstance fire;
	private static ShaderInstance smoke;
	public static final ResourceLocation SMOKE_MASK = GrandLineMod.id("textures/fx/smoke_mask.png");
	public static final ResourceLocation SMOKE_FLOW = GrandLineMod.id("textures/fx/smoke_flow.png");
	public static final ResourceLocation FIRE_MASK = GrandLineMod.id("textures/fx/fire_mask.png");
	public static final ResourceLocation FIRE_FLOW = GrandLineMod.id("textures/fx/fire_flow.png");
	private static Material material = Material.WIND;

	/** Draws {@code draw} with {@code m} (meshes drawn inside use that material's shader and fallback texture). */
	public static void with(Material m, Runnable draw) {
		Material old = material;
		material = m;
		try {
			draw.run();
		} finally {
			material = old;
		}
	}

	public static final ResourceLocation METAL_FLOW = GrandLineMod.id("textures/fx/metal_flow.png");
	private static Boolean irisPresent;

	private WindRender() {
	}

	public static void register() {
		CoreShaderRegistrationCallback.EVENT.register(context -> context.register(GrandLineMod.id("wind"),
				DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, program -> shader = program));
		CoreShaderRegistrationCallback.EVENT.register(context -> context.register(GrandLineMod.id("metal"),
				DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, program -> metal = program));
		CoreShaderRegistrationCallback.EVENT.register(context -> context.register(GrandLineMod.id("blast"),
				DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, program -> fire = program));
		CoreShaderRegistrationCallback.EVENT.register(context -> context.register(GrandLineMod.id("smoke"),
				DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, program -> smoke = program));
	}

	private static ShaderInstance program() {
		return switch (material) {
			case METAL -> metal;
			case FIRE -> fire;
			case SMOKE -> smoke;
			default -> shader;
		};
	}

	/** Whether the custom shader path is used this frame. */
	public static boolean shaderPath() {
		return program() != null && !shaderPackActive();
	}

	/** An Iris shader pack is in use (Iris replaces the core shader pipeline, so custom core shaders aren't safe). */
	static boolean shaderPackActive() {
		if (irisPresent == null) {
			irisPresent = FabricLoader.getInstance().isModLoaded("iris");
		}
		if (!irisPresent) {
			return false;
		}
		try {
			Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Object instance = api.getMethod("getInstance").invoke(null);
			return (boolean) api.getMethod("isShaderPackInUse").invoke(instance);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return true; // can't tell: be safe
		}
	}

	/**
	 * Emits one quad (corners a, b, c, d) of a wind surface. {@code alpha} and {@code fresnel} are per corner;
	 * {@code dissolve} 0 solid .. 1 gone. {@code glow}: additive (bright edges) instead of translucent.
	 */
	public static void quad(FxDraw d, boolean glow, Vec3[] p, float[] u, float[] v, int rgb, float[] alpha, float dissolve, float[] fresnel) {
		float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;
		if (shaderPath()) {
			ShaderInstance program = program();
			ResourceLocation key = switch (material) {
				case METAL -> METAL_MASK;
				case FIRE -> FIRE_MASK;
				case SMOKE -> SMOKE_MASK;
				default -> MASK;
			};
			VertexConsumer c = d.buffer(glow ? FxRenderTypes.shaderGlow(key, () -> program) : FxRenderTypes.shaderSoft(key, () -> program));
			int dis = (int) (Math.max(0, Math.min(1, dissolve)) * 240);
			for (int i = 0; i < 4; i++) {
				d.vertex(c, p[i], r, g, b, alpha[i], u[i], v[i], dis, (int) (Math.max(0, Math.min(1, fresnel[i])) * 240));
			}
			return;
		}
		ResourceLocation fallback = switch (material) {
			case METAL -> METAL_FLOW;
			case FIRE -> FIRE_FLOW;
			case SMOKE -> SMOKE_FLOW;
			default -> FLOW;
		};
		VertexConsumer c = d.buffer(glow ? FxRenderTypes.glow(fallback) : FxRenderTypes.soft(fallback));
		float keep = 1 - Math.max(0, Math.min(1, dissolve));
		if (material == Material.SMOKE) {
			// Smoke fades at its edges instead of lighting up (soft volume, no rim glow).
			for (int i = 0; i < 4; i++) {
				d.vertex(c, p[i], r, g, b, alpha[i] * keep * (1 - 0.8f * fresnel[i]), u[i], v[i]);
			}
			return;
		}
		for (int i = 0; i < 4; i++) {
			float lift = 1 + 0.6f * fresnel[i];
			d.vertex(c, p[i], Math.min(1, r * lift), Math.min(1, g * lift), Math.min(1, b * lift), alpha[i] * keep * (0.6f + 0.6f * fresnel[i]), u[i], v[i]);
		}
	}

	/** Fresnel edge factor at {@code p} for a surface with normal {@code n}: 0 facing the camera, 1 edge-on. */
	public static float fresnel(FxDraw d, Vec3 p, Vec3 n) {
		Vec3 view = d.cameraPosition().subtract(p);
		double len = view.length() * n.length();
		if (len < 1e-6) {
			return 0;
		}
		float f = (float) (1 - Math.abs(view.dot(n) / len));
		return f * f;
	}
}
