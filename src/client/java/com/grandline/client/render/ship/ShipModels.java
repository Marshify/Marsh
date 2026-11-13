package com.grandline.client.render.ship;

import com.grandline.GrandLineMod;
import com.grandline.ship.ShipClass;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Ship geometry, built from a few numbers per class. Model units are 1/8 block (the renderer scales by 2); the bow
 * points to -Z, up is -Y, and y = 0 is the bottom of the ship's hitbox (the deck is at -deck × 8).
 *
 * <p>The hull texture is split into bands of one material each (see tools/make_ship_textures.py), so every box takes
 * its UVs from the start of its band. Sails are a separate model: one 64 x 48 x 1 sail box per mast, scaled to size by
 * the renderer, so the emblem always lands on the sail face.
 */
public final class ShipModels {
	static final int HULL = 0;
	static final int DECK = 256;
	static final int TRIM = 512;
	static final int MAST = 768;

	/** Per-class layout in blocks. Masts: {forward position (+ = towards the bow), height above deck, sail width, sail height}. */
	record Layout(float length, float width, float deck, float[][] masts, float sternCastle, float sternHeight,
			float bowCastle, float bowHeight, int cannonsPerSide, float bowsprit) {
		float body() {
			return length - 2;
		}
	}

	static Layout layout(ShipClass ship) {
		return switch (ship) {
			case SLOOP -> new Layout(7, 3, 1.0F, new float[][]{{0.4F, 6.5F, 4.2F, 3.6F}}, 0, 0, 0, 0, 0, 2.5F);
			case BRIG -> new Layout(10, 4, 1.25F, new float[][]{{1.8F, 8F, 5.5F, 4.5F}, {-1.2F, 7.5F, 5F, 3.5F}}, 2.5F, 1.0F, 0, 0, 3, 3F);
			case GALLEON -> new Layout(14, 5, 1.5F, new float[][]{{3.8F, 9F, 6.5F, 5F}, {0.8F, 10.5F, 7.5F, 6F}, {-2.0F, 8F, 6F, 3.5F}},
					3.5F, 1.5F, 2.0F, 0.8F, 4, 3.5F);
		};
	}

	public static ModelLayerLocation hullLayer(ShipClass ship) {
		return new ModelLayerLocation(GrandLineMod.id("ship_" + ship.id), "hull");
	}

	public static ModelLayerLocation sailLayer(ShipClass ship) {
		return new ModelLayerLocation(GrandLineMod.id("ship_" + ship.id), "sails");
	}

	private ShipModels() {
	}

	private static void box(PartDefinition root, String name, int band, float x0, float y0, float z0, float x1, float y1, float z1) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, band)
				.addBox(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.abs(x1 - x0), Math.abs(y1 - y0), Math.abs(z1 - z0)), PartPose.ZERO);
	}

	public static LayerDefinition hull(ShipClass ship) {
		Layout l = layout(ship);
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		float hw = l.width() * 4;           // half width
		float hb = l.body() * 4;            // half body length
		float deck = -l.deck() * 8;         // deck surface
		// Hull: body, then two narrowing, shallower bow sections.
		box(root, "body", HULL, -hw, deck + 1, -hb, hw, 8, hb);
		box(root, "bow1", HULL, -hw * 0.66F, deck + 1, -hb - 8, hw * 0.66F, 6, -hb);
		box(root, "bow2", HULL, -hw * 0.33F, deck - 1, -hb - 16, hw * 0.33F, 3, -hb - 8);
		box(root, "keel", MAST, -1, 8, -hb - 6, 1, 10, hb);
		box(root, "deck", DECK, -hw + 1, deck, -hb, hw - 1, deck + 1, hb);
		// Rails along both sides and across the stern.
		box(root, "rail_l", TRIM, -hw, deck - 4, -hb, -hw + 1, deck + 1, hb);
		box(root, "rail_r", TRIM, hw - 1, deck - 4, -hb, hw, deck + 1, hb);
		box(root, "rail_s", TRIM, -hw, deck - 4, hb - 1, hw, deck, hb);
		box(root, "rail_b1", TRIM, -hw * 0.66F, deck - 4, -hb - 8, -hw * 0.66F + 1, deck + 1, -hb);
		box(root, "rail_b2", TRIM, hw * 0.66F - 1, deck - 4, -hb - 8, hw * 0.66F, deck + 1, -hb);
		// Stern castle with a rail, and an optional forecastle.
		if (l.sternCastle() > 0) {
			float top = deck - l.sternHeight() * 8;
			float front = hb - l.sternCastle() * 8;
			box(root, "stern", HULL, -hw, top, front, hw, deck, hb);
			box(root, "stern_deck", DECK, -hw + 1, top - 0.5F, front, hw - 1, top, hb);
			box(root, "stern_rail_l", TRIM, -hw, top - 4, front, -hw + 1, top, hb);
			box(root, "stern_rail_r", TRIM, hw - 1, top - 4, front, hw, top, hb);
			box(root, "stern_rail_f", TRIM, -hw, top - 4, front, hw, top, front + 1);
			box(root, "stern_window", TRIM, -hw * 0.6F, top + 2, hb, hw * 0.6F, deck - 2, hb + 0.5F);
		}
		if (l.bowCastle() > 0) {
			float top = deck - l.bowHeight() * 8;
			box(root, "fore", HULL, -hw, top, -hb, hw, deck, -hb + l.bowCastle() * 8);
			box(root, "fore_rail", TRIM, -hw, top - 3, -hb + l.bowCastle() * 8 - 1, hw, top, -hb + l.bowCastle() * 8);
		}
		// Masts, yards and a bowsprit.
		for (int i = 0; i < l.masts().length; i++) {
			float[] m = l.masts()[i];
			float z = -m[0] * 8;
			float top = deck - m[1] * 8;
			box(root, "mast" + i, MAST, -1.5F, top, z - 1.5F, 1.5F, deck, z + 1.5F);
			float sailTop = top + 4;
			box(root, "yard" + i, MAST, -m[2] * 4 - 2, sailTop - 2, z - 1, m[2] * 4 + 2, sailTop, z + 1);
			box(root, "boom" + i, MAST, -m[2] * 4 - 1, sailTop + m[3] * 8, z - 1, m[2] * 4 + 1, sailTop + m[3] * 8 + 1.5F, z + 1);
			box(root, "crow" + i, TRIM, -3, top + 6, z - 3, 3, top + 7, z + 3);
		}
		box(root, "bowsprit", MAST, -1, deck - 6, -hb - 16 - l.bowsprit() * 8, 1, deck - 4, -hb - 14);
		// Helm wheel in front of the helm seat (seat 0).
		float helm = -(ship.seat(0)[1] + 0.8F) * 8;
		float helmFloor = deck - ship.helmHeight * 8;  // on the stern castle when there is one
		box(root, "helm_post", MAST, -1, helmFloor - 3, helm, 1, helmFloor, helm + 1);
		box(root, "helm_wheel", TRIM, -3, helmFloor - 8, helm - 0.5F, 3, helmFloor - 3, helm);  // below eye level
		// Cannons poking out of both sides.
		for (int c = 0; c < l.cannonsPerSide(); c++) {
			float z = -hb + 10 + c * ((2 * hb - 20) / Math.max(1, l.cannonsPerSide() - 1));
			box(root, "cannon_l" + c, MAST, -hw - 4, deck + 3, z - 1.5F, -hw, deck + 6, z + 1.5F);
			box(root, "cannon_r" + c, MAST, hw, deck + 3, z - 1.5F, hw + 4, deck + 6, z + 1.5F);
		}
		return LayerDefinition.create(mesh, 512, 1024);
	}

	public static LayerDefinition sails(ShipClass ship) {
		Layout l = layout(ship);
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		float deck = -l.deck() * 8;
		for (int i = 0; i < l.masts().length; i++) {
			float[] m = l.masts()[i];
			float sailTop = deck - m[1] * 8 + 4;
			root.addOrReplaceChild("sail" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-32, 0, -0.5F, 64, 48, 1),
					PartPose.offset(0, sailTop, -m[0] * 8 + 2));
		}
		// Flag on the tallest mast, trailing towards the stern.
		float[] main = l.masts()[0];
		for (float[] m : l.masts()) {
			if (m[1] > main[1]) {
				main = m;
			}
		}
		root.addOrReplaceChild("flag", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, -0.5F, 64, 48, 1),
				PartPose.offsetAndRotation(0, deck - main[1] * 8 - 1, -main[0] * 8, 0, -Mth.HALF_PI, 0));
		return LayerDefinition.create(mesh, 256, 128);
	}
}
