package com.grandline.entity;

import com.grandline.config.EnemyBehavior;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;

/**
 * A level-scaled hostile islander. Health and damage grow with its level, so kill rewards (which scale with max health)
 * grow too. Shows its name and level above its head.
 *
 * <p><b>Territory AI</b> (all decided on the server; numbers per enemy kind in {@code enemyBehavior}, see
 * {@link EnemyBehavior}):
 * <ul>
 *   <li>Idle: it stays within {@code idleRadius} of its spawn spot ({@link #home}), wandering and looking around, and
 *   walks back if pushed out.</li>
 *   <li>Aggression: it attacks a player only when they come within {@code aggressionRadius} and in sight (never through
 *   walls). A player who attacks it from further away provokes that enemy only (unless {@code groupAggression}).
 *   Vanilla targeting is replaced entirely: only this logic picks targets.</li>
 *   <li>Pursuit: it gives up once it is {@code pursuitRadius} from its spawn spot (measured from the spot), when the
 *   target enters a protected area ({@link SafeZones}), or when the target dies, leaves, changes dimension or turns
 *   creative/spectator.</li>
 *   <li>Reset: on giving up it drops the target and its attack, walks home (ignoring players on the way unless attacked
 *   inside its territory), heals to full there ({@code healOnReset}) and goes back to idling.</li>
 *   <li>Protected areas: it never notices, chases or attacks a player in one, and a player in one cannot hurt it.</li>
 * </ul>
 * Specialised enemies opt out with {@link #territoryAi()} (the Saber Warden fights by its own arena rules).
 */
public class GrandLineEnemy extends Monster {
	private static final String LEVEL_KEY = "GrandLineLevel";
	private int grandLineLevel = 1;
	/** Where the island's spawner placed this enemy (its respawn point), and its island; null / -1 if unknown. */
	private net.minecraft.core.BlockPos origin;
	private long islandId = Long.MIN_VALUE; // none (island ids can be negative)

	/** The spot it guards and returns to (its spawn spot); set on first tick if it was never given one. */
	private net.minecraft.core.BlockPos home;
	/** Walking back home after giving up a fight. */
	private boolean returning;
	/** Whether its spot has been checked for safe footing (see {@link #safeSpot}). */
	private boolean homeChecked;
	private int returningTicks;
	/** The target this enemy itself chose or was provoked into (any other setTarget call is refused). */
	private boolean engaging;
	private long lastWarnTick;

	public GrandLineEnemy(EntityType<? extends GrandLineEnemy> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	/** An island's own look for this enemy (a texture under textures/entity, e.g. "canopy_primate"); empty: the kind's. */
	private static final net.minecraft.network.syncher.EntityDataAccessor<String> SKIN =
			net.minecraft.network.syncher.SynchedEntityData.defineId(GrandLineEnemy.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(SKIN, "");
	}

	public String skin() {
		return entityData.get(SKIN);
	}

	public void setSkin(String skin) {
		entityData.set(SKIN, skin == null ? "" : skin);
		updateName();
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
		this.home = origin;
		this.homeChecked = false;
		updateName();
	}

	public net.minecraft.core.BlockPos origin() {
		return origin;
	}

	/** The spot it guards (its spawn spot). */
	public net.minecraft.core.BlockPos home() {
		return home != null ? home : origin != null ? origin : blockPosition();
	}

	public boolean returning() {
		return returning;
	}

	/** This enemy's fighting numbers (per kind; race trial waves have their own). */
	public com.grandline.config.EnemyBehavior behavior() {
		var map = com.grandline.GrandLineMod.config().enemyBehavior;
		String key = getTags().contains(com.grandline.race.RaceTrials.TAG) ? "trial" : kind().id();
		var b = !skin().isEmpty() && !key.equals("trial") && map.containsKey(skin()) ? map.get(skin()) : map.get(key);
		return b != null ? b : map.getOrDefault("bandit", new com.grandline.config.EnemyBehavior());
	}

	/** Safe standing ground (not on or beside fire, campfires, magma, cactus, berry bushes...). */
	public static boolean safeGround(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos feet) {
		return net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getBlockPathTypeStatic(level, feet.mutable())
				== net.minecraft.world.level.pathfinder.BlockPathTypes.WALKABLE;
	}

	/** {@code spot}, or the nearest safe standing spot within 4 blocks if it is not safe (else {@code spot} itself). */
	public static net.minecraft.core.BlockPos safeSpot(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos spot) {
		if (safeGround(level, spot)) {
			return spot;
		}
		net.minecraft.core.BlockPos best = null;
		double bestD = Double.MAX_VALUE;
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				for (int dy = -2; dy <= 2; dy++) {
					net.minecraft.core.BlockPos p = spot.offset(dx, dy, dz);
					double d = dx * dx + dz * dz + dy * dy * 2;
					if (d < bestD && safeGround(level, p)) {
						best = p;
						bestD = d;
					}
				}
			}
		}
		return best != null ? best : spot;
	}

	/** Whether the territory AI drives it; specialised enemies with their own rules return false. */
	protected boolean territoryAi() {
		return true;
	}

	public long islandId() {
		return islandId;
	}

	protected void updateName() {
		// The name on its island (an island can give it its own name; see GrandLineNames), then the level.
		// A look with a name of its own (the Quake Monarch raid boss) goes by it; others by their kind's name.
		String own = "entity.grandline." + skin();
		String key = !skin().isEmpty() && net.minecraft.locale.Language.getInstance().has(own) ? own : "entity.grandline." + kind().id();
		setCustomName(com.grandline.names.GrandLineNames.npc(key, com.grandline.names.GrandLineNames.islandNameOf(islandId))
				.append(" [Lv. " + grandLineLevel + "]"));
		setCustomNameVisible(true);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new ReturnHomeGoal());
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
		goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.9));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7, 0.0F) {
			@Override
			public boolean canUse() {
				return !returning && getTarget() == null && super.canUse();
			}
		});
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		// No vanilla target goals: targets come only from the territory AI below (notice, provocation).
	}

	// ============================================================================================== territory AI

	/** Only targets this enemy chose itself; vanilla or other code cannot point it at anyone else. */
	@Override
	public void setTarget(net.minecraft.world.entity.LivingEntity target) {
		if (target != null && territoryAi() && !engaging) {
			return;
		}
		super.setTarget(target);
	}

	private void engage(net.minecraft.server.level.ServerPlayer player) {
		returning = false;
		engaging = true;
		setTarget(player);
		engaging = false;
	}

	/** Gives up the fight: no target, no attack, back to its spot (healed there). */
	public void disengage() {
		super.setTarget(null);
		setLastHurtByMob(null);
		setAggressive(false);
		getNavigation().stop();
		returning = true;
		returningTicks = 0;
	}

	/** A player this enemy may fight: alive, in this world, playing (not creative or spectator). */
	private boolean fightable(net.minecraft.world.entity.LivingEntity e) {
		return e instanceof net.minecraft.server.level.ServerPlayer p && p.isAlive() && !p.isRemoved() && !p.hasDisconnected()
				&& p.level() == level() && !p.isSpectator() && !creative(p);
	}

	private static boolean creative(net.minecraft.server.level.ServerPlayer p) {
		return p.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.CREATIVE;
	}

	private boolean protectedAt(net.minecraft.world.phys.Vec3 pos) {
		return SafeZones.isProtected((net.minecraft.server.level.ServerLevel) level(), pos, behavior().protectionRadius);
	}

	private double fromHome(net.minecraft.world.phys.Vec3 pos) {
		return net.minecraft.world.phys.Vec3.atBottomCenterOf(home()).distanceTo(pos);
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (!territoryAi()) {
			return;
		}
		var b = behavior();
		if (home == null) {
			home = origin != null ? origin : blockPosition(); // older saves and trial waves: where it stands now
		}
		if (!homeChecked) {
			homeChecked = true;
			net.minecraft.core.BlockPos safe = safeSpot(level(), home);
			if (!safe.equals(home)) {
				home = safe; // its spot was on a campfire, magma or the like: it guards the nearest safe ground instead
				if (position().distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(home)) > 2.5 * 2.5 && getTarget() == null) {
					returning = true;
					returningTicks = 0;
				}
			}
		}
		if (!hasRestriction() || getRestrictRadius() != (float) b.idleRadius || !getRestrictCenter().equals(home)) {
			restrictTo(home, (int) Math.max(1, Math.round(b.idleRadius)));
		}
		var target = getTarget();
		if (target != null) {
			if (tickCount % 2 == 0) {
				boolean protectedHome = protectedAt(net.minecraft.world.phys.Vec3.atBottomCenterOf(home));
				if (!fightable(target) || fromHome(position()) > b.pursuitRadius || protectedAt(target.position())
						|| (!protectedHome && protectedAt(position()))) {
					disengage();
				}
			}
			return;
		}
		if (returning || tickCount % 5 != 0 || b.aggressionRadius <= 0) {
			return;
		}
		// Notice the nearest player in range and in sight (never through walls), outside protected areas.
		net.minecraft.server.level.ServerPlayer best = null;
		double bestDistance = Double.MAX_VALUE;
		for (net.minecraft.server.level.ServerPlayer p : ((net.minecraft.server.level.ServerLevel) level()).players()) {
			double d = distanceTo(p);
			if (d > b.aggressionRadius || d >= bestDistance || !fightable(p)) {
				continue;
			}
			if ((b.lineOfSight && !hasLineOfSight(p)) || fromHome(p.position()) > b.pursuitRadius || protectedAt(p.position())) {
				continue;
			}
			best = p;
			bestDistance = d;
		}
		if (best != null) {
			engage(best);
		}
	}

	/**
	 * A player in a protected area cannot hurt it (no attacking from safety). Otherwise a player's hit provokes this
	 * enemy (and its neighbours only with group aggression), unless it is walking home and the attacker stands outside
	 * its territory.
	 */
	@Override
	public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
		if (!territoryAi() || level().isClientSide) {
			return super.hurt(source, amount);
		}
		net.minecraft.server.level.ServerPlayer attacker = source.getEntity() instanceof net.minecraft.server.level.ServerPlayer p ? p : null;
		if (attacker != null && !creative(attacker) && protectedAt(attacker.position())) {
			if (level().getGameTime() - lastWarnTick > 40) {
				lastWarnTick = level().getGameTime();
				attacker.displayClientMessage(Component.translatable("message.grandline.enemy.safe_area").withStyle(net.minecraft.ChatFormatting.YELLOW), true);
			}
			return false;
		}
		boolean hurt = super.hurt(source, amount);
		if (hurt && isAlive() && attacker != null && fightable(attacker) && getTarget() == null) {
			provoke(attacker);
			if (behavior().groupAggression) {
				for (GrandLineEnemy other : level().getEntitiesOfClass(GrandLineEnemy.class, getBoundingBox().inflate(8),
						e -> e != this && e.isAlive() && e.territoryAi() && e.getTarget() == null)) {
					other.provoke(attacker);
				}
			}
		}
		return hurt;
	}

	/** Attacked by {@code attacker}: fight back, if they stand where it may chase them. */
	void provoke(net.minecraft.server.level.ServerPlayer attacker) {
		var b = behavior();
		if (returning && fromHome(attacker.position()) > b.pursuitRadius) {
			return; // on its way home: not drawn back out by someone outside its territory
		}
		if (protectedAt(attacker.position())) {
			return;
		}
		engage(attacker);
	}

	/** Walks back to its spot after a fight, then heals and idles again. */
	private final class ReturnHomeGoal extends net.minecraft.world.entity.ai.goal.Goal {
		ReturnHomeGoal() {
			setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return returning && territoryAi();
		}

		@Override
		public boolean canContinueToUse() {
			return returning;
		}

		@Override
		public void tick() {
			returningTicks++;
			var h = home();
			double d = position().distanceTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(h));
			if (d <= 1.6 || returningTicks > 20 * 20) {
				if (d > 1.6) {
					teleportTo(h.getX() + 0.5, h.getY(), h.getZ() + 0.5); // stuck for 20 s: put it back
				}
				arrive();
				return;
			}
			if (returningTicks % 20 == 1 || getNavigation().isDone()) {
				getNavigation().moveTo(h.getX() + 0.5, h.getY(), h.getZ() + 0.5, 1.0);
			}
		}

		private void arrive() {
			returning = false;
			getNavigation().stop();
			if (behavior().healOnReset) {
				setHealth(getMaxHealth());
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
		if (home != null) {
			tag.putLong("GrandLineHome", home.asLong());
		}
		tag.putBoolean("GrandLineReturning", returning);
		if (!skin().isEmpty()) {
			tag.putString("GrandLineSkin", skin());
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
		if (tag.contains("GrandLineHome")) {
			home = net.minecraft.core.BlockPos.of(tag.getLong("GrandLineHome"));
		}
		returning = tag.getBoolean("GrandLineReturning");
		setSkin(tag.getString("GrandLineSkin"));
		// (Older saves stored a camp-wide "GrandLineLeash"; the territory AI sets its own idle area on the first tick.)
		updateName(); // picks up the mod's current names
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}
}
