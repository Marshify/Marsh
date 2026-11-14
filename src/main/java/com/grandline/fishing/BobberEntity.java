package com.grandline.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * The Grand Line fishing bobber: thrown from the Starter Fishing Rod, it flies in an arc, then floats and bobs on the
 * water. The server owns its state (flying, floating, bite, hooked); clients draw it and the line back to the angler's
 * rod. It never saves to the world: fishing is only ever live.
 */
public class BobberEntity extends Entity {
	public static final int FLYING = 0, FLOATING = 1, BITE = 2, HOOKED = 3, GROUNDED = 4;
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(BobberEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(BobberEntity.class, EntityDataSerializers.INT);

	public BobberEntity(EntityType<? extends BobberEntity> type, Level level) {
		super(type, level);
		noCulling = true;
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(OWNER, -1);
		entityData.define(STATE, FLYING);
	}

	public void setOwner(Player player) {
		entityData.set(OWNER, player.getId());
	}

	public Player owner() {
		Entity e = level().getEntity(entityData.get(OWNER));
		return e instanceof Player p ? p : null;
	}

	public int state() {
		return entityData.get(STATE);
	}

	public void setState(int state) {
		entityData.set(STATE, state);
	}

	/** The water surface height at the bobber's column, or NaN if it is not over water. */
	private double surface() {
		BlockPos pos = blockPosition();
		for (int i = 0; i < 3; i++) {
			FluidState fluid = level().getFluidState(pos);
			if (fluid.is(FluidTags.WATER)) {
				BlockPos above = pos.above();
				if (!level().getFluidState(above).is(FluidTags.WATER)) {
					return pos.getY() + fluid.getHeight(level(), pos);
				}
				pos = above;
			} else {
				return i == 0 ? Double.NaN : pos.getY();
			}
		}
		return Double.NaN;
	}

	public boolean onWater() {
		int s = state();
		return s == FLOATING || s == BITE || s == HOOKED;
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 v = getDeltaMovement();
		double surface = surface();
		if (!Double.isNaN(surface) && getY() < surface + 0.25) {
			// Float: drift to just under the surface and bob; a bite pulls it under for a moment.
			if (state() == FLYING && !level().isClientSide) {
				setState(FLOATING);
			}
			double target = surface - 0.12 + Math.sin(tickCount * 0.15) * 0.03 - (state() == BITE ? 0.22 : 0) - (state() == HOOKED ? 0.1 : 0);
			v = new Vec3(v.x * 0.75, v.y * 0.6 + (target - getY()) * 0.25, v.z * 0.75);
		} else {
			v = v.add(0, -0.03, 0).scale(0.94); // in the air (or off the water): it falls
			if (onGround()) {
				v = v.multiply(0.5, 1, 0.5);
			}
		}
		setDeltaMovement(v);
		move(MoverType.SELF, v);
		if (onGround() && state() == FLYING && !level().isClientSide) {
			setState(GROUNDED); // landed on land: not a valid cast
		}
		if (!level().isClientSide && (tickCount > 20 * 120 || owner() == null || owner().distanceTo(this) > 40)) {
			FishingSessions.lost(this);
		}
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 64 * 64;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}
}
