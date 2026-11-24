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

	/**
	 * Island enemies. One with an island skin (synced, e.g. Verdant Wilds' Canopy Primate) is drawn from that full
	 * 64 x 64 player skin: its own left limbs and the outer layer. Others use their kind's texture: a full player skin
	 * for kinds made that way (Bandit, Marine Trainee), the classic humanoid model otherwise.
	 */
	private static final class EnemyRenderer extends HumanoidMobRenderer<GrandLineEnemy, HumanoidModel<GrandLineEnemy>> {
		private final HumanoidModel<GrandLineEnemy> classic;
		private final net.minecraft.client.model.PlayerModel<GrandLineEnemy> player;
		private final String kindSkin;
		private final boolean kindIsPlayerSkin;
		private final float scale;

		EnemyRenderer(EntityRendererProvider.Context context, String kindSkin, boolean kindIsPlayerSkin, float scale) {
			super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F * scale);
			this.classic = this.model;
			this.player = new net.minecraft.client.model.PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
			this.kindSkin = kindSkin;
			this.kindIsPlayerSkin = kindIsPlayerSkin;
			this.scale = scale;
		}

		@Override
		public void render(GrandLineEnemy enemy, float yaw, float partialTick, PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
			this.model = !enemy.skin().isEmpty() || kindIsPlayerSkin ? player : classic;
			super.render(enemy, yaw, partialTick, pose, buffers, light);
		}

		@Override
		public ResourceLocation getTextureLocation(GrandLineEnemy enemy) {
			return tex(enemy.skin().isEmpty() ? kindSkin : enemy.skin());
		}

		@Override
		protected void scale(GrandLineEnemy entity, PoseStack poseStack, float partialTick) {
			poseStack.scale(scale, scale, scale);
		}
	}

	private static ResourceLocation tex(String name) {
		return GrandLineMod.id("textures/entity/" + name + ".png");
	}

	/** Ability Masters wear a full player skin (own left limbs and outer layer); the other islanders the classic model. */
	static boolean abilityMaster(Islander.Role role) {
		return role == Islander.Role.FLASH_STEP_MASTER || role == Islander.Role.AURA_MASTER || role == Islander.Role.INSTINCT_SAGE;
	}

	private static final class IslanderRenderer extends net.minecraft.client.renderer.entity.HumanoidMobRenderer<Islander, HumanoidModel<Islander>> {
		private final HumanoidModel<Islander> classic;
		private final net.minecraft.client.model.PlayerModel<Islander> player;

		IslanderRenderer(EntityRendererProvider.Context context) {
			super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
			this.classic = this.model;
			this.player = new net.minecraft.client.model.PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
		}

		@Override
		public void render(Islander npc, float yaw, float partialTick, PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
			this.model = abilityMaster(npc.role()) ? player : classic;
			super.render(npc, yaw, partialTick, pose, buffers, light);
		}

		@Override
		public ResourceLocation getTextureLocation(Islander npc) {
			return abilityMaster(npc.role()) ? tex("ability_master") : tex(npc.role().name().toLowerCase(Locale.ROOT));
		}
	}

	public static void register() {
		EntityRendererRegistry.register(GrandLineEntities.ISLANDER, IslanderRenderer::new);
		EntityRendererRegistry.register(GrandLineEntities.BANDIT, ctx -> new EnemyRenderer(ctx, "bandit", true, 1.0F));
		EntityRendererRegistry.register(GrandLineEntities.MARINE_TRAINEE, ctx -> new EnemyRenderer(ctx, "marine_trainee", true, 1.0F));
		EntityRendererRegistry.register(GrandLineEntities.RAIDER, ctx -> new EnemyRenderer(ctx, "raider", false, 1.05F));
		EntityRendererRegistry.register(GrandLineEntities.CORSAIR, ctx -> new EnemyRenderer(ctx, "corsair", false, 1.1F));
		EntityRendererRegistry.register(GrandLineEntities.CAPTAIN, ctx -> new EnemyRenderer(ctx, "captain", false, 1.35F));
		EntityRendererRegistry.register(GrandLineEntities.SABER_BOSS, SaberBossRenderer::new);
		EntityRendererRegistry.register(GrandLineEntities.BOBBER, com.grandline.client.fishing.BobberRenderer::new);
		com.grandline.client.render.ship.PirateShipRenderer.register();
	}
}
