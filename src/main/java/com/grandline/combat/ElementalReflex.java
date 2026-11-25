package com.grandline.combat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grandline.GrandLineMod;
import com.grandline.combat.fx.FxEvents;
import com.grandline.player.PlayerProfileService;
import com.grandline.technique.CombatContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Elemental Reflex (Logia intangibility), one server-side interception shared by every Elemental fruit.
 *
 * <p>A fruit registers a {@link Profile} during its own rework; only registered fruits have the passive (Natural and
 * Beast fruits never do, and unfinished Elemental fruits stay inactive until they register). The check runs first in
 * the damage pipeline (before Instinct, so no dodge charge is spent on a hit the body already lets through), whatever
 * the player is holding, with no energy, cooldown or activation. An attack is negated only when it is
 * <em>conventional</em> ({@link #classify}): a physical melee/sword/gun hit or projectile from a living attacker, not
 * Aura-enhanced, not a fruit move, not a boss, not the environment (water, falls, fire, the void), not one of the
 * NPC's bypass attacks, and (for NPC attackers only) the player's level meets that NPC type's threshold
 * ({@code data/grandline/elemental_reflex/npc_thresholds.json}, overridable in the config folder). A negated hit is
 * cancelled at the start of {@code hurt}: no damage, knockback, stagger, effects, lifesteal or rewards, and it does
 * not count as damage for interruptions. The fruit's reaction (visuals, sound) is rate-limited; the rate limit never
 * affects immunity.
 */
public final class ElementalReflex {
	/** Why an attack was or wasn't negated. */
	public enum Category {
		CONVENTIONAL_PLAYER, CONVENTIONAL_NPC, NPC_BELOW_THRESHOLD, AURA, FRUIT_OR_SPECIAL, BOSS, NPC_BYPASS, ENVIRONMENT
	}

	/**
	 * One Elemental fruit's reflex.
	 *
	 * @param active    extra activation requirement (e.g. not while transformed); usually always true
	 * @param exception attacks this fruit cannot negate even when conventional
	 * @param react     cosmetic reaction (rate-limited): player, source, contact point, attack direction
	 */
	public record Profile(String fruitId, Predicate<ServerPlayer> active, BiPredicate<ServerPlayer, DamageSource> exception, Reaction react) {
	}

	@FunctionalInterface
	public interface Reaction {
		void react(ServerPlayer player, DamageSource source, Vec3 contact, Vec3 direction);
	}

	/** One NPC type's rule (see the JSON). */
	public record NpcRule(int minPlayerLevel, int levelMargin, boolean aura, boolean bypass, boolean boss, int sea, Set<String> bypassAttacks) {
	}

	private static final Map<String, Profile> PROFILES = new HashMap<>();
	private static final Map<String, NpcRule> NPC = new HashMap<>();
	private static final Map<Integer, Integer> SEA_FLOORS = new HashMap<>();
	private static NpcRule fallback = new NpcRule(1, 0, false, false, false, 1, Set.of());
	private static final Map<UUID, Long> LAST_FX = new HashMap<>();
	/** Cosmetic rate limit: at most one reaction per player every this many ticks. */
	public static final int FX_GAP = 3;

	private ElementalReflex() {
	}

	public static void register(Profile profile) {
		PROFILES.put(profile.fruitId(), profile);
	}

	public static boolean hasReflex(ServerPlayer player) {
		var powers = PlayerProfileService.get(player).powers();
		return powers.hasFruit() && PROFILES.containsKey(powers.fruit());
	}

	/** Called first in the damage pipeline: true if the hit is negated (and the reaction has been played). */
	public static boolean negates(ServerPlayer player, DamageSource source) {
		if (!GrandLineMod.config().elementalImmunity || NPC.isEmpty() && !load()) {
			return false;
		}
		var powers = PlayerProfileService.get(player).powers();
		Profile profile = powers.hasFruit() ? PROFILES.get(powers.fruit()) : null;
		if (profile == null || !profile.active().test(player)) {
			return false;
		}
		Category c = classify(player, source);
		if (c != Category.CONVENTIONAL_PLAYER && c != Category.CONVENTIONAL_NPC || profile.exception().test(player, source)) {
			return false;
		}
		long now = player.serverLevel().getGameTime();
		Long last = LAST_FX.get(player.getUUID());
		if (last == null || now - last >= FX_GAP || now < last) {
			LAST_FX.put(player.getUUID(), now);
			Vec3 from = source.getSourcePosition() != null ? source.getSourcePosition() : player.getEyePosition().add(player.getLookAngle());
			Vec3 centre = player.getBoundingBox().getCenter();
			Vec3 dir = centre.subtract(from);
			dir = dir.lengthSqr() < 1e-6 ? player.getLookAngle().scale(-1) : dir.normalize();
			double y = Math.max(player.getY() + 0.2, Math.min(player.getY() + player.getBbHeight() - 0.1, from.y));
			Vec3 contact = new Vec3(centre.x, y, centre.z).subtract(dir.multiply(1, 0, 1).scale(player.getBbWidth() * 0.4));
			profile.react().react(player, source, contact, dir);
		}
		return true;
	}

	/** Classifies an incoming attack for Elemental Reflex. */
	public static Category classify(ServerPlayer player, DamageSource source) {
		if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == player || !CombatContext.combat(source, player)) {
			return Category.ENVIRONMENT;
		}
		if (!CombatContext.physical(source) || CombatContext.instinctBreaking() && CombatContext.currentPower() != null
				&& CombatContext.currentPower().scalingStat() == com.grandline.player.StatType.FRUIT) {
			return Category.FRUIT_OR_SPECIAL;
		}
		if (DefensivePassives.auraEnhanced(source)) {
			return Category.AURA;
		}
		if (attacker instanceof ServerPlayer) {
			return Category.CONVENTIONAL_PLAYER; // player attacks ignore NPC thresholds
		}
		NpcRule rule = rule(attacker);
		if (rule.boss() || attacker instanceof com.grandline.entity.IslandBoss || attacker instanceof com.grandline.entity.SaberBoss) {
			return Category.BOSS;
		}
		if (rule.aura()) {
			return Category.AURA;
		}
		String damageType = source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("");
		if (rule.bypass() || rule.bypassAttacks().contains(damageType)) {
			return Category.NPC_BYPASS;
		}
		int npcLevel = attacker instanceof com.grandline.entity.GrandLineEnemy e ? e.grandLineLevel() : 0;
		int need = Math.max(rule.minPlayerLevel(), Math.max(npcLevel + rule.levelMargin(), SEA_FLOORS.getOrDefault(rule.sea(), 1)));
		return PlayerProfileService.get(player).level() >= need ? Category.CONVENTIONAL_NPC : Category.NPC_BELOW_THRESHOLD;
	}

	/** The threshold rule for an NPC type (by registry id). */
	public static NpcRule rule(LivingEntity npc) {
		if (NPC.isEmpty()) {
			load();
		}
		return NPC.getOrDefault(BuiltInRegistries.ENTITY_TYPE.getKey(npc.getType()).toString(), fallback);
	}

	/** Loads the bundled thresholds, then the config-folder override if present. */
	public static synchronized boolean load() {
		NPC.clear();
		SEA_FLOORS.clear();
		try (InputStream in = ElementalReflex.class.getResourceAsStream("/data/grandline/elemental_reflex/npc_thresholds.json")) {
			if (in != null) {
				apply(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
			}
			Path override = FabricLoader.getInstance().getConfigDir().resolve("grandline/elemental_reflex.json");
			if (Files.exists(override)) {
				apply(JsonParser.parseString(Files.readString(override)).getAsJsonObject());
			}
		} catch (Exception e) {
			GrandLineMod.LOGGER.warn("Elemental Reflex thresholds failed to load; using defaults", e);
		}
		if (NPC.isEmpty()) {
			NPC.put("grandline:none", fallback); // never reload every hit
		}
		return true;
	}

	private static void apply(JsonObject json) {
		if (json.has("sea_floors")) {
			json.getAsJsonObject("sea_floors").entrySet().forEach(e -> SEA_FLOORS.put(Integer.parseInt(e.getKey()), e.getValue().getAsInt()));
		}
		if (json.has("default")) {
			fallback = parse(json.getAsJsonObject("default"));
		}
		if (json.has("npcs")) {
			json.getAsJsonObject("npcs").entrySet().forEach(e -> NPC.put(e.getKey(), parse(e.getValue().getAsJsonObject())));
		}
	}

	private static NpcRule parse(JsonObject o) {
		Set<String> attacks = new HashSet<>();
		if (o.has("bypass_attacks")) {
			o.getAsJsonArray("bypass_attacks").forEach(a -> attacks.add(a.getAsString()));
		}
		return new NpcRule(o.has("min_player_level") ? o.get("min_player_level").getAsInt() : 1, o.has("level_margin") ? o.get("level_margin").getAsInt() : 0,
				o.has("aura") && o.get("aura").getAsBoolean(), o.has("bypass") && o.get("bypass").getAsBoolean(), o.has("boss") && o.get("boss").getAsBoolean(),
				o.has("sea") ? o.get("sea").getAsInt() : 1, attacks);
	}

	/** Default reaction helper: a beat named {@code effect} at the contact point. */
	public static Reaction beat(String effect) {
		return (player, source, contact, dir) -> FxEvents.send(player, effect, contact, dir, 0, 0, contact.y - player.getY());
	}
}
