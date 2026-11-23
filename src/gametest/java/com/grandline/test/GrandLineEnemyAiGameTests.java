package com.grandline.test;

import com.grandline.entity.GrandLineEnemy;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.Islander;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The enemy territory AI: notices only nearby players in sight, is provoked only by its own attacker, gives up at its
 * pursuit limit and heals at home, never fights in protected areas, and drops invalid targets.
 */
public final class GrandLineEnemyAiGameTests implements FabricGameTest {
	/** A stone floor (relative y 1) from x 0..len, z -3..3, so enemies can walk. */
	private static void floor(GameTestHelper helper, int len) {
		for (int x = -2; x <= len; x++) {
			for (int z = -3; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static GrandLineEnemy bandit(GameTestHelper helper, int x) {
		GrandLineEnemy e = GrandLineEntities.BANDIT.create(helper.getLevel());
		BlockPos at = helper.absolutePos(new BlockPos(x, 2, 0));
		e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		e.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
		e.setGrandLineLevel(5);
		e.setOrigin(at, Long.MIN_VALUE);
		helper.getLevel().addFreshEntity(e);
		return e;
	}

	private static ServerPlayer player(GameTestHelper helper, double x) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(GameType.SURVIVAL);
		Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 2, 0.5));
		p.moveTo(at.x, at.y, at.z, 90, 0);
		return p;
	}

	private static void done(GameTestHelper helper, Entity... entities) {
		for (Entity e : entities) {
			if (e instanceof ServerPlayer p) {
				helper.getLevel().getServer().getPlayerList().remove(p);
			} else if (e != null) {
				e.discard();
			}
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "enemy_ai_notice", timeoutTicks = 80)
	public void noticesOnlyNearbyPlayers(GameTestHelper helper) {
		floor(helper, 10);
		GrandLineEnemy e = bandit(helper, 1);
		ServerPlayer p = player(helper, 7); // 6 blocks away: free to walk past
		helper.runAfterDelay(25, () -> {
			helper.assertTrue(e.getTarget() == null, "a player 6 blocks away is not noticed");
			Vec3 near = helper.absoluteVec(new Vec3(3.5, 2, 0.5));
			p.teleportTo(near.x, near.y, near.z); // 2 blocks away, in sight
		});
		helper.runAfterDelay(50, () -> {
			helper.assertTrue(e.getTarget() == p, "a player 2 blocks away in sight is attacked");
			done(helper, e, p);
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "enemy_ai_walls", timeoutTicks = 60)
	public void wallsBlockSight(GameTestHelper helper) {
		floor(helper, 8);
		for (int z = -2; z <= 2; z++) {
			for (int y = 2; y <= 4; y++) {
				helper.setBlock(new BlockPos(2, y, z), Blocks.STONE);
			}
		}
		GrandLineEnemy e = bandit(helper, 1);
		ServerPlayer p = player(helper, 3.6); // 2.6 blocks away, behind the wall
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(e.getTarget() == null, "nobody is noticed through a wall");
			done(helper, e, p);
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "enemy_ai_provoke", timeoutTicks = 40)
	public void aHitProvokesOnlyThatEnemy(GameTestHelper helper) {
		floor(helper, 14);
		GrandLineEnemy a = bandit(helper, 1);
		GrandLineEnemy b = bandit(helper, 2);
		ServerPlayer p = player(helper, 12); // well outside the aggression radius
		helper.runAfterDelay(5, () -> {
			a.hurt(helper.getLevel().damageSources().playerAttack(p), 1);
			helper.assertTrue(a.getTarget() == p, "the enemy that was hit fights back");
			helper.assertTrue(b.getTarget() == null, "its neighbour is not provoked (no group aggression)");
			// Vanilla or other code cannot point it at anyone.
			b.setTarget(p);
			helper.assertTrue(b.getTarget() == null, "targets come only from the territory AI");
			done(helper, a, b, p);
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "enemy_ai_leash", timeoutTicks = 400)
	public void givesUpAtItsLimitAndHealsAtHome(GameTestHelper helper) {
		floor(helper, 28);
		GrandLineEnemy e = bandit(helper, 1);
		ServerPlayer p = player(helper, 26);
		helper.runAfterDelay(5, () -> {
			Vec3 far = helper.absoluteVec(new Vec3(24.5, 2, 0.5)); // 23 blocks from its spot: past the 20-block limit
			e.teleportTo(far.x, far.y, far.z);
			e.setHealth(e.getMaxHealth() / 2);
			e.hurt(helper.getLevel().damageSources().playerAttack(p), 1);
			helper.assertTrue(e.getTarget() == p, "provoked");
		});
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(e.getTarget() == null && e.returning(), "past its pursuit limit it gives up and heads home");
			helper.assertTrue(e.getHealth() < e.getMaxHealth(), "not healed yet");
		});
		helper.runAfterDelay(360, () -> {
			helper.assertTrue(!e.returning() && e.getTarget() == null, "back home and idle");
			helper.assertTrue(e.position().distanceTo(Vec3.atBottomCenterOf(e.home())) <= 3.5, "near its spot: " + e.position());
			helper.assertTrue(e.getHealth() == e.getMaxHealth(), "healed to full on returning");
			done(helper, e, p);
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "enemy_ai_safe", timeoutTicks = 60)
	public void noFightingInProtectedAreas(GameTestHelper helper) {
		floor(helper, 10);
		Islander npc = GrandLineEntities.ISLANDER.create(helper.getLevel());
		Vec3 at = helper.absoluteVec(new Vec3(0.5, 2, 0.5));
		npc.moveTo(at.x, at.y, at.z, 0, 0);
		npc.setup(Islander.Role.QUEST_GIVER, Long.MIN_VALUE, 1);
		npc.setNoAi(true);
		helper.getLevel().addFreshEntity(npc);
		GrandLineEnemy e = bandit(helper, 7);
		ServerPlayer p = player(helper, 5); // 5 blocks from the quest giver (inside its 8), 2 from the enemy
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(e.getTarget() == null, "a player near a quest giver is never attacked");
			float before = e.getHealth();
			helper.assertTrue(!e.hurt(helper.getLevel().damageSources().playerAttack(p), 4) && e.getHealth() == before,
					"and cannot attack from there either");
			done(helper, e, npc, p);
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "enemy_ai_invalid", timeoutTicks = 60)
	public void dropsInvalidTargets(GameTestHelper helper) {
		floor(helper, 10);
		GrandLineEnemy e = bandit(helper, 1);
		ServerPlayer p = player(helper, 3);
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(e.getTarget() == p, "fighting");
			p.setGameMode(GameType.CREATIVE); // no longer a fair target (same for death, leaving or changing dimension)
		});
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(e.getTarget() == null && e.returning(), "an invalid target is dropped");
			helper.getLevel().getServer().getPlayerList().remove(p);
		});
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(e.getTarget() == null, "and a removed player is never chased");
			done(helper, e);
		});
	}
}
