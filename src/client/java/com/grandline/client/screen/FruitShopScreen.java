package com.grandline.client.screen;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.ui.GoldButton;
import com.grandline.client.ui.GrandLineUi;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.fruit.Fruits;
import com.grandline.network.payload.FruitActionPayload;
import com.grandline.network.payload.OpenNpcPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The dealer's current stock as rows: rarity-framed fruit icon, name, price (or "Owned"), and each move's key with the
 * mastery it unlocks at. Buying sends a request; the server checks stock, distance and money.
 */
public final class FruitShopScreen extends Screen {
	private static final String[] KEYS = {"Z", "X", "C", "V", "R"};
	private static final int ROW = 58;
	private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.US);

	private final OpenNpcPayload data;
	private final List<Fruits.FruitDef> stock = new ArrayList<>();
	private int left;
	private int top;
	private int panelWidth;

	public FruitShopScreen(OpenNpcPayload data) {
		super(Component.translatable("screen.grandline.shop.title"));
		this.data = data;
		for (String id : data.stock()) {
			Fruits.FruitDef fruit = Fruits.get(id);
			if (fruit != null) {
				stock.add(fruit);
			}
		}
	}

	private long price(Fruits.FruitDef fruit) {
		int index = data.stock().indexOf(fruit.id());
		return index >= 0 && index < data.prices().size() ? data.prices().get(index) : 0;
	}

	@Override
	protected void init() {
		panelWidth = Math.min(340, width - 20);
		left = width / 2 - panelWidth / 2;
		top = Math.max(6, height / 2 - (26 + stock.size() * ROW + 24) / 2);
		for (int i = 0; i < stock.size(); i++) {
			Fruits.FruitDef fruit = stock.get(i);
			int y = top + 26 + i * ROW;
			addRenderableWidget(new GoldButton(left + 60, y + 34, 70, 16, Component.translatable("screen.grandline.shop.buy"),
					b -> FruitDialogs.send(FruitActionPayload.SHOP_BUY, fruit.id(), data.entityId())));
		}
		addRenderableWidget(new GoldButton(left + panelWidth - 60, top + 2, 54, 16, Component.translatable("screen.grandline.back"),
				b -> NpcScreens.open(data)));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		int height = 26 + stock.size() * ROW + 22;
		GrandLineUi.panel(graphics, font, left, top, panelWidth, height, title);
		String owned = ClientProfileCache.profile() == null ? "" : ClientProfileCache.profile().powers().fruit();

		for (int i = 0; i < stock.size(); i++) {
			Fruits.FruitDef fruit = stock.get(i);
			int y = top + 26 + i * ROW;
			graphics.fill(left + 4, y, left + panelWidth - 4, y + ROW - 4, 0xF0303846);
			GrandLineUi.tile(graphics, left + 8, y + 3, 46, 46, fruit.rarity().color(), false);
			graphics.pose().pushPose();
			graphics.pose().translate(left + 15, y + 10, 0);
			graphics.pose().scale(2, 2, 1);
			graphics.renderItem(new ItemStack(fruit.item()), 0, 0);
			graphics.pose().popPose();

			GrandLineUi.scaled(graphics, font, Component.translatable(fruit.power().translationKey()), left + 60, y + 4, 1.6f, 0xFFFFFF, true);
			graphics.drawString(font, Component.translatable(fruit.rarity().translationKey()), left + 60, y + 20, fruit.rarity().color());
			Component price = fruit.id().equals(owned) ? Component.translatable("screen.grandline.shop.owned")
					: Component.literal("$" + NUMBERS.format(price(fruit)));
			graphics.drawString(font, price, left + 136, y + 38, fruit.id().equals(owned) ? 0xFFD24B : 0x6BE36B);

			int mx = left + panelWidth - 64;
			for (int m = 0; m < fruit.power().abilities().size(); m++) {
				AbilityDef ability = fruit.power().abilities().get(m);
				int my = y + 3 + m * 10;
				graphics.fill(mx, my, mx + 9, my + 9, 0xFF3A3F4A);
				graphics.drawString(font, KEYS[m], mx + 2, my + 1, 0xFFFFFF);
				graphics.drawString(font, "★ " + ability.requiredMastery(), mx + 13, my + 1, 0xFFD24B);
			}
		}
		graphics.fill(left, top + height - 16, left + panelWidth, top + height, 0xF0000000);
		graphics.drawString(font, Component.translatable("screen.grandline.shop.footer"), left + 6, top + height - 12, 0xFFFFFF);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
