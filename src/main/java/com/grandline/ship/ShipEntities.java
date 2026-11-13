package com.grandline.ship;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** One entity type per ship class (entity hitboxes are fixed per type). */
public final class ShipEntities {
	public static final EntityType<PirateShip> SLOOP = register(ShipClass.SLOOP);
	public static final EntityType<PirateShip> BRIG = register(ShipClass.BRIG);
	public static final EntityType<PirateShip> GALLEON = register(ShipClass.GALLEON);

	private ShipEntities() {
	}

	private static EntityType<PirateShip> register(ShipClass ship) {
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, GrandLineMod.id("ship_" + ship.id),
				FabricEntityTypeBuilder.<PirateShip>create(MobCategory.MISC, PirateShip::new)
						.dimensions(EntityDimensions.fixed(ship.width, ship.deck))
						.trackRangeChunks(10)
						.build());
	}

	public static EntityType<PirateShip> typeOf(ShipClass ship) {
		return switch (ship) {
			case SLOOP -> SLOOP;
			case BRIG -> BRIG;
			case GALLEON -> GALLEON;
		};
	}

	public static ShipClass classOf(EntityType<?> type) {
		if (type == GALLEON) {
			return ShipClass.GALLEON;
		}
		return type == BRIG ? ShipClass.BRIG : ShipClass.SLOOP;
	}

	public static void register() {
		GrandLineMod.LOGGER.debug("Registered ships {}, {}, {}", SLOOP, BRIG, GALLEON);
	}
}
