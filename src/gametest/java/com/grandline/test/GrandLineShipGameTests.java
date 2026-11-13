package com.grandline.test;

import com.grandline.player.PlayerProfileService;
import com.grandline.ship.PirateShip;
import com.grandline.ship.ShipClass;
import com.grandline.ship.ShipData;
import com.grandline.ship.ShipEntities;
import com.grandline.ship.ShipService;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** Ships: buying rules, launching onto water with the owner at the helm, one ship per owner, seats, hull and sinking. */
public final class GrandLineShipGameTests implements FabricGameTest {
	/** An 11 x 11 pool two blocks deep, with air above, at relative y 1-2. */
	private static void pool(GameTestHelper helper) {
		for (int x = 0; x <= 10; x++) {
			for (int z = 0; z <= 10; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
				helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
				for (int y = 3; y <= 6; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
				}
			}
		}
	}

	private static ServerPlayer sailor(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		Vec3 pos = helper.absoluteVec(new Vec3(5.5, 4, 5.5));
		p.moveTo(pos.x, pos.y, pos.z, 0, 0);
		return p;
	}

	private static void cleanUp(GameTestHelper helper, List<ServerPlayer> players, Entity... others) {
		for (ServerPlayer p : players) {
			p.stopRiding();
			helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
		}
		for (Entity e : others) {
			if (e != null) {
				e.discard();
			}
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ships_buy")
	public void buyingChecksLevelMoneyAndOwnership(GameTestHelper helper) {
		ServerPlayer p = sailor(helper);
		PlayerProfileService.update(p, prof -> prof.withLevel(1).withCurrency(ShipClass.SLOOP.price() + 100));
		helper.assertTrue(ShipService.buy(p, ShipClass.GALLEON).equals("ship_level"), "galleon needs a higher level");
		helper.assertTrue(ShipService.buy(p, ShipClass.SLOOP).equals("ship_bought"), "sloop is affordable at level 1");
		helper.assertTrue(PlayerProfileService.get(p).currency() == 100, "the price was paid");
		helper.assertTrue(ShipService.buy(p, ShipClass.SLOOP).equals("ship_owned"), "can't buy the same ship twice");
		PlayerProfileService.update(p, prof -> prof.withLevel(ShipClass.BRIG.minLevel()).withCurrency(0));
		helper.assertTrue(ShipService.buy(p, ShipClass.BRIG).equals("ship_money"), "brig needs money");
		helper.assertTrue(!ShipService.ownership(p).owns(ShipClass.BRIG), "nothing granted on failure");
		cleanUp(helper, List.of(p));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ships_launch")
	public void launchBoardsOwnerAndReplacesOldShip(GameTestHelper helper) {
		pool(helper);
		ServerPlayer p = sailor(helper);
		helper.assertTrue(ShipService.launch(p, ShipClass.SLOOP, p.position(), 12).equals("ship_not_owned"), "must own the ship");
		ShipService.grant(p, ShipClass.SLOOP);
		String first = ShipService.launch(p, ShipClass.SLOOP, p.position(), 12);
		helper.assertTrue(first.equals("ship_launched"), "launched on the pool, got " + first);
		helper.assertTrue(p.getVehicle() instanceof PirateShip, "owner is aboard");
		PirateShip ship = (PirateShip) p.getVehicle();
		helper.assertTrue(ship.getControllingPassenger() == p && ship.seatOf(p) == 0, "owner takes the helm");
		helper.assertTrue(ship.getUUID().equals(ShipData.get(p.server).active(p.getUUID())), "recorded as the active ship");
		helper.assertTrue(ShipService.launch(p, ShipClass.SLOOP, p.position(), 12).equals("ship_launched"), "launch again");
		helper.assertTrue(ship.isRemoved(), "the old ship is gone: one ship per owner");
		Entity second = p.getVehicle();
		cleanUp(helper, List.of(p), second);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ships_float", timeoutTicks = 200)
	public void shipsFloatAtTheSurface(GameTestHelper helper) {
		pool(helper);
		double surface = helper.absoluteVec(new Vec3(0, 2, 0)).y + 8 / 9.0; // top of the pool's source water
		PirateShip[] ships = new PirateShip[ShipClass.values().length];
		for (ShipClass c : ShipClass.values()) {
			PirateShip ship = ShipEntities.typeOf(c).create(helper.getLevel());
			Vec3 pos = helper.absoluteVec(new Vec3(5.5, 3, 5.5));
			ship.moveTo(pos.x, pos.y, pos.z, 0, 0);
			helper.getLevel().addFreshEntity(ship);
			ships[c.ordinal()] = ship;
		}
		helper.runAfterDelay(60, () -> {
			for (PirateShip ship : ships) {
				double bottom = ship.getY();
				helper.assertTrue(bottom > surface - 0.6 && bottom < surface, ship.shipClass() + " floats with its hull in the water (bottom "
						+ String.format("%.2f", bottom) + ", surface " + String.format("%.2f", surface) + ")");
				helper.assertTrue(ship.getY() + ship.shipClass().deck > surface + 0.5, ship.shipClass() + " deck stays above the water");
				ship.discard();
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ships_dry")
	public void noLaunchWithoutOpenWater(GameTestHelper helper) {
		ServerPlayer p = sailor(helper);
		ShipService.grant(p, ShipClass.SLOOP);
		helper.assertTrue(ShipService.launch(p, ShipClass.SLOOP, p.position(), 4).equals("ship_no_water"), "dry land: refused");
		cleanUp(helper, List.of(p));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ships_seats")
	public void onlyPlayersBoardAndOwnerKeepsTheHelm(GameTestHelper helper) {
		pool(helper);
		ServerPlayer owner = sailor(helper);
		ServerPlayer a = sailor(helper);
		ServerPlayer b = sailor(helper);
		ShipService.grant(owner, ShipClass.SLOOP);
		ShipService.launch(owner, ShipClass.SLOOP, owner.position(), 12);
		PirateShip ship = (PirateShip) owner.getVehicle();
		owner.stopRiding();
		helper.assertTrue(a.startRiding(ship), "a crew member boards");
		helper.assertTrue(!b.startRiding(ship), "the sloop's other seat is kept for the owner");
		helper.assertTrue(ship.getControllingPassenger() == null, "nobody steers without the owner");
		var zombie = EntityType.ZOMBIE.create(helper.getLevel());
		zombie.moveTo(ship.getX(), ship.getY(), ship.getZ());
		helper.getLevel().addFreshEntity(zombie);
		helper.assertTrue(!zombie.startRiding(ship), "mobs never board");
		helper.assertTrue(owner.startRiding(ship), "the owner can always board");
		helper.assertTrue(ship.getControllingPassenger() == owner && ship.seatOf(owner) == 0 && ship.seatOf(a) == 1, "owner at the helm, crew in seat 1");
		cleanUp(helper, List.of(owner, a, b), ship, zombie);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ships_hull")
	public void hullTakesDamageAndShipSinks(GameTestHelper helper) {
		helper.getLevel().getServer().setPvpAllowed(true);
		pool(helper);
		ServerPlayer owner = sailor(helper);
		ServerPlayer enemy = sailor(helper);
		ShipService.grant(owner, ShipClass.SLOOP);
		ShipService.launch(owner, ShipClass.SLOOP, owner.position(), 12);
		PirateShip ship = (PirateShip) owner.getVehicle();
		float full = ship.hull();
		helper.assertTrue(!ship.hurt(helper.getLevel().damageSources().playerAttack(owner), 50), "owners can't damage their own ship");
		helper.assertTrue(ship.hurt(helper.getLevel().damageSources().playerAttack(enemy), 50) && ship.hull() == full - 50, "enemies chip the hull");
		helper.assertTrue(!ship.isRemoved(), "a few hits don't break it like a boat");
		ship.hurt(helper.getLevel().damageSources().playerAttack(enemy), full);
		helper.assertTrue(ship.isRemoved() && owner.getVehicle() == null, "at zero hull it sinks and the crew is thrown off");
		helper.assertTrue(ShipData.get(owner.server).active(owner.getUUID()) == null, "no active ship after sinking");
		helper.assertTrue(ShipService.launch(owner, ShipClass.SLOOP, owner.position(), 12).equals("ship_sunk"), "relaunch waits for repairs");
		ShipData.get(owner.server).setSunkUntil(owner.getUUID(), 0);
		cleanUp(helper, List.of(owner, enemy));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ships_save")
	public void shipSavesOwnerAndHullAndStaleShipsLeave(GameTestHelper helper) {
		PirateShip ship = ShipEntities.BRIG.create(helper.getLevel());
		UUID owner = UUID.randomUUID();
		ship.setOwner(owner, com.grandline.player.TeamService.Side.MARINES);
		ship.setHull(123);
		CompoundTag tag = new CompoundTag();
		ship.saveWithoutId(tag);
		PirateShip loaded = ShipEntities.BRIG.create(helper.getLevel());
		loaded.load(tag);
		helper.assertTrue(owner.equals(loaded.owner()) && loaded.hull() == 123 && loaded.side() == 2, "owner, hull and side survive save/load");
		helper.assertTrue(loaded.shipClass() == ShipClass.BRIG && loaded.maxHull() == (float) ShipClass.BRIG.maxHull(), "class comes from the type");

		// A ship that is not its owner's active ship removes itself (e.g. replaced while its chunk was unloaded).
		ShipData.get(helper.getLevel().getServer()).setActive(owner, UUID.randomUUID());
		Vec3 pos = helper.absoluteVec(new Vec3(4, 2, 4));
		loaded.setUUID(UUID.randomUUID());
		loaded.moveTo(pos.x, pos.y, pos.z);
		helper.getLevel().addFreshEntity(loaded);
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(loaded.isRemoved(), "a stale ship removes itself");
			helper.succeed();
		});
	}
}
