package com.grandline.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;

/**
 * A level-scaled hostile islander. Health and damage grow with its level, so kill rewards (which scale with max health)
 * grow too. Shows its name and level above its head.
 */
public class GrandLineEnemy extends Monster {
	private static final String LEVEL_KEY = "GrandLineLevel";
	private int grandLineLevel = 1;
	/** Where the island's spawner placed this enemy (its respawn point), and its island; null / -1 if unknown. */
	private net.minecraft.core.BlockPos origin;
	private long islandId = Long.MIN_VALUE; // none (island ids can be negative)

	public GrandLineEnemy(EntityType<? extends GrandLineEnemy> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	public EnemyKind kind() {
		return GrandLineEntities.kindOf(getType());
	}

	public int grandLineLevel() {
		return grandLineLevel;
	}

	/** Sets the level, scales attributes and heals to full. */
	public void setGrandLineLevel(int level) {
		grandLineLevel = Math.max(1, level);
		EnemyKind kind = kind();
		double health = 20.0 * kind.healthScale() * (1.0 + grandLineLevel * 0.06);
		double damage = (3.0 + grandLineLevel * 0.08) * kind.damageScale();
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.min(health, 1_000_000));
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.min(damage, 2048));
		setHealth(getMaxHealth());
		updateName();
	}

	/** Records where this enemy belongs, so it can respawn there (see EnemyRespawns). */
	public void setOrigin(net.minecraft.core.BlockPos origin, long islandId) {
		this.origin = origin;
		this.islandId = islandId;
	}

	public net.minecraft.core.BlockPos origin() {
		return origin;
	}

	public long islandId() {
		return islandId;
	}

	protected void updateName() {
		setCustomName(Component.translatable("entity.grandline." + kind().id()).append(" [Lv. " + grandLineLevel + "]"));
		setCustomNameVisible(true);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
		goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.9));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/**
	 * Camp enemies only chase so far: once one is more than 12 blocks past its camp's edge it gives up and walks home,
	 * so fights stay in the combat areas and never spill into towns or onto arrival docks.
	 */
	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (hasRestriction() && getTarget() != null && tickCount % 10 == 0) {
			double limit = getRestrictRadius() + 12;
			if (getRestrictCenter().distToCenterSqr(position()) > limit * limit) {
				setTarget(null);
				getNavigation().moveTo(getRestrictCenter().getX() + 0.5, getRestrictCenter().getY(), getRestrictCenter().getZ() + 0.5, 1.0);
			}
		}
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(kind().weapon()));
		setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	public void equip() {
		populateDefaultEquipmentSlots(getRandom(), level().getCurrentDifficultyAt(blockPosition()));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt(LEVEL_KEY, grandLineLevel);
		if (origin != null) {
			tag.putLong("GrandLineOrigin", origin.asLong());
			tag.putLong("GrandLineIsland", islandId);
		}
		if (hasRestriction()) {
			tag.putInt("GrandLineLeash", (int) getRestrictRadius());
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		grandLineLevel = Math.max(1, tag.getInt(LEVEL_KEY));
		if (tag.contains("GrandLineOrigin")) {
			origin = net.minecraft.core.BlockPos.of(tag.getLong("GrandLineOrigin"));
			islandId = tag.getLong("GrandLineIsland");
		}
		if (tag.contains("GrandLineLeash")) {
			restrictTo(origin != null ? origin : blockPosition(), tag.getInt("GrandLineLeash"));
		}
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}
}
