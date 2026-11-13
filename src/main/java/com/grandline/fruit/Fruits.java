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
 * Moves unlock at mastery 0 (Z), 10 (X), 15 (R), 25 (C) and 50 (V), except Flame (see FlameMoves).
 * The active roster has one fruit per approved icon; old fruits stay registered as legacy so saves keep working.
 */
public final class Fruits {
	public record FruitDef(Power power, FruitRarity rarity, FruitItem item) {
		public String id() {
			return power.id().toString();
		}
	}

	private static final Map<ResourceLocation, FruitDef> FRUITS = new LinkedHashMap<>();
	/** Old fruits kept so existing saves stay valid; hidden from every drop and shop pool (see {@link #all()}). */
	private static final java.util.Set<ResourceLocation> LEGACY = new java.util.HashSet<>();

	// ===== Active roster: one fruit per approved icon. Ids of carried-over fruits are kept for saves. =====

	public static final FruitDef ROCKET = register("rocket", FruitRarity.COMMON, Element.EXPLOSION, 0xD94A3A,
			FruitMoves.bolt(Element.EXPLOSION, 16, 1.2, 6f),
			FruitMoves.burst(Element.EXPLOSION, 4.5, 7f),
			FruitMoves.eruption(Element.EXPLOSION, 16, 4, 10f, 0.9),
			FruitMoves.lingering(Element.EXPLOSION, 16, 4, 8f, 3f, 4),
			FruitMoves.dash(Element.EXPLOSION, 2.4, 4f),
			"rocket_missile", "rocket_thruster", "rocket_warhead", "rocket_barrage", "rocket_afterburner");

	/** Spin (id "gale" kept so saves carry over). */
	public static final FruitDef SPIN = register("gale", FruitRarity.COMMON, Element.WIND, 0x9BD13A,
			FruitMoves.bolt(Element.WIND, 14, 1.4, 6f),
			FruitMoves.burst(Element.WIND, 5, 7f),
			FruitMoves.bolt(Element.WIND, 20, 2, 9f),
			FruitMoves.vortex(Element.WIND, 16, 5, 13f),
			FruitMoves.updraft(Element.WIND),
			"spin_cutter", "spin_top", "spin_lance", "spin_drill", "spin_rotor");

	public static final FruitDef BLADE = register("blade", FruitRarity.COMMON, Element.METAL, 0x8C9BB5,
			FruitMoves.bolt(Element.METAL, 12, 1.6, 7f),
			FruitMoves.burst(Element.METAL, 4.5, 8f),
			FruitMoves.dash(Element.METAL, 2, 9f),
			FruitMoves.lingering(Element.METAL, 14, 4, 9f, 3f, 4),
			FruitMoves.blink(Element.METAL, 9),
			"blade_flick", "blade_fan", "blade_sever", "blade_thousand", "blade_step");

	public static final FruitDef SPRING = register("spring", FruitRarity.COMMON, Element.BOUNCE, 0xB8BCC8,
			FruitMoves.strike(Element.BOUNCE, 12, 7f),
			FruitMoves.burst(Element.BOUNCE, 4.5, 7f),
			FruitMoves.leap(Element.BOUNCE, 4.5, 9f),
			FruitMoves.eruption(Element.BOUNCE, 16, 5, 13f, 1.2),
			FruitMoves.updraft(Element.BOUNCE),
			"spring_jab", "spring_recoil", "spring_pogo", "spring_overwound", "spring_hop");

	/** Bomb (id "blast" kept so saves carry over). */
	public static final FruitDef BOMB = register("blast", FruitRarity.COMMON, Element.EXPLOSION, 0x5A3A3A,
			FruitMoves.bolt(Element.EXPLOSION, 14, 1.4, 7f),
			FruitMoves.burst(Element.EXPLOSION, 5, 9f),
			FruitMoves.lingering(Element.EXPLOSION, 14, 3.5, 6f, 3f, 5),
			FruitMoves.eruption(Element.EXPLOSION, 18, 6, 14f, 1),
			FruitMoves.leap(Element.EXPLOSION, 3.5, 5f),
			"bomb_fuse", "bomb_ring", "bomb_mines", "bomb_charge", "bomb_jump");

	/** Smoke (id "mist" kept so saves carry over). */
	public static final FruitDef SMOKE = register("mist", FruitRarity.COMMON, Element.MIST, 0xBDBDBD,
			FruitMoves.bolt(Element.MIST, 14, 1.6, 5f),
			FruitMoves.burst(Element.MIST, 5.5, 6f),
			FruitMoves.lingering(Element.MIST, 14, 4.5, 5f, 3f, 5),
			FruitMoves.vortex(Element.MIST, 16, 5.5, 12f),
			FruitMoves.blink(Element.MIST, 10),
			"smoke_puff", "smoke_ring", "smoke_haze", "smoke_storm", "smoke_drift");

	public static final FruitDef SPIKE = register("spike", FruitRarity.COMMON, Element.METAL, 0x9A9AA6,
			FruitMoves.bolt(Element.METAL, 16, 1, 7f),
			FruitMoves.burst(Element.METAL, 4.5, 8f),
			FruitMoves.lingering(Element.METAL, 14, 4, 6f, 3f, 5),
			FruitMoves.eruption(Element.METAL, 16, 5, 13f, 1.1),
			FruitMoves.dash(Element.METAL, 2, 5f),
			"spike_needle", "spike_urchin", "spike_bed", "spike_maiden", "spike_quill");

	/** Flame (id "ember" kept for saves): hand-made moveset, see FlameMoves. */
	public static final FruitDef FLAME = register("ember", FruitRarity.UNCOMMON, 0xFF7A33, com.grandline.fruit.flame.FlameMoves.moves());

	/** Ice (id "frost" kept so saves carry over). */
	public static final FruitDef ICE = register("frost", FruitRarity.UNCOMMON, Element.ICE, 0x8FD0FF,
			FruitMoves.bolt(Element.ICE, 16, 1.2, 8f),
			FruitMoves.burst(Element.ICE, 5.5, 9f),
			FruitMoves.bolt(Element.ICE, 22, 1.8, 11f),
			FruitMoves.lingering(Element.ICE, 16, 5, 9f, 4.5f, 6),
			FruitMoves.dash(Element.ICE, 2.2, 4.5f),
			"ice_shard", "ice_nova", "ice_spear", "ice_permafrost", "ice_slide");

	/** Sand (id "dune" kept so saves carry over). */
	public static final FruitDef SAND = register("dune", FruitRarity.UNCOMMON, Element.SAND, 0xE0B866,
			FruitMoves.bolt(Element.SAND, 14, 1.4, 8f),
			FruitMoves.burst(Element.SAND, 5, 10f),
			FruitMoves.lingering(Element.SAND, 14, 3.5, 6.5f, 3.5f, 5),
			FruitMoves.eruption(Element.SAND, 16, 5.5, 18f, 0.8),
			FruitMoves.dash(Element.SAND, 1.8, 5.5f),
			"sand_grit", "sand_devil", "sand_sinkhole", "sand_collapse", "sand_slide");

	/** Dark (id "umbra" kept so saves carry over). */
	public static final FruitDef DARK = register("umbra", FruitRarity.UNCOMMON, Element.SHADOW, 0x3A2A55,
			FruitMoves.bolt(Element.SHADOW, 16, 1.3, 8f),
			FruitMoves.burst(Element.SHADOW, 5, 9f),
			FruitMoves.vortex(Element.SHADOW, 16, 5, 13.5f),
			FruitMoves.lingering(Element.SHADOW, 16, 5, 9f, 4.5f, 6),
			FruitMoves.blink(Element.SHADOW, 10),
			"dark_bolt", "dark_eclipse", "dark_pull", "dark_field", "dark_step");

	/** Diamond (id "quartz" kept so saves carry over). */
	public static final FruitDef DIAMOND = register("quartz", FruitRarity.UNCOMMON, Element.CRYSTAL, 0x6FD8F2,
			FruitMoves.bolt(Element.CRYSTAL, 16, 1.2, 9f),
			FruitMoves.burst(Element.CRYSTAL, 5, 9f),
			FruitMoves.barrier(Element.CRYSTAL, 8, 2),
			FruitMoves.eruption(Element.CRYSTAL, 16, 5, 17f, 0.9),
			FruitMoves.dash(Element.CRYSTAL, 2, 5.5f),
			"diamond_shard", "diamond_facet", "diamond_skin", "diamond_crown", "diamond_dash");

	/** Eagle (id "feather" kept so saves carry over). */
	public static final FruitDef EAGLE = register("feather", FruitRarity.UNCOMMON, Element.WIND, 0xC9A27A,
			FruitMoves.strike(Element.WIND, 14, 9f),
			FruitMoves.burst(Element.WIND, 5, 8f),
			FruitMoves.bolt(Element.WIND, 20, 2, 10f),
			FruitMoves.eruption(Element.WIND, 18, 5, 17f, 1),
			FruitMoves.flight(Element.WIND, 200, 0.07f),
			"eagle_talon", "eagle_gust", "eagle_volley", "eagle_strike", "eagle_soar", true);

	/** Light (id "nova" kept so saves carry over). */
	public static final FruitDef LIGHT = register("nova", FruitRarity.RARE, Element.LIGHT, 0xFFE36A,
			FruitMoves.bolt(Element.LIGHT, 24, 1, 10f),
			FruitMoves.burst(Element.LIGHT, 5.5, 10f),
			FruitMoves.bolt(Element.LIGHT, 26, 1.6, 14f),
			FruitMoves.lingering(Element.LIGHT, 18, 5, 11f, 5f, 5),
			FruitMoves.flight(Element.LIGHT, 160, 0.09f),
			"light_ray", "light_flash", "light_halo", "light_rain", "light_glide", true);

	/** Rubber (id "bounce" kept so saves carry over). */
	public static final FruitDef RUBBER = register("bounce", FruitRarity.RARE, Element.BOUNCE, 0xF2649A,
			FruitMoves.strike(Element.BOUNCE, 14, 10f),
			FruitMoves.burst(Element.BOUNCE, 5, 10f),
			FruitMoves.bolt(Element.BOUNCE, 14, 2, 14f),
			FruitMoves.eruption(Element.BOUNCE, 16, 5, 17.5f, 1.2),
			FruitMoves.dash(Element.BOUNCE, 2.6, 5f),
			"rubber_jab", "rubber_whip", "rubber_flurry", "rubber_cannon", "rubber_sling");

	public static final FruitDef GHOST = register("ghost", FruitRarity.RARE, Element.SOUL, 0xBFF2E6,
			FruitMoves.bolt(Element.SOUL, 16, 1.2, 9f),
			FruitMoves.burst(Element.SOUL, 5.5, 10f),
			FruitMoves.lingering(Element.SOUL, 15, 4.5, 9f, 5f, 5),
			FruitMoves.vortex(Element.SOUL, 16, 5, 17.5f),
			FruitMoves.blink(Element.SOUL, 12),
			"ghost_wisp", "ghost_wail", "ghost_chill", "ghost_maw", "ghost_phase");

	/** Magma (id "lava" kept so saves carry over). */
	public static final FruitDef MAGMA = register("lava", FruitRarity.RARE, Element.LAVA, 0xFF5A1F,
			FruitMoves.bolt(Element.LAVA, 14, 1.4, 11f),
			FruitMoves.lingering(Element.LAVA, 14, 3.5, 9f, 6f, 6),
			FruitMoves.eruption(Element.LAVA, 16, 4.5, 19f, 1.3),
			FruitMoves.eruption(Element.LAVA, 22, 7, 25f, 0.9),
			FruitMoves.dash(Element.LAVA, 2, 9f),
			"magma_shot", "magma_pool", "magma_geyser", "magma_meteor", "magma_surge");

	/** Quake (id "tremor" kept so saves carry over). */
	public static final FruitDef QUAKE = register("tremor", FruitRarity.RARE, Element.EARTH, 0xDDE8EE,
			FruitMoves.strike(Element.EARTH, 12, 11f),
			FruitMoves.burst(Element.EARTH, 6, 11f),
			FruitMoves.bolt(Element.EARTH, 20, 2.2, 14f),
			FruitMoves.eruption(Element.EARTH, 18, 6.5, 22.5f, 1.1),
			FruitMoves.leap(Element.EARTH, 4, 7.5f),
			"quake_punch", "quake_ring", "quake_fault", "quake_slam", "quake_leap");

	public static final FruitDef SPIDER = register("spider", FruitRarity.RARE, Element.WEB, 0x8E5BB0,
			FruitMoves.bolt(Element.WEB, 18, 1.2, 9f),
			FruitMoves.lingering(Element.WEB, 15, 4, 7.5f, 4f, 5),
			FruitMoves.strike(Element.WEB, 12, 14f),
			FruitMoves.vortex(Element.WEB, 16, 5, 17.5f),
			FruitMoves.dash(Element.WEB, 2.4, 5f),
			"spider_shot", "spider_snare", "spider_fang", "spider_brood", "spider_swing");

	public static final FruitDef BUDDHA = register("buddha", FruitRarity.LEGENDARY, Element.BARRIER, 0xF5C24B,
			FruitMoves.strike(Element.BARRIER, 12, 13f),
			FruitMoves.burst(Element.BARRIER, 6, 14.5f),
			FruitMoves.barrier(Element.BARRIER, 8, 3),
			FruitMoves.eruption(Element.BARRIER, 20, 7, 26f, 1),
			FruitMoves.leap(Element.BARRIER, 4.5, 8.5f),
			"buddha_palm", "buddha_lotus", "buddha_aura", "buddha_colossus", "buddha_leap");

	public static final FruitDef LOVE = register("love", FruitRarity.LEGENDARY, Element.HEART, 0xF23A7A,
			FruitMoves.bolt(Element.HEART, 20, 1.2, 11.5f),
			FruitMoves.burst(Element.HEART, 5.5, 13f),
			FruitMoves.heal(Element.HEART, 14.5f, 8),
			FruitMoves.lingering(Element.HEART, 16, 5, 13f, 6f, 5),
			FruitMoves.dash(Element.HEART, 2.4, 6f),
			"love_arrow", "love_crush", "love_mend", "love_rain", "love_dash");

	public static final FruitDef CREATION = register("creation", FruitRarity.LEGENDARY, Element.CONTROL, 0x3FD6B8,
			FruitMoves.bolt(Element.CONTROL, 18, 1.4, 11.5f),
			FruitMoves.burst(Element.CONTROL, 5.5, 13f),
			FruitMoves.barrier(Element.CONTROL, 8, 3),
			FruitMoves.lingering(Element.CONTROL, 16, 5, 13f, 6f, 5),
			FruitMoves.blink(Element.CONTROL, 12),
			"creation_bolt", "creation_ring", "creation_bastion", "creation_forge", "creation_bridge");

	/** Sound (id "echo" kept so saves carry over). */
	public static final FruitDef SOUND = register("echo", FruitRarity.LEGENDARY, Element.SOUND, 0x8E3BD6,
			FruitMoves.bolt(Element.SOUND, 20, 1.2, 11.5f),
			FruitMoves.burst(Element.SOUND, 6, 14.5f),
			FruitMoves.lingering(Element.SOUND, 16, 5, 11.5f, 6f, 6),
			FruitMoves.eruption(Element.SOUND, 20, 6.5, 26f, 0.8),
			FruitMoves.blink(Element.SOUND, 11),
			"sound_note", "sound_bass", "sound_resonance", "sound_finale", "sound_tempo");

	public static final FruitDef PHOENIX = register("phoenix", FruitRarity.LEGENDARY, Element.FOXFIRE, 0x46C8F0,
			FruitMoves.strike(Element.FOXFIRE, 14, 13f),
			FruitMoves.heal(Element.FOXFIRE, 17.4f, 8),
			FruitMoves.burst(Element.FOXFIRE, 5.5, 14.5f),
			FruitMoves.eruption(Element.FOXFIRE, 20, 6, 26f, 1),
			FruitMoves.flight(Element.FOXFIRE, 240, 0.08f),
			"phoenix_talon", "phoenix_rebirth", "phoenix_wing", "phoenix_dive", "phoenix_glide", true);

	public static final FruitDef PORTAL = register("portal", FruitRarity.LEGENDARY, Element.PORTAL, 0x2A6FE0,
			FruitMoves.bolt(Element.PORTAL, 20, 1.2, 11.5f),
			FruitMoves.vortex(Element.PORTAL, 16, 5, 17.5f),
			FruitMoves.lingering(Element.PORTAL, 15, 4, 10f, 6f, 5),
			FruitMoves.eruption(Element.PORTAL, 20, 6, 26f, 0.8),
			FruitMoves.blink(Element.PORTAL, 16),
			"portal_rift", "portal_collapse", "portal_mine", "portal_tear", "portal_jump");

	/** Lightning (id "volt" kept so saves carry over). */
	public static final FruitDef LIGHTNING = register("volt", FruitRarity.LEGENDARY, Element.LIGHTNING, 0x4FB4FF,
			FruitMoves.bolt(Element.LIGHTNING, 18, 1.2, 13f),
			FruitMoves.burst(Element.LIGHTNING, 5.5, 16f),
			FruitMoves.eruption(Element.LIGHTNING, 24, 4, 26f, 0.4),
			FruitMoves.lingering(Element.LIGHTNING, 18, 5, 14.5f, 7f, 6),
			FruitMoves.blink(Element.LIGHTNING, 10),
			"lightning_arc", "lightning_clap", "lightning_sky", "lightning_cell", "lightning_flash");

	public static final FruitDef PAIN = register("pain", FruitRarity.LEGENDARY, Element.PAIN, 0xF5A3C0,
			FruitMoves.bolt(Element.PAIN, 18, 1.2, 11.5f),
			FruitMoves.burst(Element.PAIN, 5.5, 14.5f),
			FruitMoves.strike(Element.PAIN, 10, 17.5f),
			FruitMoves.lingering(Element.PAIN, 15, 4.5, 11.5f, 6f, 5),
			FruitMoves.leap(Element.PAIN, 4, 8.5f),
			"pain_throb", "pain_pulse", "pain_repel", "pain_field", "pain_recoil");

	public static final FruitDef BLIZZARD = register("blizzard", FruitRarity.LEGENDARY, Element.ICE, 0x9FD8FF,
			FruitMoves.bolt(Element.ICE, 18, 1.6, 11.5f),
			FruitMoves.lingering(Element.ICE, 16, 5.5, 10f, 6f, 6),
			FruitMoves.burst(Element.ICE, 6, 14.5f),
			FruitMoves.eruption(Element.ICE, 20, 6.5, 26f, 0.9),
			FruitMoves.flight(Element.ICE, 220, 0.07f),
			"blizzard_volley", "blizzard_whiteout", "blizzard_gale", "blizzard_avalanche", "blizzard_cloud", true);

	public static final FruitDef MAGNET = register("magnet", FruitRarity.LEGENDARY, Element.MAGNET, 0xE0403A,
			FruitMoves.strike(Element.MAGNET, 16, 13f),
			FruitMoves.vortex(Element.MAGNET, 14, 5.5, 16f),
			FruitMoves.burst(Element.MAGNET, 6, 14.5f),
			FruitMoves.lingering(Element.MAGNET, 16, 5, 13f, 6f, 5),
			FruitMoves.dash(Element.MAGNET, 2.6, 8.5f),
			"magnet_polarity", "magnet_attract", "magnet_repulse", "magnet_scrap", "magnet_rail");

	public static final FruitDef GAS = register("gas", FruitRarity.LEGENDARY, Element.POISON, 0x7ED957,
			FruitMoves.bolt(Element.POISON, 18, 1.6, 11.5f),
			FruitMoves.lingering(Element.POISON, 16, 5.5, 10f, 6f, 6),
			FruitMoves.burst(Element.POISON, 6, 14.5f),
			FruitMoves.vortex(Element.POISON, 18, 6, 23f),
			FruitMoves.blink(Element.POISON, 12),
			"gas_fume", "gas_cloud", "gas_burst", "gas_giant", "gas_drift");

	/** Gravity (id "orbit" kept so saves carry over). */
	public static final FruitDef GRAVITY = register("orbit", FruitRarity.MYTHICAL, Element.GRAVITY, 0x7A4FB8,
			FruitMoves.bolt(Element.GRAVITY, 18, 1.4, 15f),
			FruitMoves.burst(Element.GRAVITY, 6, 18f),
			FruitMoves.vortex(Element.GRAVITY, 18, 6, 26.5f),
			FruitMoves.eruption(Element.GRAVITY, 24, 7, 36.5f, 0.5),
			FruitMoves.updraft(Element.GRAVITY),
			"gravity_pull", "gravity_crush", "gravity_singularity", "gravity_meteor", "gravity_weightless");

	public static final FruitDef MAMMOTH = register("mammoth", FruitRarity.MYTHICAL, Element.BEAST, 0x8A5A3A,
			FruitMoves.strike(Element.BEAST, 10, 18f),
			FruitMoves.burst(Element.BEAST, 6, 16.5f),
			FruitMoves.dash(Element.BEAST, 2.6, 20f),
			FruitMoves.eruption(Element.BEAST, 16, 7, 33f, 1.2),
			FruitMoves.leap(Element.BEAST, 4.5, 11.5f),
			"mammoth_tusk", "mammoth_roar", "mammoth_stampede", "mammoth_stomp", "mammoth_charge");

	public static final FruitDef T_REX = register("t_rex", FruitRarity.MYTHICAL, Element.BEAST, 0x5FA83A,
			FruitMoves.strike(Element.BEAST, 10, 20f),
			FruitMoves.burst(Element.BEAST, 5.5, 18f),
			FruitMoves.lingering(Element.BEAST, 14, 5.5, 13f, 6.5f, 5),
			FruitMoves.eruption(Element.BEAST, 16, 7, 34.5f, 1.1),
			FruitMoves.leap(Element.BEAST, 4.5, 11.5f),
			"trex_snap", "trex_tail", "trex_roar", "trex_stomp", "trex_pounce");

	public static final FruitDef DOUGH = register("dough", FruitRarity.MYTHICAL, Element.BOUNCE, 0xF2D9B8,
			FruitMoves.strike(Element.BOUNCE, 14, 16.5f),
			FruitMoves.burst(Element.BOUNCE, 6, 16.5f),
			FruitMoves.lingering(Element.BOUNCE, 15, 4.5, 11.5f, 6.5f, 6),
			FruitMoves.eruption(Element.BOUNCE, 20, 6.5, 33f, 1),
			FruitMoves.dash(Element.BOUNCE, 2.6, 8f),
			"dough_fist", "dough_knead", "dough_trap", "dough_oven", "dough_swing");

	/** Shadow (id "ink" kept so saves carry over). */
	public static final FruitDef SHADOW = register("ink", FruitRarity.MYTHICAL, Element.SHADOW, 0x4A2A7A,
			FruitMoves.bolt(Element.SHADOW, 20, 1.3, 16.5f),
			FruitMoves.vortex(Element.SHADOW, 16, 5.5, 21.5f),
			FruitMoves.lingering(Element.SHADOW, 16, 5, 15f, 8f, 6),
			FruitMoves.eruption(Element.SHADOW, 20, 7, 36.5f, 0.8),
			FruitMoves.blink(Element.SHADOW, 12),
			"shadow_spear", "shadow_grasp", "shadow_pool", "shadow_crash", "shadow_slip");

	/** Venom (id "toxin" kept so saves carry over). */
	public static final FruitDef VENOM = register("toxin", FruitRarity.MYTHICAL, Element.POISON, 0x9A2BD6,
			FruitMoves.bolt(Element.POISON, 18, 1.4, 15f),
			FruitMoves.burst(Element.POISON, 6, 16.5f),
			FruitMoves.lingering(Element.POISON, 16, 5.5, 13f, 8f, 6),
			FruitMoves.strike(Element.POISON, 12, 23f),
			FruitMoves.dash(Element.POISON, 2.4, 10f),
			"venom_spit", "venom_acid", "venom_pool", "venom_bite", "venom_slither");

	public static final FruitDef SPIRIT = register("spirit", FruitRarity.MYTHICAL, Element.SPIRIT, 0xF26A3A,
			FruitMoves.bolt(Element.SPIRIT, 20, 1.4, 16.5f),
			FruitMoves.burst(Element.SPIRIT, 6, 18f),
			FruitMoves.heal(Element.SPIRIT, 19.8f, 8),
			FruitMoves.eruption(Element.SPIRIT, 22, 7, 36.5f, 0.9),
			FruitMoves.flight(Element.SPIRIT, 220, 0.08f),
			"spirit_twin", "spirit_ring", "spirit_lantern", "spirit_judgement", "spirit_walk", true);

	public static final FruitDef TIGER = register("tiger", FruitRarity.MYTHICAL, Element.BEAST, 0xF29A3A,
			FruitMoves.strike(Element.BEAST, 10, 18f),
			FruitMoves.dash(Element.BEAST, 2.6, 18f),
			FruitMoves.burst(Element.BEAST, 6, 16.5f),
			FruitMoves.lingering(Element.BEAST, 14, 5, 15f, 8f, 5),
			FruitMoves.blink(Element.BEAST, 11),
			"tiger_claw", "tiger_pounce", "tiger_roar", "tiger_fury", "tiger_stalk");

	public static final FruitDef YETI = register("yeti", FruitRarity.MYTHICAL, Element.ICE, 0xE6EEF5,
			FruitMoves.bolt(Element.ICE, 18, 1.8, 16.5f),
			FruitMoves.burst(Element.ICE, 6, 16.5f),
			FruitMoves.eruption(Element.ICE, 18, 6.5, 33f, 1.1),
			FruitMoves.lingering(Element.ICE, 16, 5.5, 15f, 8f, 6),
			FruitMoves.leap(Element.ICE, 5, 11.5f),
			"yeti_hurl", "yeti_roar", "yeti_slam", "yeti_rampage", "yeti_leap");

	public static final FruitDef KITSUNE = register("kitsune", FruitRarity.MYTHICAL, Element.FOXFIRE, 0x3A9AF2,
			FruitMoves.bolt(Element.FOXFIRE, 20, 1.3, 16.5f),
			FruitMoves.burst(Element.FOXFIRE, 6, 18f),
			FruitMoves.lingering(Element.FOXFIRE, 16, 5, 15f, 8f, 6),
			FruitMoves.eruption(Element.FOXFIRE, 20, 6.5, 36.5f, 1),
			FruitMoves.blink(Element.FOXFIRE, 13),
			"kitsune_orb", "kitsune_flames", "kitsune_wisp", "kitsune_pillar", "kitsune_dash");

	public static final FruitDef CONTROL = register("control", FruitRarity.MYTHICAL, Element.CONTROL, 0x46D2D2,
			FruitMoves.bolt(Element.CONTROL, 20, 1.3, 16.5f),
			FruitMoves.burst(Element.CONTROL, 6.5, 16.5f),
			FruitMoves.lingering(Element.CONTROL, 16, 6, 13f, 8f, 6),
			FruitMoves.vortex(Element.CONTROL, 18, 6, 28f),
			FruitMoves.blink(Element.CONTROL, 14),
			"control_command", "control_territory", "control_lock", "control_reshape", "control_swap");

	public static final FruitDef DRAGON = register("dragon", FruitRarity.MYTHICAL, Element.DRAGON, 0x3AA84A,
			FruitMoves.bolt(Element.DRAGON, 20, 2, 18f),
			FruitMoves.burst(Element.DRAGON, 6.5, 18f),
			FruitMoves.eruption(Element.DRAGON, 20, 6.5, 36.5f, 1),
			FruitMoves.lingering(Element.DRAGON, 18, 6, 16.5f, 8f, 6),
			FruitMoves.flight(Element.DRAGON, 240, 0.08f),
			"dragon_breath", "dragon_scales", "dragon_talon", "dragon_inferno", "dragon_flight", true);

	// ===== Legacy fruits (pre-0.8 roster). Owners keep them and their mastery until they are migrated. =====

	public static final FruitDef LEGACY_THORN = legacy("thorn", FruitRarity.COMMON, Element.PLANT, 0x5FA84A,
			FruitMoves.bolt(Element.PLANT, 12, 1.3, 6f), FruitMoves.burst(Element.PLANT, 5, 7f),
			FruitMoves.lingering(Element.PLANT, 14, 3.5, 5f, 3f, 5), FruitMoves.eruption(Element.PLANT, 15, 5, 13f, 0.7),
			FruitMoves.dash(Element.PLANT, 1.6, 4f),
			"thorn_shot", "thorn_ring", "thorn_patch", "thorn_eruption", "thorn_swing");
	public static final FruitDef LEGACY_TIDE = legacy("tide", FruitRarity.UNCOMMON, Element.WATER, 0x3F8FE0,
			FruitMoves.bolt(Element.WATER, 14, 1.4, 7f), FruitMoves.burst(Element.WATER, 5, 8f), FruitMoves.bolt(Element.WATER, 20, 2.2, 10f),
			FruitMoves.eruption(Element.WATER, 18, 6, 15f, 0.9), FruitMoves.dash(Element.WATER, 2.0, 4f),
			"tide_jet", "tide_ring", "tide_lance", "tide_crash", "tide_surf");
	public static final FruitDef LEGACY_CORAL = legacy("coral", FruitRarity.UNCOMMON, Element.CRYSTAL, 0xFF7F6B,
			FruitMoves.bolt(Element.CRYSTAL, 12, 1.2, 7f), FruitMoves.burst(Element.CRYSTAL, 5, 8f),
			FruitMoves.lingering(Element.CRYSTAL, 14, 3.5, 6f, 3f, 5), FruitMoves.eruption(Element.CRYSTAL, 16, 5, 15f, 0.8),
			FruitMoves.barrier(Element.CRYSTAL, 5, 1),
			"coral_shard", "coral_spikes", "coral_garden", "coral_eruption", "coral_armor");
	public static final FruitDef LEGACY_IRON = legacy("iron", FruitRarity.UNCOMMON, Element.METAL, 0x9AA3AE,
			FruitMoves.strike(Element.METAL, 5, 9f), FruitMoves.burst(Element.METAL, 4, 9f), FruitMoves.strike(Element.METAL, 7, 13f),
			FruitMoves.eruption(Element.METAL, 12, 4.5, 16f, 0.8), FruitMoves.barrier(Element.METAL, 6, 2),
			"iron_fist", "iron_shockwave", "iron_hammer", "iron_fall", "iron_skin");
	public static final FruitDef LEGACY_BLOOM = legacy("bloom", FruitRarity.RARE, Element.PLANT, 0xF29BC4,
			FruitMoves.bolt(Element.PLANT, 14, 1.3, 7f), FruitMoves.heal(Element.PLANT, 8f, 6),
			FruitMoves.lingering(Element.PLANT, 16, 4, 8f, 4f, 6), FruitMoves.eruption(Element.PLANT, 18, 5.5, 16f, 0.9),
			FruitMoves.barrier(Element.PLANT, 6, 1),
			"bloom_petal", "bloom_heal", "bloom_garden", "bloom_root", "bloom_bark");
	public static final FruitDef LEGACY_PRISM = legacy("prism", FruitRarity.RARE, Element.LIGHT, 0xFFF3A0,
			FruitMoves.bolt(Element.LIGHT, 20, 1.1, 8f), FruitMoves.burst(Element.LIGHT, 5, 10f), FruitMoves.bolt(Element.LIGHT, 26, 1.6, 13f),
			FruitMoves.eruption(Element.LIGHT, 22, 5, 18f, 0.6), FruitMoves.blink(Element.LIGHT, 14),
			"prism_ray", "prism_flash", "prism_beam", "prism_sunfall", "prism_speed");
	public static final FruitDef LEGACY_WOLF = legacy("wolf", FruitRarity.LEGENDARY, Element.BEAST, 0x8C8C9A,
			FruitMoves.strike(Element.BEAST, 5, 10f), FruitMoves.dash(Element.BEAST, 2.2, 10f), FruitMoves.burst(Element.BEAST, 5, 12f),
			FruitMoves.leap(Element.BEAST, 5, 18f), FruitMoves.dash(Element.BEAST, 2.4, 0f),
			"wolf_fang", "wolf_pounce", "wolf_howl", "wolf_rampage", "wolf_sprint");
	public static final FruitDef LEGACY_LEVIATHAN = legacy("leviathan", FruitRarity.MYTHICAL, Element.WATER, 0x1F5FA8,
			FruitMoves.strike(Element.WATER, 10, 11f), FruitMoves.bolt(Element.WATER, 22, 2.2, 13f), FruitMoves.vortex(Element.WATER, 18, 6, 16f),
			FruitMoves.eruption(Element.WATER, 26, 8, 26f, 1.1), FruitMoves.dash(Element.WATER, 2.6, 6f),
			"leviathan_bite", "leviathan_torrent", "leviathan_whirlpool", "leviathan_wrath", "leviathan_dive");
	public static final FruitDef LEGACY_AEGIS = legacy("aegis", FruitRarity.MYTHICAL, Element.BARRIER, 0xF5E08A,
			FruitMoves.strike(Element.BARRIER, 8, 10f), FruitMoves.heal(Element.BARRIER, 12f, 7), FruitMoves.burst(Element.BARRIER, 6, 14f),
			FruitMoves.eruption(Element.BARRIER, 22, 7, 22f, 0.9), FruitMoves.barrier(Element.BARRIER, 8, 3),
			"aegis_bash", "aegis_sanctuary", "aegis_repulse", "aegis_judgment", "aegis_wall");
	public static final FruitDef LEGACY_PEBBLE = legacy("pebble", FruitRarity.COMMON, Element.EARTH, 0x8A8178,
			FruitMoves.bolt(Element.EARTH, 12, 1.3, 6f), FruitMoves.burst(Element.EARTH, 4.5, 7f), FruitMoves.eruption(Element.EARTH, 14, 4, 10f, 0.7),
			FruitMoves.eruption(Element.EARTH, 16, 5.5, 12f, 0.9), FruitMoves.leap(Element.EARTH, 3, 4f),
			"pebble_toss", "pebble_scatter", "pebble_spire", "pebble_landslide", "pebble_hop");
	public static final FruitDef LEGACY_ASH = legacy("ash", FruitRarity.COMMON, Element.MIST, 0x5E5A57,
			FruitMoves.bolt(Element.FIRE, 12, 1.3, 6f), FruitMoves.burst(Element.MIST, 5, 6f), FruitMoves.lingering(Element.FIRE, 14, 3.5, 5f, 3f, 5),
			FruitMoves.eruption(Element.FIRE, 15, 5, 12f, 0.8), FruitMoves.blink(Element.MIST, 8),
			"ash_cinder", "ash_cloud", "ash_embers", "ash_plume", "ash_drift");
	public static final FruitDef LEGACY_SLUDGE = legacy("sludge", FruitRarity.UNCOMMON, Element.EARTH, 0x6B5A3A,
			FruitMoves.bolt(Element.POISON, 12, 1.5, 7f), FruitMoves.lingering(Element.EARTH, 14, 4, 6f, 3f, 6), FruitMoves.burst(Element.EARTH, 5, 9f),
			FruitMoves.vortex(Element.EARTH, 16, 5, 14f), FruitMoves.dash(Element.EARTH, 1.8, 4f),
			"sludge_glob", "sludge_bog", "sludge_splash", "sludge_swamp", "sludge_slide");
	public static final FruitDef LEGACY_GLASS = legacy("glass", FruitRarity.UNCOMMON, Element.CRYSTAL, 0xBFE8F2,
			FruitMoves.bolt(Element.CRYSTAL, 14, 1.1, 8f), FruitMoves.burst(Element.CRYSTAL, 5, 8f), FruitMoves.bolt(Element.CRYSTAL, 20, 1.8, 11f),
			FruitMoves.eruption(Element.CRYSTAL, 16, 5, 15f, 0.6), FruitMoves.barrier(Element.CRYSTAL, 5, 1),
			"glass_shard", "glass_shatter", "glass_lance", "glass_storm", "glass_pane");
	public static final FruitDef LEGACY_RUST = legacy("rust", FruitRarity.UNCOMMON, Element.METAL, 0xA8582E,
			FruitMoves.strike(Element.METAL, 6, 9f), FruitMoves.lingering(Element.POISON, 12, 3.5, 6f, 3f, 5), FruitMoves.burst(Element.METAL, 5, 10f),
			FruitMoves.eruption(Element.METAL, 14, 5, 15f, 0.7), FruitMoves.barrier(Element.METAL, 5, 1),
			"rust_claw", "rust_corrode", "rust_burst", "rust_collapse", "rust_plating");
	public static final FruitDef LEGACY_POLLEN = legacy("pollen", FruitRarity.UNCOMMON, Element.PLANT, 0xF2D64B,
			FruitMoves.bolt(Element.SOUND, 12, 1.6, 6f), FruitMoves.burst(Element.PLANT, 5.5, 8f), FruitMoves.heal(Element.PLANT, 6f, 5),
			FruitMoves.lingering(Element.PLANT, 16, 5, 10f, 4f, 6), FruitMoves.dash(Element.PLANT, 1.8, 3f),
			"pollen_puff", "pollen_burst", "pollen_nectar", "pollen_meadow", "pollen_breeze");
	public static final FruitDef LEGACY_MONSOON = legacy("monsoon", FruitRarity.RARE, Element.WATER, 0x4A6FA0,
			FruitMoves.bolt(Element.WATER, 16, 1.5, 8f), FruitMoves.burst(Element.LIGHTNING, 5.5, 10f), FruitMoves.lingering(Element.WATER, 16, 5, 9f, 4f, 6),
			FruitMoves.eruption(Element.LIGHTNING, 20, 6, 17f, 0.6), FruitMoves.dash(Element.WATER, 2.2, 5f),
			"monsoon_rain", "monsoon_thunder", "monsoon_flood", "monsoon_tempest", "monsoon_current");
	public static final FruitDef LEGACY_MIRAGE = legacy("mirage", FruitRarity.RARE, Element.LIGHT, 0xF5C27A,
			FruitMoves.bolt(Element.LIGHT, 18, 1.2, 8f), FruitMoves.burst(Element.SOUND, 5.5, 9f), FruitMoves.lingering(Element.MIST, 16, 5, 9f, 4f, 6),
			FruitMoves.eruption(Element.LIGHT, 20, 5, 16f, 0.6), FruitMoves.blink(Element.LIGHT, 12),
			"mirage_glint", "mirage_dazzle", "mirage_haze", "mirage_oasis", "mirage_shift");
	public static final FruitDef LEGACY_HORNET = legacy("hornet", FruitRarity.RARE, Element.POISON, 0xE0B020,
			FruitMoves.strike(Element.POISON, 8, 9f), FruitMoves.dash(Element.POISON, 2.2, 9f), FruitMoves.burst(Element.POISON, 5, 11f),
			FruitMoves.vortex(Element.POISON, 18, 5.5, 15f), FruitMoves.updraft(Element.BEAST),
			"hornet_sting", "hornet_dive", "hornet_swarm", "hornet_hive", "hornet_flight");
	public static final FruitDef LEGACY_GRIZZLY = legacy("grizzly", FruitRarity.LEGENDARY, Element.BEAST, 0x6E4A2E,
			FruitMoves.strike(Element.BEAST, 5, 11f), FruitMoves.burst(Element.EARTH, 5, 12f), FruitMoves.dash(Element.BEAST, 2.0, 13f),
			FruitMoves.leap(Element.BEAST, 6, 19f), FruitMoves.barrier(Element.BEAST, 6, 2),
			"grizzly_maul", "grizzly_roar", "grizzly_charge", "grizzly_frenzy", "grizzly_hide");
	public static final FruitDef LEGACY_TEMPEST = legacy("tempest", FruitRarity.LEGENDARY, Element.LIGHTNING, 0x5AA0D6,
			FruitMoves.bolt(Element.WIND, 18, 1.6, 10f), FruitMoves.burst(Element.LIGHTNING, 6, 12f), FruitMoves.vortex(Element.WIND, 20, 6, 15f),
			FruitMoves.lingering(Element.LIGHTNING, 22, 6.5, 18f, 6f, 6), FruitMoves.updraft(Element.WIND),
			"tempest_squall", "tempest_crack", "tempest_cyclone", "tempest_superstorm", "tempest_skyride");
	public static final FruitDef LEGACY_SOLAR = legacy("solar", FruitRarity.LEGENDARY, Element.FIRE, 0xFFB52E,
			FruitMoves.bolt(Element.LIGHT, 20, 1.3, 10f), FruitMoves.burst(Element.FIRE, 6, 13f), FruitMoves.eruption(Element.FIRE, 20, 5, 17f, 1.0),
			FruitMoves.eruption(Element.LIGHT, 26, 7.5, 24f, 0.7), FruitMoves.dash(Element.FIRE, 2.4, 6f),
			"solar_ray", "solar_flare", "solar_corona", "solar_sunburst", "solar_streak");
	public static final FruitDef LEGACY_HYDRA = legacy("hydra", FruitRarity.MYTHICAL, Element.POISON, 0x2E8A6A,
			FruitMoves.strike(Element.POISON, 10, 12f), FruitMoves.bolt(Element.WATER, 22, 2.0, 13f), FruitMoves.lingering(Element.POISON, 18, 6, 14f, 6f, 7),
			FruitMoves.vortex(Element.WATER, 24, 7.5, 25f), FruitMoves.dash(Element.WATER, 2.6, 7f),
			"hydra_bite", "hydra_spray", "hydra_mire", "hydra_maelstrom", "hydra_lunge");
	public static final FruitDef LEGACY_TITAN = legacy("titan", FruitRarity.MYTHICAL, Element.EARTH, 0x8F7F6A,
			FruitMoves.strike(Element.EARTH, 7, 14f), FruitMoves.burst(Element.EARTH, 6.5, 15f), FruitMoves.eruption(Element.EARTH, 20, 6, 20f, 1.2),
			FruitMoves.eruption(Element.EARTH, 26, 9, 28f, 1.0), FruitMoves.barrier(Element.EARTH, 8, 3),
			"titan_fist", "titan_stomp", "titan_pillar", "titan_cataclysm", "titan_bulwark");
	public static final FruitDef LEGACY_SERAPH = legacy("seraph", FruitRarity.MYTHICAL, Element.LIGHT, 0xFFF5D6,
			FruitMoves.bolt(Element.LIGHT, 22, 1.4, 12f), FruitMoves.heal(Element.BARRIER, 14f, 8), FruitMoves.burst(Element.LIGHT, 6.5, 15f),
			FruitMoves.eruption(Element.LIGHT, 26, 8, 26f, 0.8), FruitMoves.updraft(Element.LIGHT),
			"seraph_lance", "seraph_grace", "seraph_halo", "seraph_heavenfall", "seraph_wings");

	private Fruits() {
	}

	private static FruitDef register(String name, FruitRarity rarity, Element element, int color, AbilityAction z, AbilityAction x,
			AbilityAction c, AbilityAction v, AbilityAction r, String zKey, String xKey, String cKey, String vKey, String rKey) {
		return register(name, rarity, element, color, z, x, c, v, r, zKey, xKey, cKey, vKey, rKey, false);
	}

	/** Standard fruit. With {@code flightR} the R move is a toggled flight (press again to land). */
	private static FruitDef register(String name, FruitRarity rarity, Element element, int color, AbilityAction z, AbilityAction x,
			AbilityAction c, AbilityAction v, AbilityAction r, String zKey, String xKey, String cKey, String vKey, String rKey, boolean flightR) {
		Power power = Powers.register(new Power(GrandLineMod.id(name), PowerSource.FRUIT, StatType.FRUIT,
				rarity.name().toLowerCase(java.util.Locale.ROOT), color, List.of(
				new AbilityDef(zKey, 0, 50, 0.08, z),
				new AbilityDef(xKey, 10, 120, 0.14, x),
				new AbilityDef(cKey, 25, 180, 0.2, c),
				new AbilityDef(vKey, 50, 300, 0.3, v),
				flightR ? new AbilityDef(rKey, 15, 120, 0.05, AbilityDef.Activation.TOGGLE, r) : new AbilityDef(rKey, 15, 120, 0.1, r))));
		return registerItem(name, rarity, power);
	}

	/** A fruit from the old roster: still loads, works and keeps its mastery, but never drops or sells. */
	private static FruitDef legacy(String name, FruitRarity rarity, Element element, int color, AbilityAction z, AbilityAction x,
			AbilityAction c, AbilityAction v, AbilityAction r, String zKey, String xKey, String cKey, String vKey, String rKey) {
		FruitDef def = register(name, rarity, element, color, z, x, c, v, r, zKey, xKey, cKey, vKey, rKey, false);
		LEGACY.add(def.power().id());
		return def;
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

	/** The active roster (42 fruits): what the dealer, gacha, treasure, fishing and bosses use. */
	public static List<FruitDef> all() {
		return FRUITS.values().stream().filter(f -> !LEGACY.contains(f.power().id())).toList();
	}

	/** Every registered fruit, including legacy ones that only exist so old saves keep working. */
	public static List<FruitDef> allIncludingLegacy() {
		return Collections.unmodifiableList(new ArrayList<>(FRUITS.values()));
	}

	public static boolean isLegacy(FruitDef fruit) {
		return LEGACY.contains(fruit.power().id());
	}

	public static void register() {
		GrandLineMod.LOGGER.debug("Registered {} fruits", FRUITS.size());
	}
}
