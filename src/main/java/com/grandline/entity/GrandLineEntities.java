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
		if (type == CAPTAIN) {
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
	}
}
