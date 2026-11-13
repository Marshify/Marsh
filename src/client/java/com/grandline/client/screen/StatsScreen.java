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

	private final List<GoldButton> addButtons = new ArrayList<>();
	private EditBox amountBox;
	private GoldButton refund;
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
		left = width / 2 - (PANEL_WIDTH + GAP + GEAR_WIDTH) / 2;
		top = Math.max(6, this.height / 2 - height / 2);

		amountBox = new EditBox(font, left + PANEL_WIDTH - 46, top + 26, 38, 14, Component.translatable("screen.grandline.stats.amount"));
		amountBox.setValue("1");
		amountBox.setMaxLength(3);
		amountBox.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
		addRenderableWidget(amountBox);

		for (StatType stat : StatType.values()) {
			int y = top + 46 + stat.ordinal() * ROW_HEIGHT;
			addButtons.add(addRenderableWidget(new GoldButton(left + PANEL_WIDTH - 52, y + 3, 44, 18, Component.literal("+1"),
					b -> ClientNetworking.requestStatAllocation(stat, amount()))));
		}

		refund = addRenderableWidget(new GoldButton(left + PANEL_WIDTH - 108, top + 46 + StatType.values().length * ROW_HEIGHT + 6,
				100, 18, Component.literal(""), b -> FruitDialogs.send(FruitActionPayload.REFUND_STATS, "", 0)));
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
		GrandLineUi.panel(graphics, font, left, top, PANEL_WIDTH, panelHeight, title);
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
			graphics.fill(left + 6, y, left + PANEL_WIDTH - 6, y + ROW_HEIGHT - 2, GrandLineUi.darken(0xFF000000 | color(stat), 0.22f));
			graphics.fill(left + 6, y, left + 8, y + ROW_HEIGHT - 2, 0xFF000000 | color(stat));
			graphics.renderItem(icon(stat), left + 12, y + 4);
			graphics.drawString(font, Component.translatable(stat.translationKey()), left + 32, y + 4, color(stat));
			graphics.drawString(font, effectText(stat, view), left + 32, y + 14, stat.hasEffect() ? 0xA9C4A0 : 0x7A8290);
			String level = "Lv. " + value + (max ? " (MAX)" : "");
			graphics.drawString(font, level, left + PANEL_WIDTH - 60 - font.width(level), y + 8, 0xFFFFFF);

			GoldButton button = addButtons.get(stat.ordinal());
			button.setMessage(Component.literal("+" + amount));
			button.active = amount <= profile.unallocatedStatPoints() && value + amount <= view.maxPointsPerStat();
		}

		int footerY = top + 46 + StatType.values().length * ROW_HEIGHT + 10;
		graphics.drawString(font, Component.translatable("screen.grandline.stats.points", profile.unallocatedStatPoints()),
				left + 10, footerY, profile.unallocatedStatPoints() > 0 ? 0x7CFC7C : 0xC9D3E0);
		refund.setMessage(Component.translatable("screen.grandline.stats.refund", view.statRefundCost()));
		refund.active = profile.stats().total() > 0 && profile.currency() >= view.statRefundCost();

		renderGear(graphics, mouseX, mouseY, panelHeight);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private int gearLeft() {
		return left + PANEL_WIDTH + GAP;
	}

	private int[] slotBox(int slot) {
		int x = gearLeft() + 12 + slot * 44;
		int y = top + 40;
		return new int[]{x, y, x + 36, y + 36};
	}

	/** Accessory and ring slots (click to unequip) and the total bonuses the server reports. */
	private void renderGear(GuiGraphics graphics, int mouseX, int mouseY, int panelHeight) {
		GrandLineUi.panel(graphics, font, gearLeft(), top, GEAR_WIDTH, panelHeight, Component.translatable("screen.grandline.gear.title"));
		String[] labels = {"screen.grandline.gear.accessory", "screen.grandline.gear.ring", "screen.grandline.gear.ring"};
		com.grandline.gear.EquippedGear gear = com.grandline.client.ClientGear.gear();
		ItemStack hovered = ItemStack.EMPTY;
		for (int slot = 0; slot < 3; slot++) {
			int[] b = slotBox(slot);
			boolean over = mouseX >= b[0] && mouseX < b[2] && mouseY >= b[1] && mouseY < b[3];
			graphics.fill(b[0], b[1], b[2], b[3], over ? 0xFF3A4A60 : 0xFF222A36);
			graphics.fill(b[0], b[1], b[2], b[1] + 1, 0xFFC9A24A);
			graphics.drawCenteredString(font, Component.translatable(labels[slot]), (b[0] + b[2]) / 2, b[3] + 3, 0xC9D3E0);
			ItemStack stack = stackOf(gear.get(slot));
			if (!stack.isEmpty()) {
				graphics.pose().pushPose();
				graphics.pose().translate(b[0] + 2, b[1] + 2, 0);
				graphics.pose().scale(2f, 2f, 1f);
				graphics.renderItem(stack, 0, 0);
				graphics.pose().popPose();
				if (over) {
					hovered = stack;
				}
			}
		}
		int y = top + 96;
		graphics.drawString(font, Component.translatable("screen.grandline.gear.bonuses"), gearLeft() + 10, y, 0xFFD25A);
		y += 12;
		List<Component> lines = com.grandline.client.ClientGear.bonuses().describe();
		if (lines.isEmpty()) {
			graphics.drawString(font, Component.translatable("screen.grandline.gear.none"), gearLeft() + 10, y, 0x7A8290);
		}
		for (Component line : lines) {
			graphics.drawString(font, line, gearLeft() + 10, y, 0x8FB8FF);
			y += 11;
		}
		graphics.drawString(font, Component.translatable("screen.grandline.gear.hint"), gearLeft() + 10, top + panelHeight - 16, 0x7A8290);
		if (!hovered.isEmpty()) {
			graphics.renderTooltip(font, hovered, mouseX, mouseY);
		}
	}

	private static ItemStack stackOf(String itemId) {
		net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(itemId);
		if (itemId.isEmpty() || id == null) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (int slot = 0; slot < 3; slot++) {
			int[] b = slotBox(slot);
			if (button == 0 && mouseX >= b[0] && mouseX < b[2] && mouseY >= b[1] && mouseY < b[3]
					&& !com.grandline.client.ClientGear.gear().get(slot).isEmpty()) {
				ClientNetworking.requestUnequip(slot);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private static Component effectText(StatType stat, ProfileSyncPayload view) {
		return switch (stat) {
			case DEFENSE -> Component.translatable("stat.grandline.effect.health", format(view.bonusHealth()));
			case MELEE -> Component.translatable("stat.grandline.effect.melee", format(view.bonusMeleeDamage()));
			case SWORD -> Component.translatable("stat.grandline.effect.sword", format(view.bonusSwordDamage()));
			case GUN -> Component.translatable("stat.grandline.effect.none");
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
