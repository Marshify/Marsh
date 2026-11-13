package com.grandline.ship;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.network.ServerRequestGuard;
import com.grandline.network.payload.ActionResultPayload;
import com.grandline.network.payload.CallShipPayload;
import com.grandline.network.payload.ShipSyncPayload;
import com.grandline.player.PlayerProfileService;
import com.grandline.player.TeamService;
import com.grandline.reputation.ReputationService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Buying, launching and calling ships, and sinking them. Every result is a short key; the caller shows
 * {@code npc.grandline.result.<key>}. One active ship per player: launching again replaces the old one.
 */
public final class ShipService {
	/** How far from the player Call Ship looks for open water. */
	public static final int CALL_RADIUS = 12;
	/** How far from an island's dock a Navigator launch looks for open water. */
	public static final int DOCK_RADIUS = 16;
	/** Without a dock (or no water there), how far from the Navigator to look. */
	public static final int NAVIGATOR_RADIUS = 64;

	private ShipService() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));
		ServerPlayNetworking.registerGlobalReceiver(CallShipPayload.TYPE, (packet, player, sender) -> {
			if (ServerRequestGuard.check(player, CallShipPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
				return;
			}
			reply(player, call(player));
		});
	}

	public static ShipOwnership ownership(ServerPlayer player) {
		return player.getAttachedOrElse(ModAttachments.SHIPS, ShipOwnership.EMPTY);
	}

	static void setOwnership(ServerPlayer player, ShipOwnership ships) {
		player.setAttached(ModAttachments.SHIPS, ships);
		sync(player);
	}

	/** Gives a ship without payment (admin command, tests). */
	public static void grant(ServerPlayer player, ShipClass ship) {
		setOwnership(player, ownership(player).withShip(ship));
	}

	public static String buy(ServerPlayer player, ShipClass ship) {
		if (ownership(player).owns(ship)) {
			return "ship_owned";
		}
		if (PlayerProfileService.get(player).level() < ship.minLevel()) {
			return "ship_level";
		}
		long price = ship.price();
		if (PlayerProfileService.get(player).currency() < price) {
			return "ship_money";
		}
		PlayerProfileService.update(player, p -> p.withCurrency(p.currency() - price));
		grant(player, ship);
		return "ship_bought";
	}

	/** Call Ship key: the selected ship, on open water near the player. */
	public static String call(ServerPlayer player) {
		ShipClass ship = ownership(player).callable();
		if (ship == null) {
			return "ship_none";
		}
		ShipData data = ShipData.get(player.server);
		long wait = data.calledAt(player.getUUID()) + GrandLineMod.config().shipCallCooldownSeconds * 1000L - System.currentTimeMillis();
		if (wait > 0) {
			return "ship_cooldown";
		}
		String result = launch(player, ship, player.position(), CALL_RADIUS);
		if (result.equals("ship_launched")) {
			data.setCalledAt(player.getUUID(), System.currentTimeMillis());
		}
		return result;
	}

	/** Launches {@code ship} on open water within {@code radius} of {@code near} and puts the owner at the helm. */
	public static String launch(ServerPlayer player, ShipClass ship, Vec3 near, int radius) {
		if (!ownership(player).owns(ship)) {
			return "ship_not_owned";
		}
		if (ReputationService.isTagged(player)) {
			return "ship_combat";
		}
		ShipData data = ShipData.get(player.server);
		if (data.sunkUntil(player.getUUID()) > System.currentTimeMillis()) {
			return "ship_sunk";
		}
		ServerLevel level = player.serverLevel();
		BlockPos water = findWater(level, BlockPos.containing(near), radius, ship);
		if (water == null) {
			return "ship_no_water";
		}
		removeActive(player);
		PirateShip entity = ShipEntities.typeOf(ship).create(level);
		if (entity == null) {
			return "ship_no_water";
		}
		double dx = water.getX() + 0.5 - near.x;
		double dz = water.getZ() + 0.5 - near.z;
		float yaw = dx * dx + dz * dz < 0.01 ? player.getYRot() : (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG); // bow away from shore
		entity.moveTo(water.getX() + 0.5, water.getY() + 0.9, water.getZ() + 0.5, yaw, 0);
		entity.setOwner(player.getUUID(), TeamService.get(player));
		level.addFreshEntity(entity);
		data.setActive(player.getUUID(), entity.getUUID());
		setOwnership(player, ownership(player).select(ship));
		player.stopRiding();
		player.startRiding(entity);
		level.playSound(null, entity.blockPosition(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.NEUTRAL, 1.0F, 0.7F);
		return "ship_launched";
	}

	/**
	 * Navigator launch: at the nearest dock of the Navigator's island (islands are big, so the Navigator itself is often
	 * far from open water), else anywhere near the Navigator.
	 */
	public static String launchFromNavigator(ServerPlayer player, ShipClass ship, com.grandline.entity.Islander npc) {
		com.grandline.world.Island island = com.grandline.world.IslandRegistry.get(player.server).get(npc.islandId());
		BlockPos best = null;
		if (island != null) {
			for (com.grandline.world.plan.Anchor dock : com.grandline.world.plan.IslandPlans.anchors(island, com.grandline.world.plan.Anchor.Type.DOCK)) {
				BlockPos pos = new BlockPos(island.x() + dock.x(), com.grandline.world.plan.Terrain.WATER_TOP, island.z() + dock.z());
				if (best == null || pos.distSqr(npc.blockPosition()) < best.distSqr(npc.blockPosition())) {
					best = pos;
				}
			}
		}
		if (best != null) {
			String result = launch(player, ship, Vec3.atCenterOf(best), DOCK_RADIUS);
			if (!result.equals("ship_no_water")) {
				return result;
			}
		}
		return launch(player, ship, npc.position(), NAVIGATOR_RADIUS);
	}

	/** Removes the player's current ship if its chunk is loaded; otherwise it removes itself when next loaded. */
	private static void removeActive(ServerPlayer player) {
		UUID current = ShipData.get(player.server).active(player.getUUID());
		if (current == null) {
			return;
		}
		for (ServerLevel level : player.server.getAllLevels()) {
			Entity old = level.getEntity(current);
			if (old instanceof PirateShip) {
				old.ejectPassengers();
				old.discard();
			}
		}
	}

	/** The ship's hull reached zero: it sinks (no drops) and the owner waits before launching again. */
	public static void sink(PirateShip ship) {
		if (ship.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.EXPLOSION, ship.getX(), ship.getY() + 1, ship.getZ(), 6, ship.getBbWidth() / 2, 0.5, ship.getBbWidth() / 2, 0);
			level.sendParticles(ParticleTypes.SPLASH, ship.getX(), ship.getY() + 0.5, ship.getZ(), 80, ship.getBbWidth() / 2, 0.2, ship.getBbWidth() / 2, 0.2);
			level.playSound(null, ship.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 1.0F, 0.8F);
		}
		UUID owner = ship.owner();
		ship.ejectPassengers();
		ship.discard();
		if (owner == null || ship.getServer() == null) {
			return;
		}
		ShipData data = ShipData.get(ship.getServer());
		data.clearActive(owner, ship.getUUID());
		data.setSunkUntil(owner, System.currentTimeMillis() + GrandLineMod.config().shipSinkCooldownSeconds * 1000L);
		ServerPlayer player = ship.getServer().getPlayerList().getPlayer(owner);
		if (player != null) {
			player.displayClientMessage(Component.translatable("message.grandline.ship.sunk", ship.shipClass().displayName(),
					GrandLineMod.config().shipSinkCooldownSeconds).withStyle(ChatFormatting.RED), false);
			sync(player);
		}
	}

	/**
	 * Closest open water within {@code radius}: a water surface with water under the whole hitbox (plus a block of
	 * margin) and two blocks of air above it. Returns the surface water block, or null.
	 */
	public static BlockPos findWater(Level level, BlockPos center, int radius, ShipClass ship) {
		int half = Mth.ceil(ship.width / 2) + 1;
		for (int r = 0; r <= radius; r++) {
			BlockPos best = null;
			double bestDist = Double.MAX_VALUE;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue; // ring only
					}
					BlockPos surface = surface(level, center.offset(dx, 0, dz));
					if (surface != null && clear(level, surface, half)) {
						double dist = dx * dx + dz * dz;
						if (dist < bestDist) {
							bestDist = dist;
							best = surface;
						}
					}
				}
			}
			if (best != null) {
				return best;
			}
		}
		return null;
	}

	/** The top water block of the column near {@code pos} (searching 8 up and 12 down), or null. */
	private static BlockPos surface(Level level, BlockPos pos) {
		for (int dy = 8; dy >= -12; dy--) {
			BlockPos p = pos.above(dy);
			if (level.getFluidState(p).is(FluidTags.WATER) && level.getFluidState(p).isSource() && level.getBlockState(p.above()).isAir()) {
				return p;
			}
		}
		return null;
	}

	private static boolean clear(Level level, BlockPos surface, int half) {
		for (int x = -half; x <= half; x++) {
			for (int z = -half; z <= half; z++) {
				BlockPos p = surface.offset(x, 0, z);
				if (!level.getFluidState(p).is(FluidTags.WATER) || !level.getBlockState(p.above()).isAir() || !level.getBlockState(p.above(2)).isAir()) {
					return false;
				}
			}
		}
		return true;
	}

	public static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, ShipSyncPayload.TYPE)) {
			ShipOwnership ships = ownership(player);
			long sunk = Math.max(0, ShipData.get(player.server).sunkUntil(player.getUUID()) - System.currentTimeMillis());
			ShipClass callable = ships.callable();
			ServerPlayNetworking.send(player, new ShipSyncPayload(ships.owned(), callable == null ? "" : callable.id, sunk));
		}
	}

	static void reply(ServerPlayer player, String result) {
		if (ServerPlayNetworking.canSend(player, ActionResultPayload.TYPE)) {
			boolean ok = result.equals("ship_launched") || result.equals("ship_bought");
			ServerPlayNetworking.send(player, new ActionResultPayload(ok, "npc.grandline.result." + result));
		}
	}
}
