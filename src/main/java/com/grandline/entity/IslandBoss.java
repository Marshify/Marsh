package com.grandline.entity;

import com.grandline.fruit.FruitService;
import com.grandline.fruit.Fruits;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** An island boss: much tougher, shows a boss bar to nearby players and has a chance to drop a fruit. */
public class IslandBoss extends GrandLineEnemy {
	public static final float FRUIT_DROP_CHANCE = 0.25F;

	private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.grandline.captain"),
			BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

	public IslandBoss(EntityType<? extends IslandBoss> type, Level level) {
		super(type, level);
	}

	@Override
	protected void updateName() {
		super.updateName();
		bossBar.setName(getDisplayName());
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		bossBar.setProgress(getHealth() / getMaxHealth());
	}

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
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
		super.dropCustomDeathLoot(source, looting, recentlyHit);
		if (getRandom().nextFloat() < FRUIT_DROP_CHANCE) {
			Fruits.FruitDef fruit = Fruits.all().get(getRandom().nextInt(Fruits.all().size()));
			level().addFreshEntity(new ItemEntity(level(), getX(), getY() + 0.5, getZ(), new ItemStack(fruit.item())));
		}
	}
}
