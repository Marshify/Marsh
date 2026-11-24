package com.grandline.test;

import com.grandline.gear.Gear;
import com.grandline.names.GrandLineNames;
import com.grandline.names.GrandLineNames.Category;
import com.grandline.names.NamesLanguage;
import com.grandline.world.SeaLayout;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;

/**
 * Renamed names are display only: the shown text changes, every internal name (island names used as ids, registry ids,
 * translation keys) stays the same, and an entry whose original no longer exists is kept but ignored.
 */
public final class GrandLineNamesGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "names")
	public void renamesChangeOnlyWhatIsShown(GameTestHelper helper) {
		try {
			// One of each kind, typed with any letter case.
			helper.assertTrue("Saltmarket Docks".equals(GrandLineNames.resolve(Category.ISLAND, "saltmarket docks")), "islands resolve");
			GrandLineNames.rename(Category.ISLAND, "Saltmarket Docks", "Port Royal");
			GrandLineNames.rename(Category.ITEM, GrandLineNames.resolve(Category.ITEM, "katana"), "Wado Blade");
			GrandLineNames.rename(Category.ABILITY, GrandLineNames.resolve(Category.ABILITY, "Iron Jab"), "Steel Jab");
			GrandLineNames.rename(Category.NPC, GrandLineNames.resolve(Category.NPC, "Ability Master"), "Sensei");
			var keys = GrandLineNames.active().keys();
			helper.assertTrue("Wado Blade".equals(keys.get("item.grandline.katana")), "the item is renamed");
			helper.assertTrue("Wado Blade".equals(keys.get("power.grandline.katana")), "and so is its moves panel title");
			helper.assertTrue("Steel Jab".equals(keys.get("ability.grandline.brawler_jab")), "the ability is renamed");
			helper.assertTrue("Sensei".equals(keys.get("entity.grandline.islander.flash_step_master"))
					&& "Sensei".equals(keys.get("entity.grandline.islander.instinct_sage")), "every Ability Master is renamed");
			helper.assertTrue("Port Royal".equals(GrandLineNames.island("Saltmarket Docks")), "the island shows its new name");
			helper.assertTrue("Frostpeak Isle".equals(GrandLineNames.island("Frostpeak Isle")), "other islands keep theirs");
			helper.assertTrue("Sail to Port Royal!".equals(GrandLineNames.islandText("Sail to Saltmarket Docks!")), "inside text too");
			// The new name can be used to find it again.
			helper.assertTrue("Saltmarket Docks".equals(GrandLineNames.resolve(Category.ISLAND, "port royal")), "new names resolve back");
			// What the game shows: the language with the names on top.
			Language shown = NamesLanguage.wrap(Language.getInstance());
			helper.assertTrue("Wado Blade".equals(shown.getOrDefault("item.grandline.katana")), "translations show the new name");
			helper.assertTrue("Iron Jab".equals(GrandLineNames.originals().get("ability.grandline.brawler_jab")), "the originals are untouched");
			// Nothing internal changed.
			helper.assertTrue(SeaLayout.all().stream().anyMatch(s -> s.name().equals("Saltmarket Docks")), "the island's own name (its id) is unchanged");
			helper.assertTrue(SeaLayout.all().stream().noneMatch(s -> s.name().equals("Port Royal")), "no island is really called Port Royal");
			helper.assertTrue("item.grandline.katana".equals(Gear.KATANA.getDescriptionId()), "the item keeps its translation key");
			helper.assertTrue("grandline:katana".equals(BuiltInRegistries.ITEM.getKey(Gear.KATANA).toString()), "and its id");
			// An NPC or enemy renamed on one island only.
			GrandLineNames.renameOnIsland("Crystal Fountain", "Corsair", "Lion");
			helper.assertTrue("Lion".equals(GrandLineNames.npc("entity.grandline.corsair", "Crystal Fountain").getString()), "bandits are lions on that island");
			helper.assertTrue(GrandLineNames.npc("entity.grandline.bandit", "Millbrook Village").getContents()
					instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals("entity.grandline.bandit"),
					"and bandits elsewhere");
			GrandLineNames.resetOnIsland("Crystal Fountain", "Corsair");
			helper.assertTrue(!GrandLineNames.islandNpcRenames().getOrDefault("Crystal Fountain", java.util.Map.of()).containsKey("entity.grandline.corsair"), "reset on the island");
			// A rename whose original no longer exists (the mod renamed it later) is kept, not used, never lost.
			GrandLineNames.rename(Category.ITEM, "A Sword That Was Renamed Later", "Whatever");
			helper.assertTrue(GrandLineNames.renames(Category.ITEM).containsKey("A Sword That Was Renamed Later"), "kept in the file");
			helper.assertTrue(!GrandLineNames.active().keys().containsValue("Whatever"), "but not used");
			// Reset gives the original back.
			GrandLineNames.reset(Category.ITEM, "Katana");
			helper.assertTrue(!GrandLineNames.active().keys().containsKey("item.grandline.katana"), "reset restores the original");
		} finally {
			for (Category c : Category.values()) {
				for (String original : GrandLineNames.renames(c).keySet()) {
					GrandLineNames.reset(c, original);
				}
			}
		}
		helper.assertTrue(GrandLineNames.active().keys().isEmpty() && GrandLineNames.active().islands().isEmpty(), "all reset");
		helper.assertTrue(GrandLineNames.sourceFile() == null, "tests never write the mod's names file");
		helper.succeed();
	}
}
