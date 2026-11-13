package com.grandline.test;

import com.grandline.world.plan.Anchor;
import com.grandline.world.plan.DawnreachHarbor;
import com.grandline.world.plan.IslandPlan;
import com.grandline.world.plan.IslandPlans;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Dawnreach Harbor redesign: arrival on open street, one Fruit Gacha at the cart, the NPCs and docks it needs. */
public final class GrandLineDawnreachGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "dawnreach_plan")
	public void dawnreachArrivalAndGachaAreWhereTheyBelong(GameTestHelper helper) {
		IslandPlan plan = IslandPlans.all().stream().filter(p -> p.name().equals("Dawnreach Harbor")).findFirst().orElseThrow();
		helper.assertTrue(plan instanceof DawnreachHarbor, "new worlds get the redesigned Dawnreach");
		helper.assertTrue(IslandPlans.legacy("Dawnreach Harbor") != null, "old worlds keep their Dawnreach");
		int arrivals = 0, gachas = 0, docks = 0;
		for (Anchor a : plan.anchors()) {
			int px = a.x() - DawnreachHarbor.OX, pz = a.z();
			switch (a.type()) {
				case ARRIVAL -> {
					arrivals++;
					helper.assertTrue(DawnreachHarbor.openStreet(px, pz) && a.y() == DawnreachHarbor.WALK,
							"arrival is on open street, not in a building or water: " + px + "," + pz);
				}
				case GACHA -> {
					gachas++;
					helper.assertTrue(px == 48 && pz == -9, "gacha keeper at the cart (48, -9): " + px + "," + pz);
					helper.assertTrue(DawnreachHarbor.openStreet(px - 2, pz) && DawnreachHarbor.openStreet(px - 3, pz),
							"lane in front of the cart is open to walk up");
				}
				case DOCK -> docks++;
				default -> {
				}
			}
		}
		helper.assertTrue(arrivals == 1 && gachas == 1 && docks >= 2, "one arrival, one gacha, docks: " + arrivals + "/" + gachas + "/" + docks);
		helper.succeed();
	}
}
