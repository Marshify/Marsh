package com.grandline.client.render;

import com.grandline.GrandLineMod;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.Islander;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import java.util.Locale;
import java.util.function.Function;

/** Humanoid renderers for NPCs and enemies, using original 64×64 skins. */
public final class GrandLineRenderers {
	private GrandLineRenderers() {
	}

	private static final class Renderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
		private final Function<T, ResourceLocation> texture;
		private final float scale;

		Renderer(EntityRendererProvider.Context context, Function<T, ResourceLocation> texture, float scale) {
			super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F * scale);
			this.texture = texture;
			this.scale = scale;
		}

		@Override
		public ResourceLocation getTextureLocation(T entity) {
			return texture.apply(entity);
		}

		@Override
		protected void scale(T entity, PoseStack poseStack, float partialTick) {
			poseStack.scale(scale, scale, scale);
		}
	}

	private static ResourceLocation tex(String name) {
		return GrandLineMod.id("textures/entity/" + name + ".png");
	}

	public static void register() {
		EntityRendererRegistry.register(GrandLineEntities.ISLANDER, ctx -> new Renderer<Islander>(ctx,
				npc -> tex(npc.role().name().toLowerCase(Locale.ROOT)), 1.0F));
		EntityRendererRegistry.register(GrandLineEntities.BANDIT, ctx -> new Renderer<GrandLineEnemy>(ctx, e -> tex("bandit"), 1.0F));
		EntityRendererRegistry.register(GrandLineEntities.RAIDER, ctx -> new Renderer<GrandLineEnemy>(ctx, e -> tex("raider"), 1.05F));
		EntityRendererRegistry.register(GrandLineEntities.CORSAIR, ctx -> new Renderer<GrandLineEnemy>(ctx, e -> tex("corsair"), 1.1F));
		EntityRendererRegistry.register(GrandLineEntities.CAPTAIN, ctx -> new Renderer<>(ctx, e -> tex("captain"), 1.35F));
		com.grandline.client.render.ship.PirateShipRenderer.register();
	}
}
