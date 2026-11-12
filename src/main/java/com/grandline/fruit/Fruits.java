package com.grandline.fruit;

import com.grandline.GrandLineMod;
import com.grandline.combat.ability.AbilityAction;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.Power;
import com.grandline.combat.ability.PowerSource;
import com.grandline.combat.ability.Powers;
import com.grandline.player.StatType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The fruit roster. Every fruit is a {@link Power} (5 moves, scaled by the Fruit stat) plus an edible {@link FruitItem}.
 * Moves unlock at mastery 0 (Z), 10 (X), 15 (R), 25 (C) and 50 (V).
 */
public final class Fruits {
	public record FruitDef(Power power, FruitRarity rarity, FruitItem item) {
		public String id() {
			return power.id().toString();
		}
	}

	private static final Map<ResourceLocation, FruitDef> FRUITS = new LinkedHashMap<>();

	public static final FruitDef GALE = register("gale", FruitRarity.COMMON, Element.WIND, 0x9BE8CF,
			FruitMoves.bolt(Element.WIND, 12, 1.4, 6f),
			FruitMoves.burst(Element.WIND, 5, 8f),
			FruitMoves.bolt(Element.WIND, 18, 2.0, 10f),
			FruitMoves.eruption(Element.WIND, 14, 5, 14f, 1.0),
			FruitMoves.updraft(Element.WIND),
			"gale_bullet", "gale_cyclone", "gale_lance", "gale_hurricane", "gale_updraft");

	public static final FruitDef DUNE = register("dune", FruitRarity.UNCOMMON, Element.SAND, 0xE0B866,
			FruitMoves.bolt(Element.SAND, 12, 1.4, 7f),
			FruitMoves.burst(Element.SAND, 5, 9f),
			FruitMoves.lingering(Element.SAND, 14, 3.5, 6f, 3f, 5),
			FruitMoves.eruption(Element.SAND, 16, 5.5, 16f, 0.8),
			FruitMoves.dash(Element.SAND, 1.8, 5f),
			"dune_shot", "dune_sandstorm", "dune_quicksand", "dune_collapse", "dune_dash");

	/** Flame Fruit (id kept as "ember" for save compatibility): faithful base-form moveset, see FlameMoves. */
	public static final FruitDef EMBER = register("ember", FruitRarity.RARE, 0xFF7A33, com.grandline.fruit.flame.FlameMoves.moves());

	public static final FruitDef FROST = register("frost", FruitRarity.RARE, Element.ICE, 0x8FD0FF,
			FruitMoves.bolt(Element.ICE, 14, 1.3, 8f),
			FruitMoves.burst(Element.ICE, 5.5, 9f),
			FruitMoves.bolt(Element.ICE, 20, 1.8, 11f),
			FruitMoves.lingering(Element.ICE, 16, 5, 10f, 4f, 6),
			FruitMoves.dash(Element.ICE, 2.2, 4f),
			"frost_spike", "frost_nova", "frost_lance", "frost_field", "frost_glide");

	public static final FruitDef LAVA = register("lava", FruitRarity.LEGENDARY, Element.LAVA, 0xFF5A1F,
			FruitMoves.bolt(Element.LAVA, 14, 1.4, 9f),
			FruitMoves.lingering(Element.LAVA, 14, 3.5, 7f, 5f, 6),
			FruitMoves.eruption(Element.LAVA, 16, 4.5, 15f, 1.3),
			FruitMoves.eruption(Element.LAVA, 22, 7, 22f, 0.9),
			FruitMoves.dash(Element.LAVA, 2.0, 7f),
			"lava_bolt", "lava_pool", "lava_eruption", "lava_meteor", "lava_dash");

	public static final FruitDef VOLT = register("volt", FruitRarity.MYTHICAL, Element.LIGHTNING, 0xFFE95A,
			FruitMoves.bolt(Element.LIGHTNING, 18, 1.2, 9f),
			FruitMoves.burst(Element.LIGHTNING, 5.5, 12f),
			FruitMoves.eruption(Element.LIGHTNING, 24, 4, 20f, 0.4),
			FruitMoves.lingering(Element.LIGHTNING, 18, 5, 12f, 6f, 6),
			FruitMoves.blink(Element.LIGHTNING, 10),
			"volt_spark", "volt_clap", "volt_strike", "volt_storm", "volt_step");

	// --- Common ---
	public static final FruitDef BOUNCE = register("bounce", FruitRarity.COMMON, Element.BOUNCE, 0xFF8FD1,
			FruitMoves.strike(Element.BOUNCE, 10, 6f), FruitMoves.burst(Element.BOUNCE, 4.5, 7f), FruitMoves.leap(Element.BOUNCE, 4, 9f),
			FruitMoves.eruption(Element.BOUNCE, 14, 5, 12f, 1.2), FruitMoves.updraft(Element.BOUNCE),
			"bounce_punch", "bounce_wave", "bounce_slam", "bounce_big", "bounce_jump");
	public static final FruitDef THORN = register("thorn", FruitRarity.COMMON, Element.PLANT, 0x5FA84A,
			FruitMoves.bolt(Element.PLANT, 12, 1.3, 6f), FruitMoves.burst(Element.PLANT, 5, 7f),
			FruitMoves.lingering(Element.PLANT, 14, 3.5, 5f, 3f, 5), FruitMoves.eruption(Element.PLANT, 15, 5, 13f, 0.7),
			FruitMoves.dash(Element.PLANT, 1.6, 4f),
			"thorn_shot", "thorn_ring", "thorn_patch", "thorn_eruption", "thorn_swing");
	public static final FruitDef MIST = register("mist", FruitRarity.COMMON, Element.MIST, 0xC9D2DC,
			FruitMoves.bolt(Element.MIST, 12, 1.6, 5f), FruitMoves.burst(Element.MIST, 5.5, 6f),
			FruitMoves.lingering(Element.MIST, 14, 4, 4f, 3f, 6), FruitMoves.vortex(Element.MIST, 16, 5, 12f),
			FruitMoves.blink(Element.MIST, 8),
			"mist_bolt", "mist_cloud", "mist_haze", "mist_maelstrom", "mist_fade");

	// --- Uncommon ---
	public static final FruitDef TIDE = register("tide", FruitRarity.UNCOMMON, Element.WATER, 0x3F8FE0,
			FruitMoves.bolt(Element.WATER, 14, 1.4, 7f), FruitMoves.burst(Element.WATER, 5, 8f), FruitMoves.bolt(Element.WATER, 20, 2.2, 10f),
			FruitMoves.eruption(Element.WATER, 18, 6, 15f, 0.9), FruitMoves.dash(Element.WATER, 2.0, 4f),
			"tide_jet", "tide_ring", "tide_lance", "tide_crash", "tide_surf");
	public static final FruitDef BLAST = register("blast", FruitRarity.UNCOMMON, Element.EXPLOSION, 0x5A3A3A,
			FruitMoves.eruption(Element.EXPLOSION, 14, 2.5, 8f, 0.5), FruitMoves.burst(Element.EXPLOSION, 5, 9f),
			FruitMoves.eruption(Element.EXPLOSION, 18, 4, 12f, 0.8), FruitMoves.eruption(Element.EXPLOSION, 20, 6.5, 17f, 1.0),
			FruitMoves.leap(Element.EXPLOSION, 3, 5f),
			"blast_shot", "blast_detonate", "blast_cluster", "blast_grand", "blast_jump");
	public static final FruitDef CORAL = register("coral", FruitRarity.UNCOMMON, Element.CRYSTAL, 0xFF7F6B,
			FruitMoves.bolt(Element.CRYSTAL, 12, 1.2, 7f), FruitMoves.burst(Element.CRYSTAL, 5, 8f),
			FruitMoves.lingering(Element.CRYSTAL, 14, 3.5, 6f, 3f, 5), FruitMoves.eruption(Element.CRYSTAL, 16, 5, 15f, 0.8),
			FruitMoves.barrier(Element.CRYSTAL, 5, 1),
			"coral_shard", "coral_spikes", "coral_garden", "coral_eruption", "coral_armor");
	public static final FruitDef IRON = register("iron", FruitRarity.UNCOMMON, Element.METAL, 0x9AA3AE,
			FruitMoves.strike(Element.METAL, 5, 9f), FruitMoves.burst(Element.METAL, 4, 9f), FruitMoves.strike(Element.METAL, 7, 13f),
			FruitMoves.eruption(Element.METAL, 12, 4.5, 16f, 0.8), FruitMoves.barrier(Element.METAL, 6, 2),
			"iron_fist", "iron_shockwave", "iron_hammer", "iron_fall", "iron_skin");

	// --- Rare ---
	public static final FruitDef TOXIN = register("toxin", FruitRarity.RARE, Element.POISON, 0x8E4BD6,
			FruitMoves.bolt(Element.POISON, 14, 1.3, 7f), FruitMoves.lingering(Element.POISON, 14, 3.5, 6f, 4f, 6),
			FruitMoves.burst(Element.POISON, 5.5, 10f), FruitMoves.vortex(Element.POISON, 18, 5.5, 15f), FruitMoves.dash(Element.POISON, 2.0, 5f),
			"toxin_dart", "toxin_puddle", "toxin_burst", "toxin_storm", "toxin_rush");
	public static final FruitDef BLOOM = register("bloom", FruitRarity.RARE, Element.PLANT, 0xF29BC4,
			FruitMoves.bolt(Element.PLANT, 14, 1.3, 7f), FruitMoves.heal(Element.PLANT, 8f, 6),
			FruitMoves.lingering(Element.PLANT, 16, 4, 8f, 4f, 6), FruitMoves.eruption(Element.PLANT, 18, 5.5, 16f, 0.9),
			FruitMoves.barrier(Element.PLANT, 6, 1),
			"bloom_petal", "bloom_heal", "bloom_garden", "bloom_root", "bloom_bark");
	public static final FruitDef ECHO = register("echo", FruitRarity.RARE, Element.SOUND, 0x6B5BD6,
			FruitMoves.bolt(Element.SOUND, 16, 1.6, 8f), FruitMoves.burst(Element.SOUND, 6, 10f), FruitMoves.bolt(Element.SOUND, 22, 2.0, 12f),
			FruitMoves.vortex(Element.SOUND, 18, 6, 16f), FruitMoves.blink(Element.SOUND, 9),
			"echo_wave", "echo_resonance", "echo_lance", "echo_chorus", "echo_step");
	public static final FruitDef PRISM = register("prism", FruitRarity.RARE, Element.LIGHT, 0xFFF3A0,
			FruitMoves.bolt(Element.LIGHT, 20, 1.1, 8f), FruitMoves.burst(Element.LIGHT, 5, 10f), FruitMoves.bolt(Element.LIGHT, 26, 1.6, 13f),
			FruitMoves.eruption(Element.LIGHT, 22, 5, 18f, 0.6), FruitMoves.blink(Element.LIGHT, 14),
			"prism_ray", "prism_flash", "prism_beam", "prism_sunfall", "prism_speed");
	public static final FruitDef QUARTZ = register("quartz", FruitRarity.RARE, Element.CRYSTAL, 0xE6E6F5,
			FruitMoves.bolt(Element.CRYSTAL, 14, 1.2, 8f), FruitMoves.burst(Element.CRYSTAL, 5, 10f),
			FruitMoves.eruption(Element.CRYSTAL, 16, 4, 13f, 1.0), FruitMoves.lingering(Element.CRYSTAL, 16, 5, 12f, 5f, 6),
			FruitMoves.barrier(Element.CRYSTAL, 6, 2),
			"quartz_spike", "quartz_nova", "quartz_spire", "quartz_prison", "quartz_skin");

	// --- Legendary ---
	public static final FruitDef UMBRA = register("umbra", FruitRarity.LEGENDARY, Element.SHADOW, 0x3A2A55,
			FruitMoves.bolt(Element.SHADOW, 16, 1.4, 9f), FruitMoves.vortex(Element.SHADOW, 14, 5, 11f), FruitMoves.burst(Element.SHADOW, 6, 13f),
			FruitMoves.lingering(Element.SHADOW, 18, 6, 16f, 6f, 6), FruitMoves.blink(Element.SHADOW, 12),
			"umbra_bolt", "umbra_grasp", "umbra_eclipse", "umbra_abyss", "umbra_walk");
	public static final FruitDef ORBIT = register("orbit", FruitRarity.LEGENDARY, Element.GRAVITY, 0x7A4FB8,
			FruitMoves.strike(Element.GRAVITY, 14, 9f), FruitMoves.vortex(Element.GRAVITY, 16, 5, 12f),
			FruitMoves.eruption(Element.GRAVITY, 18, 5, 15f, 0.0), FruitMoves.eruption(Element.GRAVITY, 24, 7, 22f, 0.0),
			FruitMoves.updraft(Element.GRAVITY),
			"orbit_shot", "orbit_singularity", "orbit_crush", "orbit_star", "orbit_zero");
	public static final FruitDef WOLF = register("wolf", FruitRarity.LEGENDARY, Element.BEAST, 0x8C8C9A,
			FruitMoves.strike(Element.BEAST, 5, 10f), FruitMoves.dash(Element.BEAST, 2.2, 10f), FruitMoves.burst(Element.BEAST, 5, 12f),
			FruitMoves.leap(Element.BEAST, 5, 18f), FruitMoves.dash(Element.BEAST, 2.4, 0f),
			"wolf_fang", "wolf_pounce", "wolf_howl", "wolf_rampage", "wolf_sprint");
	public static final FruitDef NOVA = register("nova", FruitRarity.LEGENDARY, Element.LIGHT, 0xFFC94B,
			FruitMoves.bolt(Element.LIGHT, 18, 1.4, 10f), FruitMoves.burst(Element.LIGHT, 6, 12f), FruitMoves.eruption(Element.LIGHT, 20, 5, 16f, 1.0),
			FruitMoves.eruption(Element.LIGHT, 26, 7.5, 24f, 0.8), FruitMoves.updraft(Element.LIGHT),
			"nova_bolt", "nova_supernova", "nova_comet", "nova_starfall", "nova_ascent");

	// --- Mythical ---
	public static final FruitDef LEVIATHAN = register("leviathan", FruitRarity.MYTHICAL, Element.WATER, 0x1F5FA8,
			FruitMoves.strike(Element.WATER, 10, 11f), FruitMoves.bolt(Element.WATER, 22, 2.2, 13f), FruitMoves.vortex(Element.WATER, 18, 6, 16f),
			FruitMoves.eruption(Element.WATER, 26, 8, 26f, 1.1), FruitMoves.dash(Element.WATER, 2.6, 6f),
			"leviathan_bite", "leviathan_torrent", "leviathan_whirlpool", "leviathan_wrath", "leviathan_dive");
	public static final FruitDef AEGIS = register("aegis", FruitRarity.MYTHICAL, Element.BARRIER, 0xF5E08A,
			FruitMoves.strike(Element.BARRIER, 8, 10f), FruitMoves.heal(Element.BARRIER, 12f, 7), FruitMoves.burst(Element.BARRIER, 6, 14f),
			FruitMoves.eruption(Element.BARRIER, 22, 7, 22f, 0.9), FruitMoves.barrier(Element.BARRIER, 8, 3),
			"aegis_bash", "aegis_sanctuary", "aegis_repulse", "aegis_judgment", "aegis_wall");

	// --- Third wave: 18 more original fruits (42 total) ---
	public static final FruitDef PEBBLE = register("pebble", FruitRarity.COMMON, Element.EARTH, 0x8A8178,
			FruitMoves.bolt(Element.EARTH, 12, 1.3, 6f), FruitMoves.burst(Element.EARTH, 4.5, 7f), FruitMoves.eruption(Element.EARTH, 14, 4, 10f, 0.7),
			FruitMoves.eruption(Element.EARTH, 16, 5.5, 12f, 0.9), FruitMoves.leap(Element.EARTH, 3, 4f),
			"pebble_toss", "pebble_scatter", "pebble_spire", "pebble_landslide", "pebble_hop");
	public static final FruitDef FEATHER = register("feather", FruitRarity.COMMON, Element.WIND, 0xF2F2EA,
			FruitMoves.bolt(Element.WIND, 14, 1.2, 5f), FruitMoves.burst(Element.WIND, 5, 6f), FruitMoves.bolt(Element.WIND, 18, 2.0, 9f),
			FruitMoves.vortex(Element.WIND, 14, 5, 11f), FruitMoves.updraft(Element.WIND),
			"feather_dart", "feather_flurry", "feather_gust", "feather_tornado", "feather_glide");
	public static final FruitDef ASH = register("ash", FruitRarity.COMMON, Element.MIST, 0x5E5A57,
			FruitMoves.bolt(Element.FIRE, 12, 1.3, 6f), FruitMoves.burst(Element.MIST, 5, 6f), FruitMoves.lingering(Element.FIRE, 14, 3.5, 5f, 3f, 5),
			FruitMoves.eruption(Element.FIRE, 15, 5, 12f, 0.8), FruitMoves.blink(Element.MIST, 8),
			"ash_cinder", "ash_cloud", "ash_embers", "ash_plume", "ash_drift");

	public static final FruitDef SLUDGE = register("sludge", FruitRarity.UNCOMMON, Element.EARTH, 0x6B5A3A,
			FruitMoves.bolt(Element.POISON, 12, 1.5, 7f), FruitMoves.lingering(Element.EARTH, 14, 4, 6f, 3f, 6), FruitMoves.burst(Element.EARTH, 5, 9f),
			FruitMoves.vortex(Element.EARTH, 16, 5, 14f), FruitMoves.dash(Element.EARTH, 1.8, 4f),
			"sludge_glob", "sludge_bog", "sludge_splash", "sludge_swamp", "sludge_slide");
	public static final FruitDef GLASS = register("glass", FruitRarity.UNCOMMON, Element.CRYSTAL, 0xBFE8F2,
			FruitMoves.bolt(Element.CRYSTAL, 14, 1.1, 8f), FruitMoves.burst(Element.CRYSTAL, 5, 8f), FruitMoves.bolt(Element.CRYSTAL, 20, 1.8, 11f),
			FruitMoves.eruption(Element.CRYSTAL, 16, 5, 15f, 0.6), FruitMoves.barrier(Element.CRYSTAL, 5, 1),
			"glass_shard", "glass_shatter", "glass_lance", "glass_storm", "glass_pane");
	public static final FruitDef RUST = register("rust", FruitRarity.UNCOMMON, Element.METAL, 0xA8582E,
			FruitMoves.strike(Element.METAL, 6, 9f), FruitMoves.lingering(Element.POISON, 12, 3.5, 6f, 3f, 5), FruitMoves.burst(Element.METAL, 5, 10f),
			FruitMoves.eruption(Element.METAL, 14, 5, 15f, 0.7), FruitMoves.barrier(Element.METAL, 5, 1),
			"rust_claw", "rust_corrode", "rust_burst", "rust_collapse", "rust_plating");
	public static final FruitDef POLLEN = register("pollen", FruitRarity.UNCOMMON, Element.PLANT, 0xF2D64B,
			FruitMoves.bolt(Element.SOUND, 12, 1.6, 6f), FruitMoves.burst(Element.PLANT, 5.5, 8f), FruitMoves.heal(Element.PLANT, 6f, 5),
			FruitMoves.lingering(Element.PLANT, 16, 5, 10f, 4f, 6), FruitMoves.dash(Element.PLANT, 1.8, 3f),
			"pollen_puff", "pollen_burst", "pollen_nectar", "pollen_meadow", "pollen_breeze");
	public static final FruitDef INK = register("ink", FruitRarity.UNCOMMON, Element.SHADOW, 0x22223A,
			FruitMoves.bolt(Element.SHADOW, 14, 1.4, 7f), FruitMoves.burst(Element.MIST, 5, 7f), FruitMoves.lingering(Element.SHADOW, 14, 4, 7f, 3f, 5),
			FruitMoves.vortex(Element.SHADOW, 16, 5, 13f), FruitMoves.blink(Element.SHADOW, 9),
			"ink_splash", "ink_cloud", "ink_pool", "ink_whirl", "ink_slip");

	public static final FruitDef MONSOON = register("monsoon", FruitRarity.RARE, Element.WATER, 0x4A6FA0,
			FruitMoves.bolt(Element.WATER, 16, 1.5, 8f), FruitMoves.burst(Element.LIGHTNING, 5.5, 10f), FruitMoves.lingering(Element.WATER, 16, 5, 9f, 4f, 6),
			FruitMoves.eruption(Element.LIGHTNING, 20, 6, 17f, 0.6), FruitMoves.dash(Element.WATER, 2.2, 5f),
			"monsoon_rain", "monsoon_thunder", "monsoon_flood", "monsoon_tempest", "monsoon_current");
	public static final FruitDef MIRAGE = register("mirage", FruitRarity.RARE, Element.LIGHT, 0xF5C27A,
			FruitMoves.bolt(Element.LIGHT, 18, 1.2, 8f), FruitMoves.burst(Element.SOUND, 5.5, 9f), FruitMoves.lingering(Element.MIST, 16, 5, 9f, 4f, 6),
			FruitMoves.eruption(Element.LIGHT, 20, 5, 16f, 0.6), FruitMoves.blink(Element.LIGHT, 12),
			"mirage_glint", "mirage_dazzle", "mirage_haze", "mirage_oasis", "mirage_shift");
	public static final FruitDef TREMOR = register("tremor", FruitRarity.RARE, Element.EARTH, 0x7A6248,
			FruitMoves.strike(Element.EARTH, 8, 9f), FruitMoves.burst(Element.EARTH, 6, 11f), FruitMoves.eruption(Element.EARTH, 18, 5, 14f, 1.0),
			FruitMoves.eruption(Element.EARTH, 22, 7, 19f, 1.2), FruitMoves.leap(Element.EARTH, 4, 6f),
			"tremor_jolt", "tremor_shockwave", "tremor_fissure", "tremor_rupture", "tremor_vault");
	public static final FruitDef HORNET = register("hornet", FruitRarity.RARE, Element.POISON, 0xE0B020,
			FruitMoves.strike(Element.POISON, 8, 9f), FruitMoves.dash(Element.POISON, 2.2, 9f), FruitMoves.burst(Element.POISON, 5, 11f),
			FruitMoves.vortex(Element.POISON, 18, 5.5, 15f), FruitMoves.updraft(Element.BEAST),
			"hornet_sting", "hornet_dive", "hornet_swarm", "hornet_hive", "hornet_flight");

	public static final FruitDef GRIZZLY = register("grizzly", FruitRarity.LEGENDARY, Element.BEAST, 0x6E4A2E,
			FruitMoves.strike(Element.BEAST, 5, 11f), FruitMoves.burst(Element.EARTH, 5, 12f), FruitMoves.dash(Element.BEAST, 2.0, 13f),
			FruitMoves.leap(Element.BEAST, 6, 19f), FruitMoves.barrier(Element.BEAST, 6, 2),
			"grizzly_maul", "grizzly_roar", "grizzly_charge", "grizzly_frenzy", "grizzly_hide");
	public static final FruitDef TEMPEST = register("tempest", FruitRarity.LEGENDARY, Element.LIGHTNING, 0x5AA0D6,
			FruitMoves.bolt(Element.WIND, 18, 1.6, 10f), FruitMoves.burst(Element.LIGHTNING, 6, 12f), FruitMoves.vortex(Element.WIND, 20, 6, 15f),
			FruitMoves.lingering(Element.LIGHTNING, 22, 6.5, 18f, 6f, 6), FruitMoves.updraft(Element.WIND),
			"tempest_squall", "tempest_crack", "tempest_cyclone", "tempest_superstorm", "tempest_skyride");
	public static final FruitDef SOLAR = register("solar", FruitRarity.LEGENDARY, Element.FIRE, 0xFFB52E,
			FruitMoves.bolt(Element.LIGHT, 20, 1.3, 10f), FruitMoves.burst(Element.FIRE, 6, 13f), FruitMoves.eruption(Element.FIRE, 20, 5, 17f, 1.0),
			FruitMoves.eruption(Element.LIGHT, 26, 7.5, 24f, 0.7), FruitMoves.dash(Element.FIRE, 2.4, 6f),
			"solar_ray", "solar_flare", "solar_corona", "solar_sunburst", "solar_streak");

	public static final FruitDef HYDRA = register("hydra", FruitRarity.MYTHICAL, Element.POISON, 0x2E8A6A,
			FruitMoves.strike(Element.POISON, 10, 12f), FruitMoves.bolt(Element.WATER, 22, 2.0, 13f), FruitMoves.lingering(Element.POISON, 18, 6, 14f, 6f, 7),
			FruitMoves.vortex(Element.WATER, 24, 7.5, 25f), FruitMoves.dash(Element.WATER, 2.6, 7f),
			"hydra_bite", "hydra_spray", "hydra_mire", "hydra_maelstrom", "hydra_lunge");
	public static final FruitDef TITAN = register("titan", FruitRarity.MYTHICAL, Element.EARTH, 0x8F7F6A,
			FruitMoves.strike(Element.EARTH, 7, 14f), FruitMoves.burst(Element.EARTH, 6.5, 15f), FruitMoves.eruption(Element.EARTH, 20, 6, 20f, 1.2),
			FruitMoves.eruption(Element.EARTH, 26, 9, 28f, 1.0), FruitMoves.barrier(Element.EARTH, 8, 3),
			"titan_fist", "titan_stomp", "titan_pillar", "titan_cataclysm", "titan_bulwark");
	public static final FruitDef SERAPH = register("seraph", FruitRarity.MYTHICAL, Element.LIGHT, 0xFFF5D6,
			FruitMoves.bolt(Element.LIGHT, 22, 1.4, 12f), FruitMoves.heal(Element.BARRIER, 14f, 8), FruitMoves.burst(Element.LIGHT, 6.5, 15f),
			FruitMoves.eruption(Element.LIGHT, 26, 8, 26f, 0.8), FruitMoves.updraft(Element.LIGHT),
			"seraph_lance", "seraph_grace", "seraph_halo", "seraph_heavenfall", "seraph_wings");

	private Fruits() {
	}

	private static FruitDef register(String name, FruitRarity rarity, Element element, int color, AbilityAction z, AbilityAction x,
			AbilityAction c, AbilityAction v, AbilityAction r, String zKey, String xKey, String cKey, String vKey, String rKey) {
		Power power = Powers.register(new Power(GrandLineMod.id(name), PowerSource.FRUIT, StatType.FRUIT,
				rarity.name().toLowerCase(java.util.Locale.ROOT), color, List.of(
				new AbilityDef(zKey, 0, 50, 0.08, z),
				new AbilityDef(xKey, 10, 120, 0.14, x),
				new AbilityDef(cKey, 25, 180, 0.2, c),
				new AbilityDef(vKey, 50, 300, 0.3, v),
				new AbilityDef(rKey, 15, 120, 0.1, r))));
		return registerItem(name, rarity, power);
	}

	/** Registers a fruit with a hand-made moveset (Z, X, C, V, R order). */
	private static FruitDef register(String name, FruitRarity rarity, int color, List<AbilityDef> moves) {
		Power power = Powers.register(new Power(GrandLineMod.id(name), PowerSource.FRUIT, StatType.FRUIT,
				rarity.name().toLowerCase(java.util.Locale.ROOT), color, moves));
		return registerItem(name, rarity, power);
	}

	private static FruitDef registerItem(String name, FruitRarity rarity, Power power) {
		Item.Properties properties = new Item.Properties().stacksTo(1).fireResistant().rarity(switch (rarity) {
			case COMMON -> Rarity.COMMON;
			case UNCOMMON -> Rarity.UNCOMMON;
			case RARE -> Rarity.RARE;
			case LEGENDARY, MYTHICAL -> Rarity.EPIC;
		});
		FruitItem item = Registry.register(BuiltInRegistries.ITEM, GrandLineMod.id(name + "_fruit"), new FruitItem(power.id(), properties));
		FruitDef def = new FruitDef(power, rarity, item);
		FRUITS.put(power.id(), def);
		return def;
	}

	public static FruitDef get(String id) {
		ResourceLocation location = ResourceLocation.tryParse(id);
		return location == null ? null : FRUITS.get(location);
	}

	public static List<FruitDef> all() {
		return Collections.unmodifiableList(new ArrayList<>(FRUITS.values()));
	}

	public static void register() {
		GrandLineMod.LOGGER.debug("Registered {} fruits", FRUITS.size());
	}
}
