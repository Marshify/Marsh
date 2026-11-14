package com.grandline.race;

import com.grandline.entity.Islander;
import com.grandline.player.PlayerProfileService;
import com.grandline.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;

/**
 * The race NPCs: Race Mystic (quests, evolution V2/V3, awakening lore, reroll), Tinkerer (Cyborg), Gravekeeper
 * (Ghoul) and Trial Keeper (trial, V4). Talking to one opens the race menu with that NPC's options; every choice comes
 * back as a {@link com.grandline.network.payload.RaceActionPayload} and is checked here (distance, role, requirements,
 * money) before anything changes.
 */
public final class RaceNpcs {
	public static final int EVOLVE = 3, START_QUEST = 4, REROLL = 5, BECOME = 6, TRIAL = 7;

	private RaceNpcs() {
	}

	public static boolean isRaceNpc(Islander.Role role) {
		return role == Islander.Role.RACE_MYSTIC || role == Islander.Role.TINKERER || role == Islander.Role.GRAVEKEEPER
				|| role == Islander.Role.TRIAL_KEEPER;
	}

	/** Opens the race menu for this NPC (and records discoveries made by talking to it). */
	public static void open(ServerPlayer player, Islander npc) {
		RaceData d = RaceService.get(player);
		if (npc.role() == Islander.Role.RACE_MYSTIC && d.stage() == 3 && !d.has("lore")) {
			RaceService.update(player, x -> x.withFlag("lore"));
			RaceProgress.notify(player, "race.grandline.v4.lore", ChatFormatting.LIGHT_PURPLE);
		}
		if (npc.role() == Islander.Role.TRIAL_KEEPER && d.stage() == 3 && d.has("lore") && !d.has("found_keeper")) {
			RaceService.update(player, x -> x.withFlag("found_keeper"));
			RaceProgress.notify(player, "race.grandline.v4.found_keeper", ChatFormatting.LIGHT_PURPLE);
		}
		CompoundTag tag = new CompoundTag();
		tag.putInt("id", npc.getId());
		tag.putString("role", npc.role().name().toLowerCase(java.util.Locale.ROOT));
		if (npc.role() == Islander.Role.TINKERER || npc.role() == Islander.Role.GRAVEKEEPER) {
			Race race = npc.role() == Islander.Role.TINKERER ? Race.CYBORG : Race.GHOUL;
			tag.put("lines", RaceProgress.toTag(RaceProgress.unlockRequirements(player, race)));
			tag.putBoolean("owned", d.has("unlock:" + race.id()));
		}
		tag.putLong("reroll", RaceProgress.config().raceRerollCost);
		RaceService.sync(player, tag);
	}

	/** Runs an NPC choice. Returns a message key for a failure, or null on success. Exposed for tests. */
	public static String act(ServerPlayer player, int action, int npcId) {
		Entity entity = player.serverLevel().getEntity(npcId);
		if (!(entity instanceof Islander npc) || npc.distanceTo(player) > 8 || !isRaceNpc(npc.role())) {
			return "race.grandline.npc.too_far";
		}
		return act(player, action, npc.role());
	}

	/** The choice itself, for an NPC role the player is next to. */
	public static String act(ServerPlayer player, int action, Islander.Role role) {
		RaceData d = RaceService.get(player);
		String result = switch (action) {
			case START_QUEST -> role != Islander.Role.RACE_MYSTIC ? "race.grandline.npc.wrong" : startQuest(player, d);
			case EVOLVE -> evolve(player, d, role);
			case REROLL -> role != Islander.Role.RACE_MYSTIC ? "race.grandline.npc.wrong" : reroll(player);
			case BECOME -> role == Islander.Role.TINKERER ? become(player, Race.CYBORG)
					: role == Islander.Role.GRAVEKEEPER ? become(player, Race.GHOUL) : "race.grandline.npc.wrong";
			case TRIAL -> role != Islander.Role.TRIAL_KEEPER ? "race.grandline.npc.wrong" : RaceTrials.start(player);
			default -> "race.grandline.unknown";
		};
		if (result == null) {
			RaceService.sync(player);
		}
		return result;
	}

	static String startQuest(ServerPlayer player, RaceData d) {
		String flag = d.stage() == 1 ? "q2" : d.stage() == 2 ? "q3" : null;
		if (flag == null) {
			return "race.grandline.quest.none";
		}
		if (d.has(flag)) {
			return "race.grandline.quest.already";
		}
		RaceService.update(player, x -> x.withFlag(flag));
		RaceProgress.notify(player, "race.grandline.quest.started_" + flag, ChatFormatting.GOLD);
		return null;
	}

	static String evolve(ServerPlayer player, RaceData d, Islander.Role role) {
		if (d.stage() >= 4) {
			return "race.grandline.evolve.max";
		}
		Islander.Role needed = d.stage() == 3 ? Islander.Role.TRIAL_KEEPER : Islander.Role.RACE_MYSTIC;
		if (role != needed) {
			return "race.grandline.npc.wrong";
		}
		if (!RaceProgress.allDone(RaceProgress.requirements(player))) {
			return "race.grandline.evolve.missing";
		}
		long cost = RaceProgress.config().raceEvolutionCosts[d.stage() - 1];
		if (d.stage() == 3 && !RaceProgress.take(player, ModItems.AWAKENING_KEY, 1)) {
			return "race.grandline.evolve.missing";
		}
		PlayerProfileService.update(player, p -> p.withCurrency(p.currency() - cost));
		int next = d.stage() + 1;
		RaceService.update(player, x -> {
			RaceData y = x.withStage(next).withoutFlag("q2").withoutFlag("q3").withCounter("v2_kills", 0).withCounter("v3_goal", 0);
			java.util.List<String> keep = new ArrayList<>(y.flags());
			keep.removeIf(f -> f.startsWith("isl:"));
			return new RaceData(y.race(), y.stage(), keep, y.counters(), y.upgrades(), y.energy());
		});
		RaceProgress.forget(player.getUUID());
		com.grandline.combat.fx.FxEvents.send(player, "race_evolve", player.position(), next);
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1F, 1F);
		Component msg = Component.translatable("race.grandline.evolved", Component.translatable("race.grandline." + d.kind().id()), "V" + next)
				.withStyle(ChatFormatting.GOLD);
		player.sendSystemMessage(msg);
		player.displayClientMessage(msg, true);
		RacePassives.apply(player);
		return null;
	}

	/** A new standard race (never the current one) for the reroll cost; progression restarts at V1. */
	static String reroll(ServerPlayer player) {
		long cost = RaceProgress.config().raceRerollCost;
		if (PlayerProfileService.get(player).currency() < cost) {
			return "race.grandline.reroll.money";
		}
		if (RaceService.v3Active(player) || RaceService.v4Active(player) || RaceTrials.inTrial(player)) {
			return "race.grandline.reroll.busy";
		}
		PlayerProfileService.update(player, p -> p.withCurrency(p.currency() - cost));
		Race next = RaceService.rollStarting(player.getRandom(), RaceService.race(player));
		changeRace(player, next);
		return null;
	}

	/** Unlocks (first time: parts, boss, unlock cost) or switches to (later: reroll cost) Cyborg or Ghoul. */
	static String become(ServerPlayer player, Race race) {
		RaceData d = RaceService.get(player);
		if (d.kind() == race) {
			return "race.grandline.become.already";
		}
		if (RaceService.v3Active(player) || RaceService.v4Active(player) || RaceTrials.inTrial(player)) {
			return "race.grandline.reroll.busy";
		}
		if (!RaceProgress.allDone(RaceProgress.unlockRequirements(player, race))) {
			return "race.grandline.evolve.missing";
		}
		boolean owned = d.has("unlock:" + race.id());
		long cost = owned ? RaceProgress.config().raceRerollCost : RaceProgress.config().raceUnlockCost;
		if (!owned && !RaceProgress.take(player, race == Race.CYBORG ? ModItems.MACHINE_CORE : ModItems.BLOOD_VIAL,
				RaceProgress.config().raceUnlockParts)) {
			return "race.grandline.evolve.missing";
		}
		PlayerProfileService.update(player, p -> p.withCurrency(p.currency() - cost));
		RaceService.update(player, x -> x.withFlag("unlock:" + race.id()));
		changeRace(player, race);
		return null;
	}

	/**
	 * Changes race. Consistent rules for every change (reroll or unlock): back to V1, quests, objectives, trials,
	 * Awakening Points, upgrades and energy reset; unlocked races, bosses defeated for unlocks and items are kept.
	 */
	public static void changeRace(ServerPlayer player, Race race) {
		RaceService.V3_UNTIL.remove(player.getUUID());
		RaceService.V4_UNTIL.remove(player.getUUID());
		RaceService.update(player, x -> {
			java.util.List<String> keep = new ArrayList<>();
			for (String f : x.flags()) {
				if (f.startsWith("unlock:") || f.startsWith("boss:")) {
					keep.add(f);
				}
			}
			return new RaceData(race.id(), 1, keep, java.util.Map.of(), java.util.List.of(), 0);
		});
		RaceProgress.forget(player.getUUID());
		com.grandline.combat.fx.FxEvents.send(player, "race_evolve", player.position(), 1);
		Component msg = Component.translatable("race.grandline.changed", Component.translatable("race.grandline." + race.id()))
				.withStyle(ChatFormatting.GOLD);
		player.sendSystemMessage(msg);
		player.displayClientMessage(msg, true);
		RacePassives.apply(player);
	}
}
