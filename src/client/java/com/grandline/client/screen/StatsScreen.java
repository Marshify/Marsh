package com.grandline.client.screen;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.network.ClientNetworking;
import com.grandline.client.ui.GoldButton;
import com.grandline.client.ui.GrandLineUi;
import com.grandline.fruit.Fruits;
import com.grandline.network.payload.AllocateStatPayload;
import com.grandline.network.payload.FruitActionPayload;
import com.grandline.network.payload.ProfileSyncPayload;
import com.grandline.player.PlayerProfile;
import com.grandline.player.StatType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stats panel: one row per stat (icon, coloured name, current level, effect, +N button), a custom amount box, the
 * remaining points and a Refund button. Buttons only send intents; values always come from the server's last sync.
 */
public final class StatsScreen extends Screen {
	private static final int ROW_HEIGHT = 26;
	private static final int PANEL_WIDTH = 300;
	private static final int GEAR_WIDTH = 150;
	private static final int GAP = 8;

	private final List<com.grandline.client.ui.MarshButton> addButtons = new ArrayList<>();
	private EditBox amountBox;
	private com.grandline.client.ui.MarshButton refund;
	private int left;
	private int top;

	public StatsScreen() {
		super(Component.translatable("screen.grandline.stats.title"));
	}

	private static int color(StatType stat) {
		return switch (stat) {
			case MELEE -> 0xFF8A65;
			case DEFENSE -> 0x6FB3FF;
			case SWORD -> 0x8BE36B;
			case GUN -> 0xFFD25A;
			case FRUIT -> 0xD08CFF;
		};
	}

	private static ItemStack icon(StatType stat) {
		return switch (stat) {
			case MELEE -> new ItemStack(Items.BLAZE_POWDER);
			case DEFENSE -> new ItemStack(Items.SHIELD);
			case SWORD -> new ItemStack(Items.IRON_SWORD);
			case GUN -> new ItemStack(Items.CROSSBOW);
			case FRUIT -> new ItemStack(Fruits.FLAME.item());
		};
	}

	@Override
	protected void init() {
		addButtons.clear();
		int height = 26 + 18 + StatType.values().length * ROW_HEIGHT + 30;
		left = width / 2 - PANEL_WIDTH / 2;
		top = Math.max(6, this.height / 2 - height / 2);

		amountBox = new EditBox(font, left + PANEL_WIDTH - 46, top + 26, 38, 14, Component.translatable("screen.grandline.stats.amount"));
		amountBox.setValue("1");
		amountBox.setMaxLength(3);
		amountBox.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
		addRenderableWidget(amountBox);

		for (StatType stat : StatType.values()) {
			int y = top + 46 + stat.ordinal() * ROW_HEIGHT;
			addButtons.add(addRenderableWidget(new com.grandline.client.ui.MarshButton(left + PANEL_WIDTH - 52, y + 3, 44, 18,
					Component.literal("+1"), com.grandline.client.ui.MarshUi.Color.YELLOW, () -> ClientNetworking.requestStatAllocation(stat, amount()))));
		}

		refund = addRenderableWidget(new com.grandline.client.ui.MarshButton(left + PANEL_WIDTH - 108, top + 46 + StatType.values().length * ROW_HEIGHT + 6,
				100, 18, Component.literal(""), com.grandline.client.ui.MarshUi.Color.YELLOW, () -> FruitDialogs.send(FruitActionPayload.REFUND_STATS, "", 0)));
	}

	private int amount() {
		try {
			return Math.max(1, Math.min(AllocateStatPayload.MAX_AMOUNT_PER_REQUEST, Integer.parseInt(amountBox.getValue())));
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		int panelHeight = 26 + 18 + StatType.values().length * ROW_HEIGHT + 32;
		com.grandline.client.ui.MarshUi.window(graphics, font, left, top, PANEL_WIDTH, panelHeight, title);
		graphics.drawString(font, Component.translatable("screen.grandline.stats.custom_amount"), left + PANEL_WIDTH - 46 - 4
				- font.width(Component.translatable("screen.grandline.stats.custom_amount")), top + 29, 0xC9D3E0);

		ProfileSyncPayload view = ClientProfileCache.view();
		if (view == null) {
			graphics.drawCenteredString(font, Component.translatable("screen.grandline.stats.waiting"), width / 2, top + 60, 0xAAAAAA);
			super.render(graphics, mouseX, mouseY, partialTick);
			return;
		}
		PlayerProfile profile = view.profile();
		int amount = amount();

		for (StatType stat : StatType.values()) {
			int y = top + 46 + stat.ordinal() * ROW_HEIGHT;
			int value = profile.stats().get(stat);
			boolean max = value >= view.maxPointsPerStat();
			com.grandline.client.ui.MarshUi.row(graphics, left + 6, y, PANEL_WIDTH - 12, ROW_HEIGHT - 2,
					com.grandline.client.ui.MarshUi.Row.values()[stat.ordinal() + 1]);
			graphics.renderItem(icon(stat), left + 12, y + 4);
			graphics.drawString(font, Component.translatable(stat.translationKey()), left + 32, y + 4, color(stat));
			graphics.drawString(font, effectText(stat, view), left + 32, y + 14, stat.hasEffect() ? 0xA9C4A0 : 0x7A8290);
			String level = "Lv. " + value + (max ? " (MAX)" : "");
			graphics.drawString(font, level, left + PANEL_WIDTH - 60 - font.width(level), y + 8, 0xFFFFFF);

			com.grandline.client.ui.MarshButton button = addButtons.get(stat.ordinal());
			button.setMessage(Component.literal("+" + amount));
			button.active = amount <= profile.unallocatedStatPoints() && value + amount <= view.maxPointsPerStat();
		}

		int footerY = top + 46 + StatType.values().length * ROW_HEIGHT + 10;
		graphics.drawString(font, Component.translatable("screen.grandline.stats.points", profile.unallocatedStatPoints()),
				left + 10, footerY, profile.unallocatedStatPoints() > 0 ? 0x7CFC7C : 0xC9D3E0);
		refund.setMessage(Component.translatable("screen.grandline.stats.refund", view.statRefundCost()));
		refund.active = profile.stats().total() > 0 && profile.currency() >= view.statRefundCost();

		techniques(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	/** Techniques beside the stats: what is learned, Aura's stage and XP, Instinct's charges and XP. */
	private void techniques(GuiGraphics g) {
		var own = com.grandline.client.technique.ClientTechniques.own();
		int w = 150, x = left + PANEL_WIDTH + 6, y = top;
		if (x + w > width - 4) {
			x = left - w - 6; // no room on the right: the left side instead
			if (x < 4) {
				return;
			}
		}
		com.grandline.client.ui.MarshUi.window(g, font, x, y, w, 150, Component.translatable("screen.grandline.techniques.title"));
		int ty = y + 26;
		g.drawString(font, Component.translatable("technique.grandline.dash"), x + 8, ty, 0xFFFFFF);
		g.drawString(font, Component.translatable("screen.grandline.techniques.always"), x + w - 8 - font.width(Component.translatable("screen.grandline.techniques.always")), ty, 0x7CFC7C);
		ty += 12;
		Component fs = Component.translatable(own.flashStep() ? "screen.grandline.techniques.learned" : "screen.grandline.techniques.locked");
		g.drawString(font, Component.translatable("technique.grandline.flash_step"), x + 8, ty, 0xFFFFFF);
		g.drawString(font, fs, x + w - 8 - font.width(fs), ty, own.flashStep() ? 0x7CFC7C : 0x8A92A0);
		ty += 16;
		g.drawString(font, Component.translatable("technique.grandline.aura"), x + 8, ty, 0xFFFFFF);
		if (own.aura()) {
			int stage = com.grandline.technique.TechniqueRules.auraStage(own.auraXp());
			long next = com.grandline.technique.TechniqueRules.auraNextXp(own.auraXp());
			long prev = stage == 0 ? 0 : com.grandline.GrandLineMod.config().auraStageXp[stage - 1];
			Component st = Component.translatable("screen.grandline.techniques.stage", stage);
			g.drawString(font, st, x + w - 8 - font.width(st), ty, 0x9FD4FF);
			ty += 11;
			double frac = next < 0 ? 1 : (own.auraXp() - prev) / (double) Math.max(1, next - prev);
			com.grandline.client.ui.MarshUi.xpBar(g, x + 8, ty, w - 16, 6, frac);
			ty += 8;
			String xp = next < 0 ? own.auraXp() + " XP (max)" : own.auraXp() + " / " + next + " XP";
			g.drawString(font, xp, x + 8, ty, 0xC9D3E0);
			ty += 10;
			g.drawString(font, Component.translatable("screen.grandline.techniques.aura_bonus",
					Math.round(com.grandline.technique.TechniqueRules.auraDamageBonus(stage) * 100),
					Math.round(com.grandline.technique.TechniqueRules.auraReduction(stage) * 100)), x + 8, ty, 0xA9C4A0);
			ty += 14;
		} else {
			Component lk = Component.translatable("screen.grandline.techniques.locked");
			g.drawString(font, lk, x + w - 8 - font.width(lk), ty, 0x8A92A0);
			ty += 16;
		}
		g.drawString(font, Component.translatable("technique.grandline.instinct"), x + 8, ty, 0xFFFFFF);
		if (own.instinct()) {
			int max = com.grandline.technique.TechniqueRules.maxDodges(own.instinctXp());
			long next = com.grandline.technique.TechniqueRules.instinctNextXp(own.instinctXp());
			Component ch = Component.translatable("screen.grandline.techniques.charges", own.dodges(), max);
			g.drawString(font, ch, x + w - 8 - font.width(ch), ty, 0x9FD4FF);
			ty += 11;
			long prev = 0;
			for (long t : com.grandline.GrandLineMod.config().instinctChargeXp) {
				if (own.instinctXp() >= t) {
					prev = t;
				}
			}
			double frac = next < 0 ? 1 : (own.instinctXp() - prev) / (double) Math.max(1, next - prev);
			com.grandline.client.ui.MarshUi.xpBar(g, x + 8, ty, w - 16, 6, frac);
			ty += 8;
			String xp = next < 0 ? own.instinctXp() + " XP (max)" : own.instinctXp() + " / " + next + " XP";
			g.drawString(font, xp, x + 8, ty, 0xC9D3E0);
		} else {
			Component lk = Component.translatable("screen.grandline.techniques.locked");
			g.drawString(font, lk, x + w - 8 - font.width(lk), ty, 0x8A92A0);
		}
	}

	private static Component effectText(StatType stat, ProfileSyncPayload view) {
		return switch (stat) {
			case DEFENSE -> Component.translatable("stat.grandline.effect.health", format(view.bonusHealth()));
			case MELEE -> Component.translatable("stat.grandline.effect.melee", format(view.bonusMeleeDamage()));
			case SWORD -> Component.translatable("stat.grandline.effect.sword", format(view.bonusSwordDamage()));
			case GUN -> Component.translatable("stat.grandline.effect.gun");
			case FRUIT -> Component.translatable("stat.grandline.effect.fruit");
		};
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
