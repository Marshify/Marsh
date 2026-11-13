package com.grandline.client.fx;

import com.grandline.GrandLineMod;
import com.lowdragmc.photon.client.fx.BlockEffect;
import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The only class that touches Photon. Loaded only when Photon is installed ({@link GrandLineFx#photonLoaded()}).
 * Effects are Photon .fx files at assets/grandline/fx/&lt;name&gt;.fx; every method returns false when the file is
 * missing so Grand Line's own particles play instead.
 */
final class PhotonBridge {
	private PhotonBridge() {
	}

	private static FX fx(String name) {
		return FXHelper.getFX(GrandLineMod.id(name));
	}

	/** Effect that follows an entity (with an offset in the entity's look frame). */
	static boolean onEntity(String name, Level level, Entity entity, Vec3 offset) {
		FX fx = fx(name);
		if (fx == null) {
			return false;
		}
		EntityEffect effect = new EntityEffect(fx, level, entity, EntityEffect.AutoRotate.LOOK);
		effect.setOffset(new Vector3f((float) offset.x, (float) offset.y, (float) offset.z));
		effect.start();
		return true;
	}

	/** Effect at a fixed world point. */
	static boolean atPoint(String name, Level level, Vec3 pos) {
		FX fx = fx(name);
		if (fx == null) {
			return false;
		}
		BlockPos block = BlockPos.containing(pos);
		BlockEffect effect = new BlockEffect(fx, level, block);
		effect.setOffset(new Vector3f((float) (pos.x - block.getX() - 0.5), (float) (pos.y - block.getY() - 0.5),
				(float) (pos.z - block.getZ() - 0.5)));
		effect.setAllowMulti(true);
		effect.start();
		return true;
	}
}
