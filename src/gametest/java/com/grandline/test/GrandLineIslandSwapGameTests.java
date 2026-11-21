package com.grandline.test;

import com.grandline.world.IslandRegistry;
import com.grandline.world.SeaLayout;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Layout 19: Stonewall Prison went to Ironhold Bastion's old place, Redbrick Port to Stonewall Prison's, Ironhold
 * Bastion to Redbrick Port's. New worlds use the new places; older worlds keep the places they were built with.
 */
public final class GrandLineIslandSwapGameTests implements FabricGameTest {
	private static SeaLayout.Spec find(List<SeaLayout.Spec> specs, String name) {
		return specs.stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow();
	}

	private static boolean at(SeaLayout.Spec spec, int x, int z) {
		return spec.x() == x && spec.z() == z;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "island_swap")
	public void threeIslandsSwapPlacesOnlyInNewWorlds(GameTestHelper helper) {
		List<SeaLayout.Spec> before = SeaLayout.all(18), after = SeaLayout.all(IslandRegistry.CURRENT_LAYOUT);
		helper.assertTrue(IslandRegistry.CURRENT_LAYOUT >= SeaLayout.SWAP_LAYOUT, "new worlds get the corrected places");
		// The recorded original places.
		helper.assertTrue(at(find(before, "Redbrick Port"), -630, 120), "Redbrick Port was at (-630, 120)");
		helper.assertTrue(at(find(before, "Ironhold Bastion"), 1380, 0), "Ironhold Bastion was at (1380, 0)");
		helper.assertTrue(at(find(before, "Stonewall Prison"), -1170, 270), "Stonewall Prison was at (-1170, 270)");
		// The three-way move.
		helper.assertTrue(at(find(after, "Stonewall Prison"), 1380, 0), "Stonewall Prison takes Ironhold Bastion's old place");
		helper.assertTrue(at(find(after, "Redbrick Port"), -1170, 270), "Redbrick Port takes Stonewall Prison's old place");
		helper.assertTrue(at(find(after, "Ironhold Bastion"), -630, 120), "Ironhold Bastion takes Redbrick Port's old place");
		// Same islands otherwise (build, size, level, theme), and nothing else moved.
		helper.assertTrue(before.size() == after.size(), "same islands");
		for (int i = 0; i < before.size(); i++) {
			SeaLayout.Spec a = before.get(i), b = after.get(i);
			helper.assertTrue(a.name().equals(b.name()) && a.radius() == b.radius() && a.level() == b.level() && a.theme() == b.theme()
					&& a.sea() == b.sea() && a.starter() == b.starter(), a.name() + " keeps its size, level and theme");
			if (!Set.of("Redbrick Port", "Ironhold Bastion", "Stonewall Prison").contains(a.name())) {
				helper.assertTrue(a.x() == b.x() && a.z() == b.z(), a.name() + " does not move");
			}
		}
		// Each layout: one island per place (unique ids), and no island's build reaches into another's.
		for (List<SeaLayout.Spec> specs : List.of(before, after)) {
			Set<Long> ids = new HashSet<>();
			for (SeaLayout.Spec s : specs) {
				helper.assertTrue(ids.add(SeaLayout.idOf(s)), "unique island id: " + s.name());
			}
			for (SeaLayout.Spec a : specs) {
				for (SeaLayout.Spec b : specs) {
					if (a == b || a.sea() != b.sea()) {
						continue;
					}
					var pa = IslandPlans.get(a.name());
					var pb = IslandPlans.get(b.name());
					int ra = pa != null ? pa.extent() : (int) (a.radius() * 1.45);
					int rb = pb != null ? pb.extent() : (int) (b.radius() * 1.45);
					double d = Math.hypot(a.x() - b.x(), a.z() - b.z());
					helper.assertTrue(d > ra + rb, a.name() + " and " + b.name() + " overlap: " + d + " <= " + (ra + rb));
				}
			}
		}
		helper.assertTrue(SeaLayout.LEGACY_POSITIONS.keySet().equals(Set.of("Redbrick Port", "Ironhold Bastion", "Stonewall Prison")),
				"only these three have old places");
		helper.assertTrue(Map.copyOf(SeaLayout.LEGACY_POSITIONS).size() == 3, "three");
		helper.succeed();
	}
}
