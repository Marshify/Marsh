package com.grandline.client.hud;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.GrandLineKeys;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.Power;
import com.grandline.combat.ability.PowerSource;
import com.grandline.combat.ability.Powers;
import com.grandline.network.payload.ProfileSyncPayload;
import com.grandline.player.PlayerProfile;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Grand Line HUD. Bottom-left: currency, level, XP bar, Health and Energy bars. Bottom-right: the active power with
 * its mastery bar and the four moves (mastery requirement, key, live cooldown). Draws only server-synced data.
 * Nautical navy/gold styling. Hidden with F1 and while the F3 screen is open.
 */
public final class ProfileHud {
	private static final int NAVY = 0xE6101C2E;
	private static final int GOLD = 0xFFE8B84A;
	private static final int PANEL_BORDER = 0xFF0A1220;
	private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.US);

	private ProfileHud() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> render(graphics));
	}

	/** Movable HUD elements, in Settings order. */
	public static final String[] ELEMENTS = {"status", "moves", "quest"};
	/** Last drawn box of each element (x, y, w, h, before its offset), for the HUD editor. */
	private static final java.util.Map<String, int[]> BOUNDS = new java.util.HashMap<>();

	private static void render(GuiGraphics graphics) {
		Minecraft client = Minecraft.getInstance();
		if (client.screen != null && client.screen.getClass().getName().startsWith("com.grandline.")
				&& !(client.screen instanceof com.grandline.client.screen.MenuScreen)) {
			return; // our own screens have their own layout; avoid overlapping panels
		}
		renderAll(graphics);
	}

	/** Draws every enabled HUD element at its configured position. Also used by the HUD editor. */
	public static void renderAll(GuiGraphics graphics) {
		Minecraft client = Minecraft.getInstance();
		ProfileSyncPayload view = ClientProfileCache.view();
		if (view == null || client.player == null || client.options.hideGui || client.options.renderDebug) {
			return;
		}
		var settings = com.grandline.client.settings.ClientSettings.get();
		// Draw in front of anything else on the GUI layer (items in particular render with depth).
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 400);
		element(graphics, "status", () -> renderStatus(graphics, client, view));
		if (settings.showMoveList) {
			element(graphics, "moves", () -> renderPowerPanel(graphics, client, view));
		}
		if (settings.showQuestTracker) {
			element(graphics, "quest", () -> renderQuest(graphics, client));
		}
		renderCombatTag(graphics, client);
		renderShipHull(graphics, client);
		graphics.pose().popPose();
	}

	private static void element(GuiGraphics graphics, String id, Runnable draw) {
		int[] offset = com.grandline.client.settings.ClientSettings.get().offset(id);
		graphics.pose().pushPose();
		graphics.pose().translate(offset[0], offset[1], 0);
		draw.run();
		graphics.pose().popPose();
	}

	/** The element's box on screen with its offset applied, or null if it was not drawn. */
	public static int[] bounds(String id) {
		int[] b = BOUNDS.get(id);
		if (b == null) {
			return null;
		}
		int[] offset = com.grandline.client.settings.ClientSettings.get().offset(id);
		return new int[]{b[0] + offset[0], b[1] + offset[1], b[2], b[3]};
	}

	/** Where the Menu button is drawn (screen coordinates, offset applied), so the Menu screen can line up with it. */
	public static int[] menuButtonBox() {
		int[] status = bounds("status");
		return status == null ? new int[]{6, 110, 124, 20} : new int[]{status[0], status[1] + status[3] + 2, 124, 20};
	}

	/** Top-left status block (kept out of the bottom-left, where vanilla chat lives). */
	private static void renderStatus(GuiGraphics graphics, Minecraft client, ProfileSyncPayload view) {
		Font font = client.font;
		PlayerProfile profile = view.profile();
		int width = 124;
		int x = 6;
		int y = 6;

		graphics.drawString(font, "$ " + NUMBERS.format(profile.currency()), x, y, 0x6BE36B);
		var rep = com.grandline.client.ClientReputation.state();
		if (rep.side() >= 0) {
			boolean pirate = rep.side() == 0;
			long total = pirate ? rep.bounty() : rep.honor();
			Component label = Component.translatable(pirate ? "hud.grandline.bounty" : "hud.grandline.honor", NUMBERS.format(total));
			int lx = x + font.width("$ " + NUMBERS.format(profile.currency())) + 8;
			graphics.drawString(font, label, lx, y, pirate ? 0xFF7070 : 0x7FB0FF);
		}
		y += 11;
		drawScaled(graphics, font, Component.translatable("hud.grandline.level_big", profile.level()), x, y, 2.0f, 0xFFFFFF);
		y += 19;

		double xpProgress = view.experienceToNextLevel() <= 0 ? 1.0 : Math.min(1.0, (double) profile.experience() / view.experienceToNextLevel());
		com.grandline.client.ui.MarshUi.xpBar(graphics, x, y, width, 8, xpProgress);
		Component xpText = view.experienceToNextLevel() <= 0 ? Component.translatable("hud.grandline.max_level")
				: Component.literal(NUMBERS.format(profile.experience()) + " / " + NUMBERS.format(view.experienceToNextLevel()));
		drawScaled(graphics, font, xpText, x + width / 2f - font.width(xpText) * 0.35f, y + 1, 0.7f, 0xFFFFFF);
		y += 10;

		float health = client.player.getHealth();
		float maxHealth = client.player.getMaxHealth();
		spriteBar(graphics, font, x, y, width, Component.translatable("hud.grandline.health"),
				Math.round(health) + "/" + Math.round(maxHealth), maxHealth <= 0 ? 0 : health / maxHealth, 0);
		y += 16;

		float energy = shownEnergy(ClientProfileCache.energy());
		float maxEnergy = shownMaxEnergy(ClientProfileCache.maxEnergy());
		spriteBar(graphics, font, x, y, width, Component.translatable("hud.grandline.energy"),
				Math.round(energy) + " / " + Math.round(maxEnergy), maxEnergy <= 0 ? 0 : energy / maxEnergy, 1);
		y += 17;
		y = com.grandline.client.race.RaceHud.render(graphics, font, x, y, width);

		if (profile.unallocatedStatPoints() > 0) {
			graphics.drawString(font, Component.translatable("hud.grandline.points_hint", NUMBERS.format(profile.unallocatedStatPoints()),
					GrandLineKeys.STATS.getTranslatedKeyMessage()), x, y, 0x7CFC7C);
			y += 10;
		}
		BOUNDS.put("status", new int[]{x, 6, width, y - 6});
		if (com.grandline.client.settings.ClientSettings.get().showMenuButton && !(client.screen instanceof com.grandline.client.screen.MenuScreen)) {
			// The Menu button (opened with its key; the Menu screen puts the clickable buttons right here).
			com.grandline.client.ui.MarshUi.menuButton(graphics, x, y + 2, width, 20, com.grandline.client.ui.MarshUi.State.NORMAL);
			Component menu = Component.translatable("hud.grandline.menu", GrandLineKeys.MENU.getTranslatedKeyMessage());
			com.grandline.client.ui.MarshUi.outlined(graphics, font, menu, x + width / 2 - font.width(menu) / 2, y + 8, 0xFFFFFF);
			if (profile.unallocatedStatPoints() > 0) {
				com.grandline.client.ui.MarshUi.badge(graphics, x + width - 10, y - 2, 12);
			}
		}
	}

	private static void spriteBar(GuiGraphics graphics, Font font, int x, int y, int width, Component label, String value,
			double progress, int kind) {
		com.grandline.client.ui.MarshUi.bar(graphics, x, y, width, 14, progress, kind);
		com.grandline.client.ui.MarshUi.outlined(graphics, font, label, x + 4, y + 3, 0xFFFFFF);
		com.grandline.client.ui.MarshUi.outlined(graphics, font, Component.literal(value), x + width - font.width(value) - 4, y + 3, 0xFFFFFF);
	}

	/** Bottom-right power panel: translucent backing, opaque text, so the held item stays visible through it. */
	private static final int PANEL_BACK = 0x400B1422;  // 25% opaque dark navy
	private static final int LOCKED_NAME = 0xFF8D96A3;
	private static final int COOLING_NAME = 0xFFB9C4D2;

	/**
	 * The panel's scale: about 75% of the normal GUI size, rounded so the pixel font lands on half-pixel steps (sharp
	 * at every GUI scale); never below the normal size at GUI scale 1, where smaller text would be unreadable.
	 */
	private static float panelScale(Minecraft client) {
		double gui = client.getWindow().getGuiScale();
		double pixels = Math.max(1.0, Math.round(gui * 0.75 * 2) / 2.0);
		return (float) Math.min(1.0, pixels / gui);
	}

	private static void renderPowerPanel(GuiGraphics graphics, Minecraft client, ProfileSyncPayload view) {
		Font font = client.font;
		PlayerProfile profile = view.profile();
		if (!GrandLineKeys.movesReady()) {
			return; // an item from slots 5-9 is in hand: the style and fruit are put away
		}
		PowerSource source = GrandLineKeys.activeSource();
		Power power = source == PowerSource.WEAPON ? GrandLineKeys.activePower()
				: source == PowerSource.FRUIT ? Powers.get(profile.powers().fruit()) : Powers.get(profile.powers().fightingStyle());
		if (power == null) {
			return;
		}

		// Laid out in the panel's own units with the origin at its bottom-right corner, then scaled down as a whole.
		float scale = panelScale(client);
		int right = graphics.guiWidth() - 6, bottom = graphics.guiHeight() - 6;
		int width = Math.max(80, Math.min(128, (int) ((graphics.guiWidth() / 2 - 91 - 12) / scale)));
		int rowHeight = 12;
		int movesHeight = rowHeight * (int) power.abilities().stream().filter(a -> !a.isEmpty()).count();
		int movesTop = -movesHeight;
		int titleTop = movesTop - 13;
		int barTop = titleTop - 4;
		int masteryTop = barTop - 9;
		int x = -width;
		BOUNDS.put("moves", new int[]{right - Math.round(width * scale), bottom + Math.round(masteryTop * scale),
				Math.round(width * scale), Math.round(-masteryTop * scale)});

		long masteryXp = profile.powers().masteryXp(power.id().toString());
		int masteryLevel = view.masteryExperiencePerLevel() <= 0 ? 1 : (int) Math.min(view.maxMastery(), 1 + masteryXp / view.masteryExperiencePerLevel());
		double masteryProgress = masteryLevel >= view.maxMastery() ? 1.0
				: (double) (masteryXp % view.masteryExperiencePerLevel()) / view.masteryExperiencePerLevel();

		graphics.pose().pushPose();
		graphics.pose().translate(right, bottom, 0);
		graphics.pose().scale(scale, scale, 1);

		// Mastery: small label over a thin, softened bar.
		Component masteryText = Component.translatable("hud.grandline.mastery", masteryLevel);
		graphics.drawString(font, masteryText, x + width - font.width(masteryText), masteryTop, 0xFFE8ECF2);
		graphics.fill(x, barTop, x + width, barTop + 2, 0x50000000);
		graphics.fill(x, barTop, x + (int) Math.round(width * Math.max(0, Math.min(1, masteryProgress))), barTop + 2, soften(power.color()));

		// Title: power name in gold, the switch key on the right, a faint accent line underneath.
		graphics.fill(x, titleTop, x + width, titleTop + 12, PANEL_BACK);
		graphics.fill(x, titleTop + 11, x + width, titleTop + 12, (power.color() & 0xFFFFFF) | 0x80000000);
		graphics.drawString(font, Component.translatable(power.translationKey()), x + 3, titleTop + 2, GOLD);
		Component switchHint = Component.literal("[" + GrandLineKeys.SWITCH_POWER.getTranslatedKeyMessage().getString() + "]");
		graphics.drawString(font, switchHint, x + width - font.width(switchHint) - 2, titleTop + 2, 0xFFA9BCD3);

		for (int slot = 0, row = 0; slot < power.abilities().size(); slot++) {
			AbilityDef ability = power.abilities().get(slot);
			if (ability.isEmpty()) {
				continue;
			}
			int y = movesTop + row++ * rowHeight;
			boolean unlocked = masteryLevel >= ability.requiredMastery();
			int cooldown = ClientProfileCache.remainingCooldownTicks(power.abilityId(ability));

			graphics.fill(x, y + 1, x + width, y + rowHeight, PANEL_BACK);
			if (cooldown > 0 && ability.cooldownTicks() > 0) {
				int filled = (int) Math.round(width * (double) cooldown / ability.cooldownTicks());
				graphics.fill(x, y + 1, x + filled, y + rowHeight, 0x50000000);
			}

			int nameColor = !unlocked ? LOCKED_NAME : cooldown > 0 ? COOLING_NAME : 0xFFFFFFFF;
			// Explosive Jump: red while the next use would cost health (repeated-use penalty, synced from the server).
			if (unlocked && "bomb_jump".equals(ability.key()) && com.grandline.client.fx.BombFx.jumpWarning()) {
				nameColor = 0xFFFF5555;
			}
			graphics.drawString(font, Component.translatable(ability.translationKey()), x + 3, y + 3, nameColor);

			String key = "[" + GrandLineKeys.MOVES[slot].getTranslatedKeyMessage().getString() + "]";
			String rightText = cooldown > 0 ? String.format(Locale.ROOT, "%.1fs", cooldown / 20.0) : key;
			graphics.drawString(font, rightText, x + width - font.width(rightText) - 2, y + 3, cooldown > 0 ? 0xFFFF8A65 : GOLD);

			if (!unlocked) {
				Component mastery = Component.translatable("hud.grandline.move_mastery", ability.requiredMastery());
				drawScaled(graphics, font, mastery, x + width - 2 - font.width(mastery) * 0.75f - font.width(rightText) - 4, y + 4, 0.75f, 0xFFE07A7A);
			}
		}
		graphics.pose().popPose();
	}

	/** The power's colour, a little less vivid (blended toward grey) for the mastery bar. */
	private static int soften(int color) {
		int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
		r = (r * 7 + 0x90 * 3) / 10;
		g = (g * 7 + 0x90 * 3) / 10;
		b = (b * 7 + 0x90 * 3) / 10;
		return 0xE6000000 | r << 16 | g << 8 | b;
	}

	/** Hull bar above the hotbar while riding a ship. */
	private static void renderShipHull(GuiGraphics graphics, Minecraft client) {
		if (!(client.player.getVehicle() instanceof com.grandline.ship.PirateShip ship)) {
			return;
		}
		int w = 182;
		int x = graphics.guiWidth() / 2 - w / 2;
		int y = graphics.guiHeight() - 62;
		Component label = Component.translatable("hud.grandline.ship.hull");
		String value = Math.round(ship.hull()) + "/" + Math.round(ship.maxHull());
		com.grandline.client.ui.MarshUi.bar(graphics, x, y, w, 10, ship.hull() / ship.maxHull(), 0);
		drawScaled(graphics, client.font, ship.shipClass().displayName().copy().append(" · ").append(label), x + 3, y + 2, 0.75F, 0xFFFFFF);
		drawScaled(graphics, client.font, Component.literal(value), x + w - 3 - client.font.width(value) * 0.75F, y + 2, 0.75F, 0xFFFFFF);
	}

	/** "In Combat" banner (top centre) while combat-tagged, with "Bounty/Honor at Risk" from 2.5M. */
	private static void renderCombatTag(GuiGraphics graphics, Minecraft client) {
		int seconds = com.grandline.client.ClientReputation.tagSeconds();
		if (seconds <= 0) {
			return;
		}
		var rep = com.grandline.client.ClientReputation.state();
		Component text = Component.translatable("hud.grandline.in_combat", seconds);
		if (rep.atRisk()) {
			text = text.copy().append("  ").append(Component.translatable(rep.side() == 0 ? "hud.grandline.bounty_risk" : "hud.grandline.honor_risk"));
		}
		int w = client.font.width(text) + 16;
		int x = graphics.guiWidth() / 2 - w / 2;
		int y = 34;
		com.grandline.client.ui.MarshUi.button(graphics, x, y, w, 16, com.grandline.client.ui.MarshUi.Color.RED, com.grandline.client.ui.MarshUi.State.NORMAL);
		com.grandline.client.ui.MarshUi.outlined(graphics, client.font, text, x + 8, y + 4, 0xFFFFFF);
	}

	/** Width that keeps a side panel clear of the centred vanilla hotbar (182 px wide). */
	private static int sideWidth(GuiGraphics graphics, int preferred) {
		return Math.max(80, Math.min(preferred, graphics.guiWidth() / 2 - 91 - 12));
	}

	/** Top-centre quest tracker with distance and direction to the quest island. */
	private static void renderQuest(GuiGraphics graphics, Minecraft client) {
		com.grandline.npc.QuestState quest = ClientProfileCache.quest();
		if (quest == null) {
			return;
		}
		Font font = client.font;
		Component title = com.grandline.client.screen.NpcScreensAccess.describe(quest);
		Component progress = Component.translatable("hud.grandline.quest.progress", quest.progress(), quest.required());
		double dx = quest.x() - client.player.getX();
		double dz = quest.z() - client.player.getZ();
		int distance = (int) Math.sqrt(dx * dx + dz * dz);
		double bearing = Math.toDegrees(Math.atan2(-dx, dz)) - client.player.getYRot();
		bearing = ((bearing % 360) + 540) % 360 - 180;
		String arrow = Math.abs(bearing) < 22.5 ? "↑" : Math.abs(bearing) > 157.5 ? "↓" : bearing > 0 ? (bearing < 67.5 ? "↗" : bearing < 112.5 ? "→" : "↘")
				: (bearing > -67.5 ? "↖" : bearing > -112.5 ? "←" : "↙");
		Component where = Component.literal(arrow + " " + distance + "m");
		int width = Math.max(font.width(title), font.width(progress) + font.width(where) + 12) + 12;
		int x = graphics.guiWidth() / 2 - width / 2;
		int y = 52; // below the boss bar / Jade's block info at the top centre
		BOUNDS.put("quest", new int[]{x - 1, y - 1, width + 2, 29});
		graphics.fill(x - 1, y - 1, x + width + 1, y + 27, PANEL_BORDER);
		graphics.fill(x, y, x + width, y + 26, NAVY);
		graphics.fill(x, y, x + width, y + 2, GOLD);
		graphics.drawString(font, title, x + 6, y + 5, 0xFFFFFF);
		graphics.drawString(font, progress, x + 6, y + 16, 0x9CFF9C);
		graphics.drawString(font, where, x + width - 6 - font.width(where), y + 16, GOLD);
	}

	private static void labelledBar(GuiGraphics graphics, Font font, int x, int y, int width, Component label, String value,
			double progress, int color, int background) {
		bar(graphics, x, y, width, 11, progress, color, background);
		graphics.drawString(font, label, x + 3, y + 2, 0xFFFFFF);
		graphics.drawString(font, value, x + width - font.width(value) - 3, y + 2, 0xFFFFFF);
	}

	private static void bar(GuiGraphics graphics, int x, int y, int width, int height, double progress, int color, int background) {
		graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, PANEL_BORDER);
		graphics.fill(x, y, x + width, y + height, background);
		graphics.fill(x, y, x + (int) Math.round(width * Math.max(0, Math.min(1, progress))), y + height, color);
		graphics.fill(x, y, x + width, y + 1, 0x30FFFFFF);
	}

	private static void drawScaled(GuiGraphics graphics, Font font, Component text, float x, float y, float scale, int color) {
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(scale, scale, 1);
		graphics.drawString(font, text, 0, 0, color);
		graphics.pose().popPose();
	}

	// The energy bar eases towards the server's values (synced a few times a second), so spends, regeneration and a
	// bigger max from Melee animate smoothly at any frame rate. Big drops (a move) settle fast, regeneration glides.
	private static float shownEnergy = -1, shownMax = -1;
	private static long energyNanos;

	private static float shownEnergy(float target) {
		long now = System.nanoTime();
		double dt = energyNanos == 0 ? 0 : Math.min(0.1, (now - energyNanos) / 1.0e9);
		energyNanos = now;
		if (shownEnergy < 0 || Math.abs(target - shownEnergy) > Math.max(1, shownMax) * 0.9) {
			shownEnergy = target;
		} else {
			shownEnergy += (float) ((target - shownEnergy) * (1 - Math.exp(-12 * dt)));
		}
		return shownEnergy;
	}

	private static float shownMaxEnergy(float target) {
		if (shownMax < 0) {
			shownMax = target;
		} else {
			shownMax += (target - shownMax) * 0.25f;
			if (Math.abs(target - shownMax) < 0.5f) {
				shownMax = target;
			}
		}
		return shownMax;
	}
}
