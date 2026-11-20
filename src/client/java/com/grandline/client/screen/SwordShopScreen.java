package com.grandline.client.screen;

import com.grandline.client.ClientProfileCache;
import com.grandline.client.ui.GoldButton;
import com.grandline.client.ui.GrandLineUi;
import com.grandline.combat.ability.AbilityDef;
import com.grandline.combat.ability.Power;
import com.grandline.fruit.FruitRarity;
import com.grandline.gear.Gear;
import com.grandline.gear.sword.Swords;
import com.grandline.network.payload.NpcActionPayload;
import com.grandline.network.payload.OpenNpcPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * The Swordsmith's catalogue: every sword for sale, cheapest first, with its rarity frame, name, price, both moves (key
 * and mastery) and Buy, or Owned. Scroll with the mouse wheel. Buying sends a request; the server checks distance,
 * ownership and money.
 */
public final class SwordShopScreen extends Screen {
	private static final String[] KEYS = {"Z", "X", "C"};
	private static final int ROW = 52;
	private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.US);

	private final OpenNpcPayload data;
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;
	private double scroll;

	public SwordShopScreen(OpenNpcPayload data) {
		super(Component.translatable("screen.grandline.swords.title"));
		this.data = data;
	}

	private int count() {
		return Math.min(data.stock().size(), data.prices().size());
	}

	private Item item(int i) {
		return BuiltInRegistries.ITEM.get(new ResourceLocation(data.stock().get(i)));
	}

	@Override
	protected void init() {
		panelWidth = Math.min(400, width - 20);
		panelHeight = Math.min(height - 16, 340);
		left = width / 2 - panelWidth / 2;
		top = height / 2 - panelHeight / 2;
		addRenderableWidget(new GoldButton(left + panelWidth - 60, top + 2, 54, 16, Component.translatable("screen.grandline.back"),
				b -> NpcScreens.open(data)));
	}

	private int listTop() {
		return top + 26;
	}

	private int listBottom() {
		return top + panelHeight - 18;
	}

	private double maxScroll() {
		return Math.max(0, count() * ROW - (listBottom() - listTop()));
	}

	private int buyX() {
		return left + 168;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		GrandLineUi.panel(graphics, font, left, top, panelWidth, panelHeight, title);
		long money = ClientProfileCache.profile() == null ? 0 : ClientProfileCache.profile().currency();
		graphics.enableScissor(left + 2, listTop(), left + panelWidth - 2, listBottom());
		for (int i = 0; i < count(); i++) {
			int y = listTop() + i * ROW - (int) scroll;
			if (y + ROW < listTop() || y > listBottom()) {
				continue;
			}
			Item item = item(i);
			long price = data.prices().get(i);
			boolean owned = price < 0;
			Swords.Spec spec = Swords.spec(item);
			FruitRarity rarity = spec == null ? FruitRarity.COMMON : FruitRarity.valueOf(spec.rarity().toUpperCase(Locale.ROOT));
			graphics.fill(left + 4, y, left + panelWidth - 4, y + ROW - 4, owned ? 0xF0222833 : 0xF0303846);
			GrandLineUi.tile(graphics, left + 8, y + 3, 42, 42, rarity.color(), false);
			graphics.pose().pushPose();
			graphics.pose().translate(left + 13, y + 8, 0);
			graphics.pose().scale(2, 2, 1);
			graphics.renderItem(new ItemStack(item), 0, 0);
			graphics.pose().popPose();

			GrandLineUi.scaled(graphics, font, item.getDescription(), left + 56, y + 4, 1.4f, owned ? 0x9AA3AE : 0xFFFFFF, true);
			graphics.drawString(font, Component.translatable(rarity.translationKey()), left + 56, y + 19, rarity.color());
			if (!owned) {
				graphics.drawString(font, "$" + NUMBERS.format(price), left + 56, y + 31, money >= price ? 0x6BE36B : 0xB05A5A);
			}

			int bx = buyX();
			int by = y + 26;
			if (owned) {
				graphics.drawString(font, Component.translatable("screen.grandline.shop.owned"), bx, by + 4, 0xFFD24B);
			} else {
				boolean affordable = money >= price;
				boolean hover = mouseX >= bx && mouseX < bx + 56 && mouseY >= by && mouseY < by + 16 && mouseY >= listTop() && mouseY < listBottom();
				int face = !affordable ? 0xFF5A6270 : hover ? 0xFFFFD36A : GrandLineUi.GOLD;
				graphics.fill(bx - 1, by - 1, bx + 57, by + 17, GrandLineUi.BORDER);
				graphics.fill(bx, by, bx + 56, by + 16, face);
				graphics.drawCenteredString(font, Component.translatable("screen.grandline.shop.buy"), bx + 28, by + 4, 0xFFFFFF);
			}

			Power power = Gear.powerFor(item);
			if (power != null) {
				int mx = left + panelWidth - 150;
				for (int m = 0; m < power.abilities().size() && m < KEYS.length; m++) {
					AbilityDef ability = power.abilities().get(m);
					int my = y + 6 + m * 14;
					graphics.fill(mx, my, mx + 9, my + 9, 0xFF3A3F4A);
					graphics.drawString(font, KEYS[m], mx + 2, my + 1, 0xFFFFFF);
					graphics.drawString(font, Component.translatable(ability.translationKey()), mx + 13, my + 1, 0xE6EAF0);
					graphics.drawString(font, "★" + ability.requiredMastery(), left + panelWidth - 34, my + 1, 0xFFD24B);
				}
			}
		}
		graphics.disableScissor();

		double max = maxScroll();
		if (max > 0) {
			int trackTop = listTop();
			int trackHeight = listBottom() - listTop();
			int thumb = Math.max(20, trackHeight * trackHeight / (count() * ROW));
			int thumbY = trackTop + (int) ((trackHeight - thumb) * (scroll / max));
			graphics.fill(left + panelWidth - 6, trackTop, left + panelWidth - 3, trackTop + trackHeight, 0x60000000);
			graphics.fill(left + panelWidth - 6, thumbY, left + panelWidth - 3, thumbY + thumb, 0xFFC9D3E0);
		}
		graphics.drawString(font, Component.translatable("screen.grandline.swords.footer"), left + 6, top + panelHeight - 13, 0xC9D3E0);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		scroll = Mth.clamp(scroll - delta * ROW * 0.8, 0, maxScroll());
		return true;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (mouseY >= listTop() && mouseY < listBottom()) {
			for (int i = 0; i < count(); i++) {
				int y = listTop() + i * ROW - (int) scroll;
				int bx = buyX();
				int by = y + 26;
				if (mouseX >= bx && mouseX < bx + 56 && mouseY >= by && mouseY < by + 16 && data.prices().get(i) >= 0) {
					NpcScreens.send(data, NpcActionPayload.BUY_SWORD + i);
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
