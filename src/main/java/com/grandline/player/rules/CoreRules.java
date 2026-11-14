package com.grandline.player.rules;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.level.GameRules;

/**
 * Grand Line's survival rules that replace vanilla ones. Hunger and vanilla XP are handled by mixins (FoodDataMixin,
 * ExperienceOrbMixin, PlayerExperienceMixin); this class covers what fits events: clearing leftover vanilla XP, fall
 * damage and advancement announcements. Every rule can be switched off in the config.
 */
public final class CoreRules {
	private CoreRules() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> clearVanillaExperience(handler.getPlayer()));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && GrandLineMod.config().disableFallDamage && source.is(DamageTypeTags.IS_FALL)) {
				player.resetFallDistance();
				return false;
			}
			return true;
		});
		ServerLifecycleEvents.SERVER_STARTED.register(CoreRules::applyGameRules);
		HealthRegen.register();
		KeepInventory.register();
		OceanWeakness.register();
	}

	/** Existing worlds: players lose any vanilla XP they had (it can no longer be earned or used). */
	public static void clearVanillaExperience(ServerPlayer player) {
		if (!GrandLineMod.config().disableVanillaExperience) {
			return;
		}
		player.experienceLevel = 0;
		player.experienceProgress = 0;
		player.totalExperience = 0;
		player.setExperienceLevels(0);
	}

	private static void applyGameRules(MinecraftServer server) {
		if (GrandLineMod.config().hideAdvancementAnnouncements) {
			server.getGameRules().getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, server);
		}
	}
}
