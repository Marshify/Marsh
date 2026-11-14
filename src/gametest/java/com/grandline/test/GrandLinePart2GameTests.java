package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.entity.EnemyKind;
import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.Islander;
import com.grandline.entity.SaberBoss;
import com.grandline.gear.Gear;
import com.grandline.network.payload.NpcActionPayload;
import com.grandline.npc.EnemyRespawns;
import com.grandline.npc.KeeperNpcs;
import com.grandline.npc.NpcService;
import com.grandline.npc.SwordShop;
import com.grandline.npc.TempleBosses;
import com.grandline.player.PlayerProfileService;
import com.grandline.world.Island;
import com.grandline.world.IslandRegistry;
import com.grandline.world.IslandSpawns;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Part 2: home NPCs, the Swordsmith, the Saber, regular enemy respawns and the Saber Warden. */
public final class GrandLinePart2GameTests implements FabricGameTest {
	private static ServerPlayer survivor(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(GameType.SURVIVAL);
		try {
			var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
			field.setAccessible(true);
			field.setInt(p, 0);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		Vec3 home = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
		p.moveTo(home.x, home.y, home.z, 0, 0);
		return p;
	}

	private static void remove(GameTestHelper helper, Entity... entities) {
		for (Entity e : entities) {
			if (e instanceof ServerPlayer p) {
				helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
			} else if (e != null) {
				e.discard();
			}
		}
	}

	private static void setLayout(IslandRegistry registry, int layout) {
		try {
			var field = IslandRegistry.class.getDeclaredField("layout");
			field.setAccessible(true);
			field.setInt(registry, layout);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void floor(GameTestHelper helper, int size) {
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "p2_keepers", timeoutTicks = 60)
	public void innkeeperIsPlacedOnceAndComesBack(GameTestHelper helper) {
		floor(helper, 10);
		ServerLevel level = helper.getLevel();
		BlockPos centre = helper.absolutePos(new BlockPos(5, 2, 5));
		Island island = new Island(990010, centre.getX(), centre.getZ(), 6, 12, "Test Keeper Isle", false, true, "village");
		IslandRegistry.get(level.getServer()).add(island);
		KeeperNpcs.forget(level, island.id());
		KeeperNpcs.ensure(level, island);
		KeeperNpcs.ensure(level, island); // a second check never adds another
		AABB area = new AABB(centre).inflate(16);
		List<Islander> keepers = level.getEntitiesOfClass(Islander.class, area, n -> n.role() == Islander.Role.INNKEEPER && n.islandId() == island.id());
		helper.assertTrue(keepers.size() == 1, "one innkeeper on the island: " + keepers.size());
		helper.assertTrue(keepers.get(0).isInvulnerable() && !keepers.get(0).removeWhenFarAway(1000), "innkeeper is permanent");
		BlockPos spot = KeeperNpcs.spot(level, island, Islander.Role.INNKEEPER);
		helper.assertTrue(spot != null && keepers.get(0).blockPosition().closerThan(spot, 2), "stands at its saved spot");
		keepers.get(0).discard();
		KeeperNpcs.ensure(level, island);
		helper.assertTrue(level.getEntitiesOfClass(Islander.class, area, n -> n.isAlive() && n.role() == Islander.Role.INNKEEPER
				&& n.islandId() == island.id()).size() == 1, "a lost innkeeper is replaced");
		helper.assertTrue(KeeperNpcs.rolesFor(island).equals(List.of(Islander.Role.INNKEEPER)), "only Millbrook has a swordsmith");
		Island millbrook = new Island(990011, 0, 0, 6, 30, KeeperNpcs.SWORD_ISLAND, false, true, "village");
		helper.assertTrue(KeeperNpcs.rolesFor(millbrook).contains(Islander.Role.SWORDSMITH), "Millbrook has a swordsmith");
		level.getEntitiesOfClass(Islander.class, area, n -> n.islandId() == island.id()).forEach(Entity::discard);
		IslandRegistry.get(level.getServer()).removeIf(i -> i.id() == island.id());
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "p2_home", timeoutTicks = 40)
	public void innkeeperSetsHomeUsedForSeaDeaths(GameTestHelper helper) {
		floor(helper, 6);
		ServerLevel level = helper.getLevel();
		IslandRegistry registry = IslandRegistry.get(level.getServer());
		Island a = new Island(990012, 60_000, 60_000, 20, 15, "Test Home A", false, true, "town");
		Island b = new Island(990013, 61_000, 60_000, 20, 15, "Test Home B", false, true, "town");
		registry.add(a);
		registry.add(b);
		ServerPlayer p = survivor(helper);
		ServerPlayer q = survivor(helper);
		Islander innA = GrandLineEntities.ISLANDER.create(level);
		innA.setup(Islander.Role.INNKEEPER, a.id(), a.level());
		Islander innB = GrandLineEntities.ISLANDER.create(level);
		innB.setup(Islander.Role.INNKEEPER, b.id(), b.level());
		helper.assertTrue(NpcService.setHome(p, innA).equals("home_set"), "home set");
		helper.assertTrue(p.getAttached(ModAttachments.HOME_ISLAND) == a.id(), "home stored");
		helper.assertTrue(q.getAttached(ModAttachments.HOME_ISLAND) == null, "each player's home is their own");
		helper.assertTrue(IslandSpawns.respawnIsland(p, IslandSpawns.NO_ISLAND).id() == a.id(), "dying at sea: respawn at home");
		helper.assertTrue(IslandSpawns.respawnIsland(p, b.id()).id() == b.id(), "dying on an island still respawns there");
		NpcService.setHome(p, innB);
		helper.assertTrue(p.getAttached(ModAttachments.HOME_ISLAND) == b.id(), "home can be changed");
		NpcService.setHome(q, innA);
		helper.assertTrue(q.getAttached(ModAttachments.HOME_ISLAND) == a.id() && p.getAttached(ModAttachments.HOME_ISLAND) == b.id(),
				"two players, two homes");
		remove(helper, p, q, innA, innB);
		registry.removeIf(i -> i.id() == a.id() || i.id() == b.id());
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "p2_swords", timeoutTicks = 40)
	public void swordsmithSellsKatanaAndCutlass(GameTestHelper helper) {
		ServerPlayer p = survivor(helper);
		long katana = SwordShop.price(0), cutlass = SwordShop.price(1);
		helper.assertTrue(SwordShop.STOCK.equals(List.of(Gear.KATANA, Gear.CUTLASS)), "stock is Katana and Cutlass");
		PlayerProfileService.update(p, pr -> pr.withCurrency(cutlass - 1));
		helper.assertTrue(SwordShop.buy(p, 1).equals("sword_money"), "too poor for the Cutlass");
		helper.assertTrue(PlayerProfileService.get(p).currency() == cutlass - 1 && !p.getInventory().contains(new ItemStack(Gear.CUTLASS)),
				"nothing taken, nothing given");
		PlayerProfileService.update(p, pr -> pr.withCurrency(katana + cutlass + 5));
		helper.assertTrue(SwordShop.buy(p, 0).equals("sword_bought") && SwordShop.buy(p, 1).equals("sword_bought"), "both bought");
		helper.assertTrue(PlayerProfileService.get(p).currency() == 5, "exact price taken: " + PlayerProfileService.get(p).currency());
		helper.assertTrue(p.getInventory().countItem(Gear.KATANA) == 1 && p.getInventory().countItem(Gear.CUTLASS) == 1, "one of each delivered");
		helper.assertTrue(SwordShop.buy(p, 2).equals("too_far") && SwordShop.buy(p, -1).equals("too_far"), "no such sword");
		helper.assertTrue(SwordShop.buy(p, 0).equals("sword_money") && p.getInventory().countItem(Gear.KATANA) == 1, "no free swords");
		remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "p2_saber", timeoutTicks = 40)
	public void saberOutclassesKatanaAndCutlass(GameTestHelper helper) {
		ServerPlayer p = survivor(helper);
		double[] dmg = new double[3];
		double[] speed = new double[3];
		var items = List.of(Gear.SABER, Gear.KATANA, Gear.CUTLASS);
		for (int i = 0; i < 3; i++) {
			// What a player holding it gets: base 1 damage and 4 attacks/s plus the item's main-hand modifiers.
			var mods = items.get(i).getDefaultAttributeModifiers(EquipmentSlot.MAINHAND);
			dmg[i] = 1 + mods.get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(m -> m.getAmount()).sum();
			speed[i] = 4 + mods.get(Attributes.ATTACK_SPEED).stream().mapToDouble(m -> m.getAmount()).sum();
		}
		helper.assertTrue(Math.abs(speed[0] - GrandLineMod.config().saberAttackSpeed) < 1e-6, "saber speed from config: " + speed[0]);
		helper.assertTrue(dmg[0] > dmg[1] + 2 && dmg[0] > dmg[2] + 2, "saber hits harder: " + dmg[0] + " vs " + dmg[1] + "/" + dmg[2]);
		helper.assertTrue(dmg[0] * speed[0] > dmg[1] * speed[1] * 1.4, "saber out-damages the katana per second");
		helper.assertTrue(Gear.powerFor(Gear.SABER) != null && Gear.powerFor(Gear.SABER).abilities().size() == 3, "saber keeps its three moves");
		remove(helper, p);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "p2_respawn", timeoutTicks = 280)
	public void regularEnemyRespawnsAfterTenSeconds(GameTestHelper helper) {
		floor(helper, 6);
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(3, 2, 3));
		GrandLineEnemy bandit = GrandLineEntities.BANDIT.create(level);
		bandit.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0, 0);
		bandit.setGrandLineLevel(17);
		bandit.setOrigin(origin, 990014);
		level.addFreshEntity(bandit);
		AABB area = new AABB(origin).inflate(6);
		helper.runAfterDelay(2, () -> {
			bandit.teleportTo(origin.getX() + 2.5, origin.getY(), origin.getZ() + 0.5); // dies away from its spot
			bandit.kill();
		});
		long delay = Math.round(GrandLineMod.config().enemyRespawnSeconds * 20);
		helper.runAfterDelay(2 + delay - 2, () -> helper.assertTrue(level.getEntitiesOfClass(GrandLineEnemy.class, area, Entity::isAlive).isEmpty(),
				"not back before the delay"));
		helper.runAfterDelay(2 + delay + 2, () -> {
			List<GrandLineEnemy> back = level.getEntitiesOfClass(GrandLineEnemy.class, area, Entity::isAlive);
			helper.assertTrue(back.size() == 1, "exactly one came back: " + back.size());
			GrandLineEnemy e = back.get(0);
			helper.assertTrue(e.kind() == EnemyKind.BANDIT && e.grandLineLevel() == 17, "same kind and level");
			helper.assertTrue(e.blockPosition().equals(origin), "at its spawn point: " + e.blockPosition());
			helper.assertTrue(e.origin().equals(origin) && e.islandId() == 990014, "keeps its spawn point");
			helper.assertTrue(e.getMainHandItem().getItem() == EnemyKind.BANDIT.weapon(), "equipped");
			e.discard();
		});
		helper.runAfterDelay(2 + delay + 30, () -> {
			helper.assertTrue(level.getEntitiesOfClass(GrandLineEnemy.class, area, Entity::isAlive).isEmpty(), "no second copy");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "p2_boss_fight", timeoutTicks = 200)
	public void saberWardenCapsHitsDropsOneSaberAndResets(GameTestHelper helper) {
		floor(helper, 10);
		ServerLevel level = helper.getLevel();
		BlockPos home = helper.absolutePos(new BlockPos(5, 2, 5));
		SaberBoss boss = GrandLineEntities.SABER_BOSS.create(level);
		boss.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0, 0);
		boss.equip();
		boss.setup(home, 0, 990015, 20);
		level.addFreshEntity(boss);
		helper.assertTrue(boss.getMainHandItem().getItem() == Gear.SABER, "wields the Saber");
		helper.assertTrue(boss.getMaxHealth() == GrandLineMod.config().saberBossHealth, "boss health from config");
		helper.assertTrue(boss.getMaxHealth() > 5 * 38, "far tougher than a Verdant bandit");
		ServerPlayer p = survivor(helper);
		ServerPlayer q = survivor(helper);
		float max = p.getMaxHealth();
		boss.hitWithSword(p, 1000);
		helper.assertTrue(p.getHealth() >= max - max * GrandLineMod.config().saberBossMaxHitPercent / 100 - 0.01, "no one-shots: " + p.getHealth());
		boss.hurt(level.damageSources().playerAttack(q), 30);
		boss.invulnerableTime = 0;
		boss.hurt(level.damageSources().playerAttack(p), 10);
		helper.assertTrue(boss.damageBy().get(q.getUUID()) > boss.damageBy().get(p.getUUID()), "damage is tracked per player");
		boss.invulnerableTime = 0;
		boss.hurt(level.damageSources().playerAttack(q), 100_000);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(!boss.isAlive(), "defeated");
			helper.assertTrue(q.getInventory().countItem(Gear.SABER) == 1, "top damage dealer gets the Saber");
			helper.assertTrue(p.getInventory().countItem(Gear.SABER) == 0, "only one Saber");
			boss.die(level.damageSources().playerAttack(q));
			helper.assertTrue(q.getInventory().countItem(Gear.SABER) == 1, "a second death call gives nothing");
			List<net.minecraft.world.entity.item.ItemEntity> drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new AABB(home).inflate(8), it -> it.getItem().is(Gear.SABER));
			helper.assertTrue(drops.isEmpty(), "the held Saber does not also drop");
		});
		// A second boss: hurt, then everyone leaves the temple: it heals and forgets.
		SaberBoss other = GrandLineEntities.SABER_BOSS.create(level);
		helper.runAfterDelay(4, () -> {
			other.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0, 0);
			other.equip();
			other.setup(home, 0, 990016, 20);
			level.addFreshEntity(other);
			other.hurt(level.damageSources().playerAttack(p), 50);
			Vec3 away = Vec3.atCenterOf(home).add(80, 0, 0);
			p.teleportTo(away.x, away.y, away.z);
			q.teleportTo(away.x, away.y, away.z);
		});
		// A third boss: the top fighter has fallen, so the next living fighter gets the Saber.
		SaberBoss third = GrandLineEntities.SABER_BOSS.create(level);
		ServerPlayer r = survivor(helper);
		ServerPlayer s = survivor(helper);
		helper.runAfterDelay(6, () -> {
			third.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0, 0);
			third.setup(home, 0, 990018, 20);
			level.addFreshEntity(third);
			third.hurt(level.damageSources().playerAttack(r), 60);
			third.invulnerableTime = 0;
			third.hurt(level.damageSources().playerAttack(s), 20);
			r.kill();
			third.invulnerableTime = 0;
			third.hurt(level.damageSources().playerAttack(s), 100_000);
		});
		helper.runAfterDelay(8, () -> {
			helper.assertTrue(s.getInventory().countItem(Gear.SABER) == 1, "the fallen top fighter's Saber goes to the next living fighter");
			remove(helper, r, s);
		});
		helper.runAfterDelay(4 + 130, () -> {
			helper.assertTrue(other.getHealth() == other.getMaxHealth(), "reset to full with nobody in the arena: " + other.getHealth());
			helper.assertTrue(other.damageBy().isEmpty(), "fight forgotten");
			remove(helper, p, q, other);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "p2_boss_respawn", timeoutTicks = 60)
	public void templeBossRespawnsAfterCooldownWithoutDuplicates(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		IslandRegistry registry = IslandRegistry.get(level.getServer());
		int layout = registry.layout();
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT); // the redesigned Verdant (its temple has the boss)
		BlockPos anchorHome = helper.absolutePos(new BlockPos(2, 2, 2));
		// A test "Verdant Wilds" whose temple spot lands in this test's area.
		var spot = com.grandline.world.plan.IslandPlans.anchors(new Island(0, 0, 0, 1, 1, "Verdant Wilds", false, true, ""),
				com.grandline.world.plan.Anchor.Type.TEMPLE_BOSS).get(0);
		Island verdant = new Island(990017, anchorHome.getX() - spot.x(), anchorHome.getZ() - spot.z(), 40, 15, "Verdant Wilds", false, true, "jungle");
		registry.add(verdant);
		TempleBosses.forget(level, verdant.id()); // the test world keeps saved data between runs
		BlockPos home = TempleBosses.home(verdant);
		for (BlockPos p : BlockPos.betweenClosed(home.offset(-2, -1, -2), home.offset(2, -1, 2))) {
			level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
		}
		double cooldown = GrandLineMod.config().saberBossRespawnSeconds;
		SaberBoss first = TempleBosses.ensure(level, verdant);
		helper.assertTrue(first != null && first.blockPosition().equals(home), "boss at the temple spot: " + (first == null ? null : first.blockPosition()));
		helper.assertTrue(TempleBosses.ensure(level, verdant) == first, "no duplicate");
		SaberBoss stray = GrandLineEntities.SABER_BOSS.create(level);
		stray.moveTo(home.getX() + 1.5, home.getY(), home.getZ() + 0.5, 0, 0);
		stray.setup(home, 0, verdant.id(), 20);
		level.addFreshEntity(stray);
		TempleBosses.ensure(level, verdant);
		helper.assertTrue(stray.isRemoved() && first.isAlive(), "a stray copy is removed");
		GrandLineMod.config().saberBossRespawnSeconds = 1; // a one-second cooldown for the test (default: see config)
		first.kill();
		helper.runAfterDelay(2, () -> {
			long next = TempleBosses.nextSpawn(level, verdant.id());
			helper.assertTrue(next - level.getGameTime() > 10 && next - level.getGameTime() <= 20, "respawn timer started: " + (next - level.getGameTime()));
			helper.assertTrue(TempleBosses.ensure(level, verdant) == null, "not back during the cooldown");
		});
		helper.runAfterDelay(26, () -> {
			SaberBoss again = TempleBosses.ensure(level, verdant);
			helper.assertTrue(again != null && again != first && again.blockPosition().equals(home), "back after the cooldown, at the temple spot");
			helper.assertTrue(level.getEntitiesOfClass(SaberBoss.class, new AABB(home).inflate(40), Entity::isAlive).size() == 1, "exactly one boss");
		});
		helper.runAfterDelay(28, () -> {
			GrandLineMod.config().saberBossRespawnSeconds = cooldown;
			level.getEntitiesOfClass(SaberBoss.class, new AABB(home).inflate(40)).forEach(Entity::discard);
			registry.removeIf(i -> i.id() == verdant.id());
			setLayout(registry, layout);
			for (BlockPos p : BlockPos.betweenClosed(home.offset(-2, -1, -2), home.offset(2, -1, 2))) {
				level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "millbrook_plan", timeoutTicks = 20)
	public void millbrookRebuildKeepsEveryRole(GameTestHelper helper) {
		IslandRegistry registry = IslandRegistry.get(helper.getLevel().getServer());
		int layout = registry.layout();
		setLayout(registry, IslandRegistry.CURRENT_LAYOUT);
		var plan = com.grandline.world.plan.IslandPlans.get("Millbrook Village");
		var anchors = plan.anchors();
		java.util.function.Function<com.grandline.world.plan.Anchor.Type, List<com.grandline.world.plan.Anchor>> of =
				t -> anchors.stream().filter(a -> a.type() == t).toList();
		var quest = of.apply(com.grandline.world.plan.Anchor.Type.QUEST);
		helper.assertTrue(quest.size() == 1 && quest.get(0).x() == 29 && quest.get(0).z() == 12, "quest giver at local (29, 12)");
		helper.assertTrue(of.apply(com.grandline.world.plan.Anchor.Type.HOME).size() == 1, "innkeeper spot");
		helper.assertTrue(of.apply(com.grandline.world.plan.Anchor.Type.SWORDSMITH).size() == 1, "swordsmith spot");
		helper.assertTrue(of.apply(com.grandline.world.plan.Anchor.Type.GACHA).isEmpty(), "the gacha stays at Dawnreach");
		var arrival = of.apply(com.grandline.world.plan.Anchor.Type.ARRIVAL).get(0);
		helper.assertTrue(arrival.z() > 40, "arrival at the southern dock");
		var camps = of.apply(com.grandline.world.plan.Anchor.Type.ENEMY_CAMP);
		helper.assertTrue(camps.size() == 2, "two combat fields");
		for (var camp : camps) {
			helper.assertTrue(camp.x() > 35, "combat fields on the east side");
			helper.assertTrue(Math.hypot(camp.x() - arrival.x(), camp.z() - arrival.z()) > 45, "fields well away from the arrival");
			helper.assertTrue(plan.terrain().paving(camp.x(), camp.z()) == null, "no road through the middle of a field");
		}
		// About 138 x 122: measure the land through the centre lines.
		int west = 0, east = 0, north = 0, south = 0;
		for (int x = 0; x > -90 && plan.terrain().ground(x, 0) > com.grandline.world.plan.Terrain.WATER_TOP; x--) west = x;
		for (int x = 0; x < 90 && plan.terrain().ground(x, 0) > com.grandline.world.plan.Terrain.WATER_TOP; x++) east = x;
		for (int z = 0; z > -90 && plan.terrain().ground(0, z) > com.grandline.world.plan.Terrain.WATER_TOP; z--) north = z;
		for (int z = 0; z < 90 && plan.terrain().ground(0, z) > com.grandline.world.plan.Terrain.WATER_TOP; z++) south = z;
		int width = east - west + 1, depth = south - north + 1;
		helper.assertTrue(width >= 125 && width <= 145 && depth >= 110 && depth <= 130, "island size " + width + " x " + depth);
		setLayout(registry, 3);
		helper.assertTrue(com.grandline.world.plan.IslandPlans.get("Millbrook Village") != plan, "older worlds keep the original Millbrook");
		setLayout(registry, layout);
		helper.succeed();
	}
}
