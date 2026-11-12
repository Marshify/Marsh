package com.grandline.fruit;

import com.grandline.combat.ability.AbilityHelpers;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Areas left behind by moves (lava pools, frozen fields, storm fields) that keep damaging enemies inside them for a
 * while. Server-only and in-memory: they simply end if the server stops.
 */
public final class LingeringAreas {
	private static final List<Area> AREAS = new ArrayList<>();

	private record Area(ServerPlayer owner, Vec3 center, double radius, Element element, float damagePerPulse,
			double multiplier, int[] ticksLeft) {
	}

	private LingeringAreas() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> AREAS.clear());
	}

	public static void add(ServerPlayer owner, Vec3 center, double radius, int durationTicks, Element element, float damagePerPulse,
			double multiplier) {
		AREAS.add(new Area(owner, center, radius, element, damagePerPulse, multiplier, new int[]{durationTicks}));
	}

	private static void tick() {
		Iterator<Area> iterator = AREAS.iterator();
		while (iterator.hasNext()) {
			Area area = iterator.next();
			int left = --area.ticksLeft()[0];
			if (left <= 0 || area.owner().isRemoved() || area.owner().hasDisconnected()) {
				iterator.remove();
				continue;
			}

			if (left % 5 == 0) {
				area.owner().serverLevel().sendParticles(area.element().particle(), area.center().x, area.center().y + 0.2,
						area.center().z, 6, area.radius() / 2, 0.1, area.radius() / 2, 0.01);
			}
			if (left % 10 == 0) {
				for (LivingEntity target : AbilityHelpers.inRadius(area.owner(), area.center(), area.radius())) {
					if (target.hurt(area.owner().serverLevel().damageSources().playerAttack(area.owner()),
							(float) (area.damagePerPulse() * area.multiplier()))) {
						switch (area.element()) {
							case ICE -> target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
									net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 3), area.owner());
							case LAVA, FIRE -> target.setSecondsOnFire(3);
							case SAND -> target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
									net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 1), area.owner());
							default -> {
							}
						}
					}
				}
			}
		}
	}
}
