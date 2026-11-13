package com.grandline.player.rules;

import com.grandline.GrandLineMod;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/**
 * The sea weakens fruit users: a player with a fruit who is submerged in ocean water past {@code oceanMinSubmersion}
 * loses {@code oceanDamagePercentPerSecond} of maximum health each second (at most once per second, server-side).
 * Ocean water = water in an ocean biome whose surface is at sea level, so fountains and hilltop ponds are safe; rain
 * and standing beside water never count. Other systems (e.g. races, later) scale it through {@link #addModifier}:
 * a multiplier of 0 makes a player immune.
 */
public final class OceanWeakness {
	public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, GrandLineMod.id("sea_weakness"));
	private static final ResourceKey<net.minecraft.world.level.biome.Biome> SEA_BIOME =
			ResourceKey.create(Registries.BIOME, GrandLineMod.id("grand_line_sea"));
	private static final List<ToDoubleFunction<ServerPlayer>> MODIFIERS = new ArrayList<>();
	private static final Map<UUID, Integer> LAST_HIT = new HashMap<>();

	private OceanWeakness() {
	}

	/** Adds a damage multiplier source (1.0 unchanged, 0.5 halved, 0 immune). Multipliers stack by multiplying. */
	public static void addModifier(ToDoubleFunction<ServerPlayer> modifier) {
		MODIFIERS.add(modifier);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(OceanWeakness::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_HIT.remove(handler.getPlayer().getUUID()));
	}

	public static double multiplier(ServerPlayer player) {
		double m = 1.0;
		for (ToDoubleFunction<ServerPlayer> f : MODIFIERS) {
			m *= Math.max(0, f.applyAsDouble(player));
		}
		return m;
	}

	/** Whether the sea is weakening this player right now. */
	public static boolean affected(ServerPlayer player) {
		var config = GrandLineMod.config();
		if (config.oceanDamagePercentPerSecond <= 0 || !player.gameMode.isSurvival() || !player.isAlive()
				|| !PlayerProfileService.get(player).powers().hasFruit()) {
			return false;
		}
		if (!player.isInWater() || player.getFluidHeight(FluidTags.WATER) < config.oceanMinSubmersion) {
			return false;
		}
		return !config.oceanOnly || isOceanWater(player.serverLevel(), player.blockPosition());
	}

	/** Water at {@code feet} belongs to the ocean: an ocean biome, and the water column's surface is at sea level. */
	public static boolean isOceanWater(ServerLevel level, BlockPos feet) {
		var biome = level.getBiome(feet);
		if (!biome.is(SEA_BIOME) && !biome.is(BiomeTags.IS_OCEAN) && !biome.is(BiomeTags.IS_DEEP_OCEAN)) {
			return false;
		}
		BlockPos.MutableBlockPos p = feet.mutable();
		while (level.getFluidState(p).is(FluidTags.WATER) && p.getY() < level.getMaxBuildHeight()) {
			p.move(0, 1, 0);
		}
		return p.getY() - 1 < GrandLineMod.config().oceanMaxSurfaceY;
	}

	public static DamageSource source(ServerLevel level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE));
	}

	private static void tick(MinecraftServer server) {
		int now = server.getTickCount();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!affected(player)) {
				continue;
			}
			Integer last = LAST_HIT.get(player.getUUID());
			if (last != null && now - last < 20) {
				continue;
			}
			double m = multiplier(player);
			if (m <= 0) {
				continue;
			}
			LAST_HIT.put(player.getUUID(), now);
			float damage = (float) (player.getMaxHealth() * GrandLineMod.config().oceanDamagePercentPerSecond / 100.0 * m);
			player.invulnerableTime = 0; // a recent hit must not swallow the sea's damage
			player.hurt(source(player.serverLevel()), damage);
			if (last == null || now - last > 40) {
				player.displayClientMessage(Component.translatable("message.grandline.sea_weakness").withStyle(ChatFormatting.AQUA), true);
			}
		}
	}
}
