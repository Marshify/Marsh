package com.grandline.combat;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Who gets the credit for a death. Every time a player damages something (melee, a move, a projectile they own), that
 * player is remembered as its attacker. If the creature then dies from something without an attacker of its own (the
 * fire a Flame move lit, a fall after a knockback, drowning, a lingering area, the void), the last player who hit it
 * within {@code killCreditSeconds} gets the kill. Policy: the most recent player hit wins (so a finishing blow by a
 * second player counts for them). A death caused by another creature, or long after the last player hit, credits no
 * one. One death is credited once.
 */
public final class CombatCredit {
	private record Hit(UUID player, long tick) {
	}

	private static final Map<LivingEntity, Hit> LAST_PLAYER_HIT = new WeakHashMap<>();
	private static final Map<LivingEntity, Boolean> CREDITED = new WeakHashMap<>();

	private CombatCredit() {
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			ServerPlayer attacker = playerBehind(source);
			if (attacker != null && attacker != entity && amount > 0) {
				LAST_PLAYER_HIT.put(entity, new Hit(attacker.getUUID(), attacker.server.getTickCount()));
			}
			return true;
		});
	}

	/** The player who dealt this damage directly or through something they own (a projectile, a summoned effect). */
	public static ServerPlayer playerBehind(DamageSource source) {
		Entity e = source.getEntity();
		if (e instanceof ServerPlayer p) {
			return p;
		}
		Entity direct = source.getDirectEntity();
		if (direct instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() instanceof ServerPlayer p) {
			return p;
		}
		return null;
	}

	/**
	 * The player credited for {@code victim} dying from {@code source}, or null. Returns null the second time it is
	 * asked about the same death, so rewards cannot be paid twice.
	 */
	public static ServerPlayer killer(LivingEntity victim, DamageSource source) {
		if (CREDITED.put(victim, Boolean.TRUE) != null) {
			return null;
		}
		ServerPlayer direct = playerBehind(source);
		if (direct != null) {
			return direct;
		}
		if (source.getEntity() instanceof LivingEntity other && !(other instanceof Player)) {
			return null; // killed by another creature: not the player's kill
		}
		return recentAttacker(victim);
	}

	/** The last player to hurt {@code victim} within the credit window, if they are still online. */
	public static ServerPlayer recentAttacker(LivingEntity victim) {
		Hit hit = LAST_PLAYER_HIT.get(victim);
		MinecraftServer server = victim.getServer();
		if (hit == null || server == null) {
			return null;
		}
		long window = Math.round(GrandLineMod.config().killCreditSeconds * 20);
		if (server.getTickCount() - hit.tick() > window) {
			return null;
		}
		return server.getPlayerList().getPlayer(hit.player());
	}
}
