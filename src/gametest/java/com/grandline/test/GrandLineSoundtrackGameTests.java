package com.grandline.test;

import com.grandline.audio.BossEncounters;
import com.grandline.entity.GrandLineEntities;
import com.grandline.entity.IslandBoss;
import com.grandline.entity.SaberBoss;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;

/** Boss music only for players fighting the boss; every assigned soundtrack file exists. */
public final class GrandLineSoundtrackGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "soundtrack", timeoutTicks = 40)
	public void bossMusicOnlyWhileFighting(GameTestHelper helper) {
		ServerPlayer fighter = GrandLineBatch3GameTests.survivor(helper);
		ServerPlayer onlooker = GrandLineBatch3GameTests.survivor(helper);
		IslandBoss boss = GrandLineEntities.CAPTAIN.create(helper.getLevel());
		Vec3 at = helper.absoluteVec(new Vec3(3.5, 2, 3.5));
		boss.moveTo(at.x, at.y, at.z, 0, 0);
		boss.setNoAi(true);
		helper.getLevel().addFreshEntity(boss);
		helper.assertTrue(BossEncounters.tier(boss) == BossEncounters.STANDARD, "a quest captain is a standard boss");
		SaberBoss temple = GrandLineEntities.SABER_BOSS.create(helper.getLevel());
		helper.assertTrue(BossEncounters.tier(temple) == BossEncounters.MAJOR, "a temple boss is a major boss");
		helper.assertTrue(BossEncounters.tier(fighter) == BossEncounters.NONE, "a player is not a boss");

		helper.assertTrue(BossEncounters.current(fighter) == BossEncounters.NONE && BossEncounters.current(onlooker) == BossEncounters.NONE,
				"standing near a boss is not a fight");
		boss.hurt(helper.getLevel().damageSources().playerAttack(fighter), 2);
		helper.assertTrue(BossEncounters.current(fighter) == BossEncounters.STANDARD, "hitting the boss starts the fight music");
		helper.assertTrue(BossEncounters.current(onlooker) == BossEncounters.NONE, "only for the player fighting");
		onlooker.hurt(helper.getLevel().damageSources().mobAttack(boss), 1);
		helper.assertTrue(BossEncounters.current(onlooker) == BossEncounters.STANDARD, "being hit by the boss joins the fight too");
		Vec3 far = helper.absoluteVec(new Vec3(3.5, 90, 3.5)); // straight up, clear of the other tests
		onlooker.teleportTo(far.x, far.y, far.z);
		helper.assertTrue(BossEncounters.current(onlooker) == BossEncounters.NONE, "running far away ends it");
		boss.kill();
		helper.assertTrue(BossEncounters.current(fighter) == BossEncounters.NONE, "the boss's death ends it");
		temple.discard();
		GrandLineBatch3GameTests.remove(helper, fighter, onlooker);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "soundtrack")
	public void everyListedTrackHasItsStreamedFile(GameTestHelper helper) {
		// Server resources hold data only; the assets ship in the same jar, so read them from the classpath.
		try (var in = GrandLineSoundtrackGameTests.class.getResourceAsStream("/assets/grandline/music/soundtrack.json");
			 var sounds = GrandLineSoundtrackGameTests.class.getResourceAsStream("/assets/grandline/sounds.json")) {
			helper.assertTrue(in != null && sounds != null, "soundtrack.json and sounds.json are in the mod");
			var data = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			var events = com.google.gson.JsonParser.parseString(new String(sounds.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			var tracks = data.getAsJsonObject("tracks");
			helper.assertTrue(tracks.size() >= 1, "at least one track is in the game");
			for (var e : tracks.entrySet()) {
				String event = e.getValue().getAsString().substring("grandline:".length());
				helper.assertTrue(events.has(event), e.getKey() + " has a sound event");
				var entry = events.getAsJsonObject(event).getAsJsonArray("sounds").get(0).getAsJsonObject();
				helper.assertTrue(entry.get("stream").getAsBoolean(), e.getKey() + " is streamed");
				String file = "/assets/grandline/sounds/" + entry.get("name").getAsString().substring("grandline:".length()) + ".ogg";
				helper.assertTrue(GrandLineSoundtrackGameTests.class.getResource(file) != null, e.getKey() + " file " + file);
			}
			// The open ocean and every boss tier of the First Sea have their music.
			helper.assertTrue(tracks.has(data.getAsJsonObject("ocean").get("1").getAsString()), "the ocean has its music");
			for (String tier : new String[]{"standard", "major", "final"}) {
				helper.assertTrue(tracks.has(data.getAsJsonObject("bosses").getAsJsonObject("1").get(tier).getAsString()), tier + " boss music ships");
			}
			helper.assertTrue(GrandLineSoundtrackGameTests.class.getResource("/assets/grandline/music/CREDITS.txt") != null, "the music credits ship too");
			// Every First Sea island has its track.
			for (var spec : com.grandline.world.SeaLayout.all()) {
				if (spec.sea() == 1) {
					helper.assertTrue(data.getAsJsonObject("islands").has(spec.name()), spec.name() + " has a soundtrack assignment");
					String track = data.getAsJsonObject("islands").get(spec.name()).getAsString();
					helper.assertTrue(tracks.has(track), spec.name() + "'s track " + track + " ships with the mod");
				}
			}
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
		helper.succeed();
	}
}
