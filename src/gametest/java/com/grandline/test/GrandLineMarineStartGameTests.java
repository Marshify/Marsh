package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.config.GrandLineConfig;
import com.grandline.entity.Islander;
import com.grandline.npc.IslandPopulation;
import com.grandline.npc.KeeperNpcs;
import com.grandline.world.FirstSeaProgression;
import com.grandline.world.Island;
import com.grandline.world.SeaLayout;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Ironhold Bastion is the Marines' starting island (Lv 1-10); Redbrick Port took its Lv 120-150 stage. */
public final class GrandLineMarineStartGameTests implements FabricGameTest {
	private static Island island(String name) {
		SeaLayout.Spec spec = SeaLayout.all().stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow();
		return new Island(SeaLayout.idOf(spec), spec.x(), spec.z(), spec.radius(), spec.level(), spec.name(), false, true, spec.theme().name());
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "marine_start")
	public void ironholdStartsMarinesAndRedbrickTookItsLevels(GameTestHelper helper) {
		helper.assertTrue("Ironhold Bastion".equals(GrandLineConfig.defaults().marineSpawnIsland), "Marines start on Ironhold Bastion by default");
		helper.assertTrue("Millbrook Village".equals(GrandLineConfig.defaults().spawnIsland), "Pirates still start on Millbrook Village");
		// A config written before this update with the old default moves to Ironhold; a custom choice is kept.
		GrandLineConfig old = GrandLineConfig.defaults();
		old.marineSpawnIsland = "Redbrick Port";
		invokeMigrate(old, 4);
		helper.assertTrue("Ironhold Bastion".equals(old.marineSpawnIsland), "an old config's Redbrick Port start becomes Ironhold");
		GrandLineConfig custom = GrandLineConfig.defaults();
		custom.marineSpawnIsland = "Verdant Wilds";
		invokeMigrate(custom, 4);
		helper.assertTrue("Verdant Wilds".equals(custom.marineSpawnIsland), "a server's own choice is kept");

		Island iron = island("Ironhold Bastion"), red = island("Redbrick Port");
		for (int layout : new int[]{SeaLayout.SWAP_LAYOUT - 1, com.grandline.world.IslandRegistry.CURRENT_LAYOUT}) {
			for (SeaLayout.Spec spec : SeaLayout.all(layout)) {
				if (spec.name().equals("Ironhold Bastion") || spec.name().equals("Redbrick Port")) {
					helper.assertTrue(spec.level() == (spec.name().equals("Ironhold Bastion") ? 1 : 120), "layout " + layout + ": " + spec);
				}
			}
		}
		helper.assertTrue(iron.level() == 1 && red.level() == 120, "island levels swapped: " + iron.level() + " / " + red.level());
		helper.assertTrue(java.util.Arrays.equals(FirstSeaProgression.islandSpan("Ironhold Bastion"), new int[]{1, 10})
				&& java.util.Arrays.equals(FirstSeaProgression.islandSpan("Redbrick Port"), new int[]{120, 150}), "enemy level spans swapped");
		// Enemies saved with the old levels are out of line and get re-levelled when they load.
		helper.assertTrue(IslandPopulation.outOfLine(135, iron) && !IslandPopulation.outOfLine(5, iron), "old Lv 120+ Ironhold enemies re-level");
		helper.assertTrue(IslandPopulation.outOfLine(5, red) && !IslandPopulation.outOfLine(135, red), "old Lv 1-10 Redbrick enemies re-level");
		// The starting-island keepers follow the Marines' island.
		java.util.List<Islander.Role> ironRoles = KeeperNpcs.rolesFor(iron), redRoles = KeeperNpcs.rolesFor(red);
		helper.assertTrue(ironRoles.contains(Islander.Role.SWORDSMITH) && ironRoles.contains(Islander.Role.MOVEMENT_INSTRUCTOR),
				"Ironhold has the starting Swordsmith and Movement Instructor");
		helper.assertTrue(!redRoles.contains(Islander.Role.SWORDSMITH) && !redRoles.contains(Islander.Role.MOVEMENT_INSTRUCTOR),
				"Redbrick Port no longer does");
		helper.assertTrue(GrandLineMod.config().marineSpawnIsland.equals("Ironhold Bastion"), "the running config uses Ironhold");
		helper.succeed();
	}

	private static void invokeMigrate(GrandLineConfig config, int from) {
		try {
			var m = GrandLineConfig.class.getDeclaredMethod("migrateFrom", int.class);
			m.setAccessible(true);
			m.invoke(config, from);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
