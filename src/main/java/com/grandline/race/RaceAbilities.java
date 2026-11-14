package com.grandline.race;

import com.grandline.combat.fx.FxEvents;
import com.grandline.config.RaceBalance;
import com.grandline.network.ServerRequestGuard;
import com.grandline.network.payload.RaceActionPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

/**
 * V3 race abilities (key R) and V4 awakenings (key Y): activation checks, timers, per-second effects and the beats the
 * client draws. Everything is decided here; the client only asks.
 */
public final class RaceAbilities {
	private RaceAbilities() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(RaceActionPayload.TYPE, (packet, player, sender) -> {
			if (ServerRequestGuard.check(player, RaceActionPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
				return;
			}
			String result = switch (packet.action()) {
				case RaceActionPayload.V3 -> useV3(player);
				case RaceActionPayload.V4 -> awaken(player);
				case RaceActionPayload.UPGRADE -> buyUpgrade(player, packet.arg());
				case RaceNpcs.EVOLVE, RaceNpcs.START_QUEST, RaceNpcs.REROLL, RaceNpcs.BECOME, RaceNpcs.TRIAL -> RaceNpcs.act(player, packet.action(), packet.arg());
				default -> "race.grandline.unknown";
			};
			if (result != null) {
				player.displayClientMessage(Component.translatable(result).withStyle(ChatFormatting.RED), true);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(RaceAbilities::tick);
	}

	/** Uses the V3 ability. Returns a message key if it can't be used, else null. */
	public static String useV3(ServerPlayer player) {
		RaceData data = RaceService.get(player);
		if (data.stage() < 3) {
			return "race.grandline.v3.locked";
		}
		if (!player.isAlive() || player.isSpectator()) {
			return "race.grandline.v3.cannot";
		}
		long now = player.serverLevel().getGameTime();
		RaceBalance b = RaceService.balance(data.kind());
		long ready = data.counter("v3_ready");
		if (ready - now > (long) (b.v3Cooldown * 20) + 20) {
			ready = now; // a clock that went backwards (another world, a reset) never locks the ability
		}
		if (ready > now) {
			return "race.grandline.v3.cooldown";
		}
		if (RaceService.v3Active(player)) {
			return "race.grandline.v3.active";
		}
		RaceService.V3_UNTIL.put(player.getUUID(), now + (long) (b.v3Duration * 20));
		long readyAt = now + (long) ((b.v3Duration + b.v3Cooldown) * 20);
		RaceService.update(player, d -> d.withCounter("v3_ready", readyAt));
		onV3Start(player, data.kind(), b);
		RacePassives.apply(player);
		return null;
	}

	private static void onV3Start(ServerPlayer player, Race race, RaceBalance b) {
		Vec3 at = player.position();
		FxEvents.send(player, "race_v3_" + race.id(), at, 1);
		switch (race) {
			case HUMAN -> sound(player, SoundEvents.RAVAGER_ROAR, 0.7F, 1.3F);
			case SHARK -> sound(player, SoundEvents.TRIDENT_RIPTIDE_3, 1F, 0.9F);
			case ANGEL -> sound(player, SoundEvents.BEACON_ACTIVATE, 1F, 1.6F);
			case RABBIT -> sound(player, SoundEvents.RABBIT_JUMP, 1F, 0.7F);
			case CYBORG -> {
				sound(player, SoundEvents.BEACON_POWER_SELECT, 1F, 1.8F);
				pulse(player, b.v3Power, 5);
				com.grandline.combat.EnergyService.restore(player, 0.25);
			}
			case GHOUL -> {
				sound(player, SoundEvents.WARDEN_HEARTBEAT, 1.2F, 1F);
				revealNearby(player, 24, (int) (b.v3Duration * 20));
			}
		}
	}

	/** Awakens (V4). Returns a message key if it can't, else null. */
	public static String awaken(ServerPlayer player) {
		RaceData data = RaceService.get(player);
		if (data.stage() < 4) {
			return "race.grandline.v4.locked";
		}
		if (RaceService.v4Active(player)) {
			return "race.grandline.v4.active";
		}
		if (data.energy() < 100) {
			return "race.grandline.v4.energy";
		}
		if (!player.isAlive() || player.isSpectator()) {
			return "race.grandline.v3.cannot";
		}
		RaceBalance b = RaceService.balance(data.kind());
		long now = player.serverLevel().getGameTime();
		double seconds = b.v4Duration + RaceUpgrades.bonus(data, "duration");
		RaceService.V4_UNTIL.put(player.getUUID(), now + (long) (seconds * 20));
		RaceService.update(player, d -> d.withEnergy(0));
		FxEvents.send(player, "race_v4_" + data.kind().id(), player.position(), 1.5);
		sound(player, SoundEvents.TOTEM_USE, 0.8F, 0.8F);
		if (data.kind() == Race.CYBORG) {
			pulse(player, b.v4Power + RaceUpgrades.bonus(data, "pulse"), 6);
		}
		if (data.kind() == Race.SHARK) {
			pulse(player, 6, 5); // Leviathan's first wave
		}
		RacePassives.apply(player);
		return null;
	}

	public static String buyUpgrade(ServerPlayer player, int index) {
		RaceData data = RaceService.get(player);
		var list = RaceUpgrades.forRace(data.kind());
		if (index < 0 || index >= list.size()) {
			return "race.grandline.upgrade.unknown";
		}
		String id = list.get(index).id();
		String problem = RaceUpgrades.problem(data, id);
		if (problem != null) {
			return problem;
		}
		RaceService.update(player, d -> {
			java.util.List<String> owned = new java.util.ArrayList<>(d.upgrades());
			owned.add(id);
			return d.withUpgrades(owned);
		});
		player.displayClientMessage(Component.translatable("race.grandline.upgrade.bought",
				Component.translatable("race.grandline.upgrade." + id)).withStyle(ChatFormatting.GOLD), true);
		sound(player, SoundEvents.PLAYER_LEVELUP, 0.7F, 1.2F);
		return null;
	}

	private static void tick(MinecraftServer server) {
		boolean second = server.getTickCount() % 20 == 0;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID id = player.getUUID();
			long now = player.serverLevel().getGameTime();
			endIfOver(player, RaceService.V3_UNTIL, now);
			endIfOver(player, RaceService.V4_UNTIL, now);
			if (!second) {
				continue;
			}
			RaceData data = RaceService.get(player);
			RaceBalance b = RaceService.balance(data.kind());
			boolean v3 = RaceService.v3Active(player), v4 = RaceService.v4Active(player);
			if (data.kind() == Race.ANGEL && (v3 || v4) && !com.grandline.player.rules.OceanWeakness.affected(player)) {
				double share = (v3 ? b.v3Power : 0) + (v4 ? b.v4Power + RaceUpgrades.bonus(data, "heal") : 0);
				player.heal((float) (player.getMaxHealth() * share));
			}
			if (data.kind() == Race.SHARK && v4 && (now / 20) % 3 == 0) {
				pulse(player, 4 + b.v4Power * 6, 4); // Leviathan: a tidal wave every 3 s
			}
			if (data.kind() == Race.CYBORG && v4 && (now / 20) % 2 == 0) {
				pulse(player, b.v4Power + RaceUpgrades.bonus(data, "pulse"), 5); // Overdrive: a discharge every 2 s
			}
			if (data.kind() == Race.GHOUL && v4) {
				revealNearby(player, 24, 40);
			}
			if (v3 || v4) {
				FxEvents.send(player, "race_aura_" + data.kind().id(), player.position(), v4 ? 1.5 : 1);
			}
		}
	}

	private static void endIfOver(ServerPlayer player, Map<UUID, Long> until, long now) {
		Long end = until.get(player.getUUID());
		if (end != null && end <= now) {
			until.remove(player.getUUID());
			RacePassives.apply(player);
			RaceService.sync(player);
		}
	}

	/**
	 * Damages and pushes back hostile creatures (never players) within {@code radius}. The damage grows with the
	 * player's level (+2% per level) so pulses stay useful later on.
	 */
	static void pulse(ServerPlayer player, double baseDamage, double radius) {
		double damage = baseDamage * (1 + 0.02 * com.grandline.player.PlayerProfileService.get(player).level());
		com.grandline.combat.fx.FxEvents.send(player, "race_pulse_" + RaceService.race(player).id(), player.position(), radius);
		for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
				e -> e != player && e.isAlive() && e instanceof Mob && !(e instanceof com.grandline.entity.Islander))) {
			e.hurt(player.damageSources().playerAttack(player), (float) damage);
			Vec3 push = e.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(0.8);
			e.push(push.x, 0.35, push.z);
		}
	}

	private static void revealNearby(ServerPlayer player, double radius, int ticks) {
		for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
				m -> m.isAlive() && m instanceof net.minecraft.world.entity.monster.Enemy)) {
			mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
		}
	}

	private static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		player.serverLevel().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
	}
}
