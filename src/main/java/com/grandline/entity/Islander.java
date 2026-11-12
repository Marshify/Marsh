package com.grandline.entity;

import com.grandline.fruit.FruitDealer;
import com.grandline.npc.NpcService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A friendly, stationary island NPC. Its {@link Role} decides what talking to it does. Invulnerable and never despawns.
 * Remembers which island it belongs to (for quests).
 */
public class Islander extends PathfinderMob implements FruitDealer {
	public enum Role {
		QUEST_GIVER, FRUIT_DEALER, FISHERMAN, NAVIGATOR;

		public static Role byId(int id) {
			Role[] values = values();
			return id >= 0 && id < values.length ? values[id] : QUEST_GIVER;
		}
	}

	/** Synced to clients so they can pick the right skin. */
	private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(Islander.class, EntityDataSerializers.INT);
	private long islandId;
	private int islandLevel = 1;

	public Islander(EntityType<? extends Islander> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		setInvulnerable(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(ROLE, Role.QUEST_GIVER.ordinal());
	}

	public Role role() {
		return Role.byId(entityData.get(ROLE));
	}

	public long islandId() {
		return islandId;
	}

	public int islandLevel() {
		return islandLevel;
	}

	public void setup(Role role, long islandId, int islandLevel) {
		entityData.set(ROLE, role.ordinal());
		this.islandId = islandId;
		this.islandLevel = islandLevel;
		setCustomName(Component.translatable("entity.grandline.islander." + role.name().toLowerCase(java.util.Locale.ROOT)));
		setCustomNameVisible(true);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		com.grandline.GrandLineMod.LOGGER.debug("Islander interact client={} role={} hand={}", level().isClientSide, role(), hand);
		if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
			NpcService.open(serverPlayer, this);
		}
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	@Override
	public boolean isFruitDealer() {
		return role() == Role.FRUIT_DEALER;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return !source.isCreativePlayer() || super.isInvulnerableTo(source);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("GrandLineRole", role().ordinal());
		tag.putLong("GrandLineIsland", islandId);
		tag.putInt("GrandLineIslandLevel", islandLevel);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(ROLE, Role.byId(tag.getInt("GrandLineRole")).ordinal());
		islandId = tag.getLong("GrandLineIsland");
		islandLevel = Math.max(1, tag.getInt("GrandLineIslandLevel"));
	}
}
