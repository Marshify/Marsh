package com.grandline.entity;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Entity type registrations. */
public final class GrandLineEntities {
	public static final EntityType<Islander> ISLANDER = Registry.register(BuiltInRegistries.ENTITY_TYPE, GrandLineMod.id("islander"),
			FabricEntityTypeBuilder.<Islander>create(MobCategory.MISC, Islander::new).dimensions(EntityDimensions.fixed(0.6F, 1.95F)).build());

	public static final EntityType<GrandLineEnemy> BANDIT = enemy("bandit");
	public static final EntityType<GrandLineEnemy> RAIDER = enemy("raider");
	public static final EntityType<GrandLineEnemy> CORSAIR = enemy("corsair");
	public static final EntityType<IslandBoss> CAPTAIN = Registry.register(BuiltInRegistries.ENTITY_TYPE, GrandLineMod.id("captain"),
			FabricEntityTypeBuilder.<IslandBoss>create(MobCategory.MONSTER, IslandBoss::new).dimensions(EntityDimensions.fixed(0.8F, 2.6F)).build());

	/** Verdant Wilds' temple boss (player-sized, wields the Saber). */
	public static final EntityType<SaberBoss> SABER_BOSS = Registry.register(BuiltInRegistries.ENTITY_TYPE, GrandLineMod.id("saber_boss"),
			FabricEntityTypeBuilder.<SaberBoss>create(MobCategory.MONSTER, SaberBoss::new).dimensions(EntityDimensions.fixed(0.65F, 2.1F))
					.trackRangeBlocks(64).build());

	/** The fishing bobber (never saved; lives only while someone is fishing). */
	public static final EntityType<com.grandline.fishing.BobberEntity> BOBBER = Registry.register(BuiltInRegistries.ENTITY_TYPE, GrandLineMod.id("bobber"),
			FabricEntityTypeBuilder.<com.grandline.fishing.BobberEntity>create(MobCategory.MISC, com.grandline.fishing.BobberEntity::new)
					.dimensions(EntityDimensions.fixed(0.25F, 0.25F)).trackRangeBlocks(64).trackedUpdateRate(2).disableSaving().build());

	private GrandLineEntities() {
	}

	private static EntityType<GrandLineEnemy> enemy(String name) {
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, GrandLineMod.id(name),
				FabricEntityTypeBuilder.<GrandLineEnemy>create(MobCategory.MONSTER, GrandLineEnemy::new)
						.dimensions(EntityDimensions.fixed(0.6F, 1.95F)).build());
	}

	public static EntityType<? extends GrandLineEnemy> typeOf(EnemyKind kind) {
		return switch (kind) {
			case BANDIT -> BANDIT;
			case RAIDER -> RAIDER;
			case CORSAIR -> CORSAIR;
			case CAPTAIN -> CAPTAIN;
		};
	}

	public static EnemyKind kindOf(EntityType<?> type) {
		if (type == RAIDER) {
			return EnemyKind.RAIDER;
		}
		if (type == CORSAIR) {
			return EnemyKind.CORSAIR;
		}
		if (type == CAPTAIN || type == SABER_BOSS) {
			return EnemyKind.CAPTAIN;
		}
		return EnemyKind.BANDIT;
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(ISLANDER, Islander.createAttributes());
		FabricDefaultAttributeRegistry.register(BANDIT, GrandLineEnemy.createAttributes());
		FabricDefaultAttributeRegistry.register(RAIDER, GrandLineEnemy.createAttributes());
		FabricDefaultAttributeRegistry.register(CORSAIR, GrandLineEnemy.createAttributes());
		FabricDefaultAttributeRegistry.register(CAPTAIN, GrandLineEnemy.createAttributes());
		FabricDefaultAttributeRegistry.register(SABER_BOSS, SaberBoss.createAttributes());
	}
}
