package com.grandline.ship;

import com.grandline.GrandLineMod;
import com.grandline.player.TeamService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A player's ship. Built on the vanilla boat (same water physics, client-side steering and server checks) with:
 * a bigger hull and more seats per {@link ShipClass}; extra speed under sail; hull points instead of breaking on a few
 * hits (it sinks at zero and drops nothing); only the owner steers (they always take the helm); only players board.
 */
public class PirateShip extends Boat {
	private static final EntityDataAccessor<Float> HULL = SynchedEntityData.defineId(PirateShip.class, EntityDataSerializers.FLOAT);
	/** 0 no side, 1 Pirates, 2 Marines: picks the sails. */
	private static final EntityDataAccessor<Integer> SIDE = SynchedEntityData.defineId(PirateShip.class, EntityDataSerializers.INT);
	/** Synced so the owner's client knows it is at the helm (the helm's client runs the ship's movement, as for boats). */
	private static final EntityDataAccessor<java.util.Optional<UUID>> OWNER = SynchedEntityData.defineId(PirateShip.class, EntityDataSerializers.OPTIONAL_UUID);
	/** Ticks without damage before the hull starts repairing itself. */
	private static final int REPAIR_DELAY = 15 * 20;

	private boolean forwardInput;
	private int lastDamagedTick = -REPAIR_DELAY;

	public PirateShip(EntityType<? extends Boat> type, Level level) {
		super(type, level);
	}

	/** From the entity type (also valid during construction, when synced data is defined). */
	public ShipClass shipClass() {
		return ShipEntities.classOf(getType());
	}

	public UUID owner() {
		return entityData.get(OWNER).orElse(null);
	}

	public void setOwner(UUID owner, TeamService.Side side) {
		entityData.set(OWNER, java.util.Optional.ofNullable(owner));
		entityData.set(SIDE, side == null ? 0 : side.ordinal() + 1);
	}

	/** 0 no side, 1 Pirates, 2 Marines. */
	public int side() {
		return entityData.get(SIDE);
	}

	public float hull() {
		return entityData.get(HULL);
	}

	public float maxHull() {
		return (float) shipClass().maxHull();
	}

	public void setHull(float hull) {
		entityData.set(HULL, Mth.clamp(hull, 0, maxHull()));
	}

	public boolean isOwner(Entity entity) {
		UUID owner = owner();
		return owner != null && entity != null && owner.equals(entity.getUUID());
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(HULL, (float) shipClass().maxHull());
		entityData.define(SIDE, 0);
		entityData.define(OWNER, java.util.Optional.empty());
	}

	// ------------------------------------------------------------------------------------------------ riding

	@Override
	protected int getMaxPassengers() {
		return shipClass().seats();
	}

	/** Players only. Seat 0 (the helm) is kept for the owner; everyone else shares the other seats. */
	@Override
	protected boolean canAddPassenger(Entity passenger) {
		if (!(passenger instanceof Player) || isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) {
			return false;
		}
		if (isOwner(passenger)) {
			return true;
		}
		long others = getPassengers().stream().filter(p -> !isOwner(p)).count();
		return others < shipClass().seats() - 1;
	}

	/** Only the owner steers. Without the owner aboard the ship drifts. */
	@Override
	public LivingEntity getControllingPassenger() {
		for (Entity passenger : getPassengers()) {
			if (isOwner(passenger) && passenger instanceof LivingEntity living) {
				return living;
			}
		}
		return null;
	}

	/** The seat a passenger sits in: the owner at the helm (0), others in boarding order. */
	public int seatOf(Entity passenger) {
		if (isOwner(passenger)) {
			return 0;
		}
		int seat = 1;
		for (Entity p : getPassengers()) {
			if (p == passenger) {
				return seat;
			}
			if (!isOwner(p)) {
				seat++;
			}
		}
		return seat;
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction move) {
		if (!hasPassenger(passenger)) {
			return;
		}
		int seatIndex = seatOf(passenger);
		float[] seat = shipClass().seat(seatIndex);
		Vec3 offset = new Vec3(seat[1], 0, seat[0]).yRot(-getYRot() * Mth.DEG_TO_RAD - Mth.HALF_PI);
		double up = shipClass().deck + (seatIndex == 0 ? shipClass().helmHeight : 0);
		move.accept(passenger, getX() + offset.x, getY() + up + passenger.getMyRidingOffset(), getZ() + offset.z);
		// Turn riders with the ship, as a boat does.
		float turned = getYRot() - yRotO;
		passenger.setYRot(passenger.getYRot() + turned);
		passenger.setYHeadRot(passenger.getYHeadRot() + turned);
	}

	@Override
	public double getPassengersRidingOffset() {
		return shipClass().deck;
	}

	// ------------------------------------------------------------------------------------------------ sailing

	@Override
	public void setInput(boolean left, boolean right, boolean forward, boolean back) {
		super.setInput(left, right, forward, back);
		this.forwardInput = forward;
	}

	/** Water under the hull (the ship sits on the surface). */
	public boolean onWater() {
		BlockPos below = BlockPos.containing(getX(), getY() - 0.25, getZ());
		return !level().getFluidState(below).isEmpty() || !level().getFluidState(blockPosition()).isEmpty();
	}

	@Override
	public void tick() {
		boolean water = onWater();
		if (isControlledByLocalInstance() && getControllingPassenger() != null && forwardInput && water) {
			float yaw = getYRot() * Mth.DEG_TO_RAD;
			setDeltaMovement(getDeltaMovement().add(Mth.sin(-yaw) * shipClass().thrust, 0, Mth.cos(yaw) * shipClass().thrust));
		}
		super.tick();
		if (isControlledByLocalInstance()) {
			if (!water) {
				setDeltaMovement(getDeltaMovement().multiply(0.3, 1, 0.3)); // aground: ships don't slide over land
			}
			keepAfloat();
		}
		if (!level().isClientSide) {
			serverTick();
		}
	}

	/** How deep the hull sits: its bottom is this far below the water surface. */
	private static final double DRAFT = 0.3;

	/**
	 * Holds the hull at {@link #DRAFT} below the real water surface. Vanilla boat buoyancy assumes a hull about half a
	 * block tall; with a taller hitbox it reads the water level from the wrong block and lets the ship sink.
	 */
	private void keepAfloat() {
		double surface = waterSurface();
		if (Double.isNaN(surface)) {
			return; // on land or in the air: vanilla handles it
		}
		double dy = surface - DRAFT - getY();
		if (dy < -1.0) {
			return; // still falling into the water from above
		}
		setPos(getX(), getY() + Mth.clamp(dy * 0.35, -0.2, 0.25), getZ());
		Vec3 v = getDeltaMovement();
		setDeltaMovement(v.x, 0, v.z);
	}

	/** Height of the water surface in this column, or NaN if there is no water just under or around the hull. */
	public double waterSurface() {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		int start = Mth.floor(getY()) - 2;
		int first = Integer.MIN_VALUE;
		for (int y = start; y <= start + 4; y++) {
			p.set(getX(), y, getZ());
			if (level().getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)) {
				first = y;
				break;
			}
		}
		if (first == Integer.MIN_VALUE) {
			return Double.NaN;
		}
		int y = first;
		while (y < level().getMaxBuildHeight() - 1) { // climb to the top of the water (refloats a ship that went under)
			p.set(getX(), y + 1, getZ());
			if (!level().getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)) {
				break;
			}
			y++;
		}
		p.set(getX(), y, getZ());
		return y + level().getFluidState(p).getHeight(level(), p);
	}

	private void serverTick() {
		UUID owner = owner();
		if (owner != null && tickCount % 20 == 0) {
			UUID active = ShipData.get(getServer()).active(owner);
			if (!getUUID().equals(active)) {
				discard(); // replaced by a newer launch of the same owner
				return;
			}
		}
		if (tickCount - lastDamagedTick > REPAIR_DELAY && hull() < maxHull()) {
			setHull(hull() + (float) (maxHull() * GrandLineMod.config().shipRepairPerSecond / 20.0));
		}
	}

	// ------------------------------------------------------------------------------------------------ damage

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (isInvulnerableTo(source)) {
			return false;
		}
		if (level().isClientSide || isRemoved()) {
			return true;
		}
		if (source.getEntity() instanceof ServerPlayer attacker && !canBeDamagedBy(attacker)) {
			return false;
		}
		lastDamagedTick = tickCount;
		setHull(hull() - amount);
		setHurtDir(-getHurtDir());
		setHurtTime(10);
		setDamage(getDamage() + amount * 10.0F);
		markHurt();
		gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
		if (hull() <= 0) {
			ShipService.sink(this);
		}
		return true;
	}

	/** Owners can't hurt their own ship; players can't hurt ships with PvP off, nor Marines their own side's ships. */
	public boolean canBeDamagedBy(ServerPlayer attacker) {
		if (isOwner(attacker) || owner() == null) {
			return owner() == null; // ownerless ships (e.g. summoned) can be broken by anyone
		}
		if (!attacker.server.isPvpAllowed()) {
			return false;
		}
		TeamService.Side attackerSide = TeamService.get(attacker);
		return !(attackerSide == TeamService.Side.MARINES && side() == TeamService.Side.MARINES.ordinal() + 1);
	}

	/** Falling never breaks a ship into planks. */
	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
		resetFallDistance();
	}

	@Override
	public Item getDropItem() {
		return Items.AIR;
	}

	@Override
	public ItemStack getPickResult() {
		return ItemStack.EMPTY;
	}

	// ------------------------------------------------------------------------------------------------ saving

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (owner() != null) {
			tag.putUUID("Owner", owner());
		}
		tag.putFloat("Hull", hull());
		tag.putInt("Side", side());
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(OWNER, tag.hasUUID("Owner") ? java.util.Optional.of(tag.getUUID("Owner")) : java.util.Optional.empty());
		if (tag.contains("Hull")) {
			setHull(tag.getFloat("Hull"));
		}
		entityData.set(SIDE, tag.getInt("Side"));
	}
}
