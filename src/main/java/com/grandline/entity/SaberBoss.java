package com.grandline.entity;

import com.grandline.GrandLineMod;
import com.grandline.combat.fx.FxEvents;
import com.grandline.gear.Gear;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Verdant Wilds temple boss: a swordsman who fights with the Saber inside the temple hall. Besides plain sword
 * swings it has three telegraphed attacks (each with a wind-up you can read and step away from, then a recovery you
 * can punish):
 * <ul>
 *   <li><b>Saber Slash</b>: raises the blade, then one wide, fast cut in front of it.</li>
 *   <li><b>Flying Saber Slash</b>: draws the blade back, locks its aim, then hurls a crescent of sword energy in a
 *   straight line. Sidestep it.</li>
 *   <li><b>Heavy Combo</b>: crouches, then three lunging cuts (left, right, overhead), then stands winded.</li>
 * </ul>
 * It stays inside its arena: targets that leave it are dropped, the boss walks back to its spot, and with no one in the
 * arena it recovers to full health. No single hit can take more than {@code saberBossMaxHitPercent} of a player's
 * maximum health. Its Saber goes to the player who dealt the most damage (see {@link #die}).
 */
public class SaberBoss extends GrandLineEnemy {
	public static final int NONE = 0, SLASH = 1, FLYING = 2, COMBO = 3;

	// Attack timelines (ticks from the start of the attack).
	public static final int SLASH_HIT = 10, SLASH_END = 22;
	public static final int FLY_AIM = 12, FLY_RELEASE = 16, FLY_END = 26;
	public static final int[] COMBO_HITS = {14, 22, 32};
	public static final int COMBO_END = 60;

	private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(SaberBoss.class, EntityDataSerializers.INT);

	private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.grandline.saber_boss"),
			BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
	private final Map<UUID, Float> damageBy = new HashMap<>();

	private BlockPos home;
	private float homeYaw;
	private int attackTick;
	private int slashCooldown = 30, flyCooldown = 60, comboCooldown = 90;
	private Vec3 flyDirection = Vec3.ZERO;
	private int noTargetTicks;
	private int emptyArenaTicks;
	private boolean engaged;
	private boolean rewarded;

	public SaberBoss(EntityType<? extends SaberBoss> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return GrandLineEnemy.createAttributes()
				.add(Attributes.MAX_HEALTH, 400.0)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
				.add(Attributes.ARMOR, 6.0);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(ATTACK, NONE);
	}

	/** Client: the attack being shown and the tick it started on (for poses and effects). */
	public int clientAttack;
	public int clientAttackStart;

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (ATTACK.equals(key) && level().isClientSide) {
			clientAttack = entityData.get(ATTACK);
			clientAttackStart = tickCount;
		}
	}

	/** The attack being performed (client: drives the pose). */
	public int attack() {
		return entityData.get(ATTACK);
	}

	private void setAttack(int attack) {
		entityData.set(ATTACK, attack);
		attackTick = 0;
	}

	@Override
	public EnemyKind kind() {
		return EnemyKind.CAPTAIN; // counts for the island's boss quest; never respawns as a regular enemy
	}

	/** Places the boss at its arena spot, scaled to {@code level}. */
	public void setup(BlockPos home, float yaw, long islandId, int level) {
		this.home = home.immutable();
		this.homeYaw = yaw;
		setOrigin(this.home, islandId);
		setGrandLineLevel(level);
		applyStats(1);
	}

	public BlockPos home() {
		return home;
	}

	/** Health and damage from the config; {@code players} scales health for a group. */
	private void applyStats(int players) {
		var config = GrandLineMod.config();
		double health = config.saberBossHealth * (1 + config.saberBossHealthPerExtraPlayer * Math.max(0, players - 1));
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(config.saberBossSwingDamage);
		setHealth(getMaxHealth());
	}

	@Override
	protected void updateName() {
		setCustomName(Component.translatable("entity.grandline.saber_boss").append(" [Lv. " + grandLineLevel() + "]"));
		setCustomNameVisible(true);
		bossBar.setName(getDisplayName());
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
			@Override
			public boolean canUse() {
				return attack() == NONE && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return attack() == NONE && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::inArena));
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Gear.SABER));
		setDropChance(EquipmentSlot.MAINHAND, 0.0F); // the reward is handed out in die(), once
	}

	// ------------------------------------------------------------------------------------------------ arena

	/** The temple hall: 23 wide, from the back wall to the door, 9 high. */
	public AABB arena() {
		BlockPos h = home == null ? blockPosition() : home;
		return new AABB(h.getX() - 11, h.getY() - 1, h.getZ() - 5, h.getX() + 12, h.getY() + 8, h.getZ() + 14);
	}

	public boolean inArena(LivingEntity e) {
		return e.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()) && arena().inflate(1).contains(e.position());
	}

	private List<ServerPlayer> playersInArena() {
		return ((ServerLevel) level()).getEntitiesOfClass(ServerPlayer.class, arena(), this::inArena);
	}

	// ------------------------------------------------------------------------------------------------ AI

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		bossBar.setProgress(getHealth() / getMaxHealth());
		if (home == null) {
			home = blockPosition();
		}
		slashCooldown = Math.max(0, slashCooldown - 1);
		flyCooldown = Math.max(0, flyCooldown - 1);
		comboCooldown = Math.max(0, comboCooldown - 1);

		LivingEntity target = getTarget();
		if (target != null && !inArena(target)) {
			setTarget(null); // left the temple: the fight is over for them
			target = null;
		}
		List<ServerPlayer> inside = playersInArena();
		if (!engaged && target != null) {
			engaged = true;
			applyStatsKeepingRatio(Math.max(1, inside.size()));
		}
		if (inside.isEmpty()) {
			emptyArenaTicks++;
			if (emptyArenaTicks >= 100) {
				reset();
			}
		} else {
			emptyArenaTicks = 0;
		}

		if (attack() != NONE) {
			runAttack(target);
			return;
		}
		if (target == null) {
			noTargetTicks++;
			if (noTargetTicks > 40) {
				returnHome();
			}
			return;
		}
		noTargetTicks = 0;
		if (!arena().contains(position())) {
			returnHome(); // knocked out of the hall: walk back in first
			return;
		}
		chooseAttack(target);
	}

	private void applyStatsKeepingRatio(int players) {
		float ratio = getHealth() / getMaxHealth();
		var config = GrandLineMod.config();
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(config.saberBossHealth * (1 + config.saberBossHealthPerExtraPlayer * (players - 1)));
		setHealth(getMaxHealth() * ratio);
	}

	/** No one left in the arena: back to the altar, full health, fight forgotten. */
	private void reset() {
		if (!engaged && getHealth() >= getMaxHealth()) {
			returnHome();
			return;
		}
		engaged = false;
		damageBy.clear();
		setAttack(NONE);
		setTarget(null);
		applyStats(1);
		returnHome();
	}

	private void returnHome() {
		if (home == null) {
			return;
		}
		double d = position().distanceToSqr(Vec3.atBottomCenterOf(home));
		if (d > 32 * 32) {
			teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5); // never wander off the island
		} else if (d > 1.5) {
			getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
		} else if (getNavigation().isDone()) {
			setYRot(homeYaw);
			setYBodyRot(homeYaw);
			setYHeadRot(homeYaw);
		}
	}

	private void chooseAttack(LivingEntity target) {
		double dist = distanceTo(target);
		boolean sees = getSensing().hasLineOfSight(target);
		if (dist <= 3.4 && comboCooldown == 0 && getRandom().nextFloat() < 0.5F) {
			start(COMBO, SoundEvents.RAVAGER_ROAR, 0.6F, 1.6F);
		} else if (dist <= 3.8 && slashCooldown == 0) {
			start(SLASH, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 0.6F);
		} else if (dist >= 5 && dist <= 20 && sees && flyCooldown == 0) {
			start(FLYING, SoundEvents.TRIDENT_RIPTIDE_1, 0.9F, 1.5F);
		} else if (dist > 5 && getNavigation().isStuck() && sees && flyCooldown < 30) {
			flyCooldown = 0; // stuck on the hall's pillars: reach the target from range instead
		}
	}

	private void start(int attack, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		setAttack(attack);
		getNavigation().stop();
		level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, volume, pitch);
		Vec3 forward = forward();
		switch (attack) {
			case SLASH -> FxEvents.sendFrom(this, "boss_warn_slash", position(), forward, 0, 3.8, SLASH_HIT);
			case FLYING -> FxEvents.sendFrom(this, "boss_warn_fly", position(), forward, 0, 0, FLY_RELEASE);
			case COMBO -> FxEvents.sendFrom(this, "boss_warn_combo", position(), forward, 0, 3.5, COMBO_HITS[0]);
			default -> { }
		}
	}

	private Vec3 forward() {
		float yaw = getYRot() * Mth.DEG_TO_RAD;
		return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
	}

	private void faceTarget(LivingEntity target, float maxTurn) {
		if (target == null) {
			return;
		}
		double dx = target.getX() - getX(), dz = target.getZ() - getZ();
		float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
		float yaw = Mth.approachDegrees(getYRot(), want, maxTurn);
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
		getLookControl().setLookAt(target, 30, 30);
	}

	private void runAttack(LivingEntity target) {
		attackTick++;
		getNavigation().stop();
		var config = GrandLineMod.config();
		switch (attack()) {
			case SLASH -> {
				if (attackTick < SLASH_HIT - 3) {
					faceTarget(target, 20); // tracks you during the wind-up, then commits
				}
				if (attackTick == SLASH_HIT) {
					swing(InteractionHand.MAIN_HAND);
					cut(3.8, 120, (float) config.saberBossSlashDamage, 0.9, "boss_slash", 0);
				}
				if (attackTick >= SLASH_END) {
					slashCooldown = 45;
					setAttack(NONE);
				}
			}
			case FLYING -> {
				if (attackTick <= FLY_AIM) {
					faceTarget(target, 25);
					if (target != null) {
						Vec3 to = target.position().subtract(position()).multiply(1, 0, 1);
						flyDirection = to.lengthSqr() < 1e-4 ? forward() : to.normalize();
					}
				}
				if (attackTick == FLY_RELEASE) {
					swing(InteractionHand.MAIN_HAND);
					SaberWave.launch(this, position().add(0, 1.1, 0).add(flyDirection.scale(0.8)), flyDirection,
							(float) config.saberBossFlyingDamage);
				}
				if (attackTick >= FLY_END) {
					flyCooldown = 110;
					setAttack(NONE);
				}
			}
			case COMBO -> {
				for (int i = 0; i < COMBO_HITS.length; i++) {
					int hit = COMBO_HITS[i];
					if (attackTick < hit - 3 && attackTick > (i == 0 ? 0 : COMBO_HITS[i - 1])) {
						faceTarget(target, 18);
					}
					if (attackTick == hit - 2) {
						Vec3 lunge = forward().scale(0.55);
						setDeltaMovement(lunge.x, getDeltaMovement().y, lunge.z);
						hasImpulse = true;
					}
					if (attackTick == hit) {
						swing(i == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
						boolean last = i == COMBO_HITS.length - 1;
						float dmg = (float) (last ? config.saberBossComboFinisherDamage : config.saberBossComboDamage);
						cut(3.5, 110, dmg, last ? 1.3 : 0.4, "boss_slash", i + 1);
					}
				}
				if (attackTick >= COMBO_END) {
					comboCooldown = 150;
					slashCooldown = Math.max(slashCooldown, 20);
					setAttack(NONE);
				}
			}
			default -> setAttack(NONE);
		}
	}

	/**
	 * A sword cut: every target within {@code range} and inside the {@code arcDegrees} arc in front is hit once. The
	 * visual arc is drawn the same tick. {@code style}: 0 slash, 1 left-to-right, 2 right-to-left, 3 overhead.
	 */
	private void cut(double range, double arcDegrees, float damage, double knockback, String fx, int style) {
		Vec3 forward = forward();
		Vec3 origin = position().add(0, 1.0, 0);
		FxEvents.sendFrom(this, fx, origin, forward, 0, range, style);
		level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 0.8F + style * 0.08F);
		double cos = Math.cos(Math.toRadians(arcDegrees / 2));
		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range + 1),
				e -> e != this && e.isAlive() && !(e instanceof Islander) && !(e instanceof GrandLineEnemy))) {
			Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
			double dist = to.length() - e.getBbWidth() / 2;
			if (dist > range || Math.abs(e.getY() - getY()) > 2.5) {
				continue;
			}
			if (to.lengthSqr() > 0.25 && to.normalize().dot(forward) < cos) {
				continue; // outside the arc: stepped aside or got behind the boss
			}
			if (!clearPath(origin, e.position().add(0, e.getBbHeight() / 2, 0))) {
				continue;
			}
			if (hitWithSword(e, damage)) {
				Vec3 push = to.lengthSqr() < 1e-4 ? forward : to.normalize();
				e.knockback(knockback, -push.x, -push.z);
				level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.0F, 0.9F);
			}
		}
	}

	private boolean clearPath(Vec3 from, Vec3 to) {
		BlockHitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		return hit.getType() == HitResult.Type.MISS;
	}

	/** Damage with the per-hit cap (no single hit takes more than the configured share of a player's health). */
	public boolean hitWithSword(LivingEntity target, float damage) {
		if (target instanceof Player) {
			damage = Math.min(damage, (float) (target.getMaxHealth() * GrandLineMod.config().saberBossMaxHitPercent / 100.0));
		}
		target.invulnerableTime = 0; // combo hits come faster than the vanilla hurt cooldown
		return target.hurt(damageSources().mobAttack(this), damage);
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		if (target instanceof LivingEntity living) {
			swing(InteractionHand.MAIN_HAND);
			FxEvents.sendFrom(this, "boss_slash", position().add(0, 1.0, 0), forward(), 0, 3.0, 0);
			return hitWithSword(living, (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
		}
		return super.doHurtTarget(target);
	}

	// ------------------------------------------------------------------------------------------------ damage & reward

	@Override
	public boolean hurt(DamageSource source, float amount) {
		float before = getHealth();
		boolean hurt = super.hurt(source, amount);
		if (hurt && source.getEntity() instanceof ServerPlayer player) {
			damageBy.merge(player.getUUID(), Math.max(0, before - getHealth()), Float::sum);
			if (getTarget() == null && inArena(player)) {
				setTarget(player);
			}
		}
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		boolean first = !rewarded && !isRemoved() && !dead;
		super.die(source);
		if (first && !level().isClientSide) {
			rewarded = true;
			dropSaber();
		}
	}

	/**
	 * Exactly one Saber per defeat. It goes to the player who dealt the most damage; if they have fallen (or left), to
	 * the next living fighter; if no fighter is alive, it is left at the boss's spot for the top fighter alone to pick
	 * up, and it never despawns.
	 */
	private void dropSaber() {
		List<Map.Entry<UUID, Float>> ranked = new java.util.ArrayList<>(damageBy.entrySet());
		ranked.removeIf(e -> e.getValue() <= 0);
		if (ranked.isEmpty()) {
			return; // not a fight (removed by a command, killed by the world): no reward
		}
		ranked.sort(Map.Entry.<UUID, Float>comparingByValue().reversed());
		ItemStack saber = new ItemStack(Gear.SABER);
		for (Map.Entry<UUID, Float> e : ranked) {
			ServerPlayer p = level().getServer().getPlayerList().getPlayer(e.getKey());
			if (p != null && p.isAlive() && p.level() == level() && p.distanceTo(this) < 64) {
				if (!p.getInventory().add(saber)) {
					ItemEntity item = new ItemEntity(level(), p.getX(), p.getY() + 0.5, p.getZ(), saber);
					item.setTarget(p.getUUID());
					item.setNoPickUpDelay();
					level().addFreshEntity(item);
				}
				p.sendSystemMessage(Component.translatable("message.grandline.saber_boss.reward").withStyle(net.minecraft.ChatFormatting.GOLD));
				level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.9F, 1.0F);
				return;
			}
		}
		ItemEntity item = new ItemEntity(level(), getX(), getY() + 0.5, getZ(), saber);
		item.setTarget(ranked.get(0).getKey());
		item.setUnlimitedLifetime();
		level().addFreshEntity(item);
	}

	/** Players who fought it (for tests). */
	public Map<UUID, Float> damageBy() {
		return damageBy;
	}

	// ------------------------------------------------------------------------------------------------ boss bar & saving

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossBar.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossBar.removePlayer(player);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (home != null) {
			tag.putLong("SaberHome", home.asLong());
		}
		tag.putFloat("SaberHomeYaw", homeYaw);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("SaberHome")) {
			home = BlockPos.of(tag.getLong("SaberHome"));
		}
		homeYaw = tag.getFloat("SaberHomeYaw");
		updateName();
	}
}
