package com.grandline.client.screen;

import com.grandline.client.ui.GoldButton;
import com.grandline.client.ui.GrandLineUi;
import com.grandline.fruit.FruitRarity;
import com.grandline.fruit.Fruits;
import com.grandline.network.payload.FruitActionPayload;
import com.grandline.network.payload.OpenNpcPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gacha roll with a spinning reel. The server decides the fruit when "Spin" is pressed; this screen only animates a
 * reel that decelerates and lands on that fruit, then reveals it with a rarity-coloured flash.
 */
public final class GachaScreen extends Screen {
	private static final int TILE = 40;
	private static final int REEL_LENGTH = 42;
	private static final int RESULT_INDEX = 36;
	private static final long SPIN_MILLIS = 4_200;

	/** Set when the server's result arrives; picked up by the open screen. */
	private static GachaScreen current;

	private final OpenNpcPayload data;
	private final List<Fruits.FruitDef> reel = new ArrayList<>();
	private Fruits.FruitDef result;
	private long spinStart;
	private int lastTickIndex = -1;
	private boolean revealed;
	private GoldButton spin;

	public GachaScreen(OpenNpcPayload data) {
		super(Component.translatable("screen.grandline.gacha.title"));
		this.data = data;
	}

	/** Called when the server's {@code GachaResultPayload} arrives. */
	public static void onResult(String fruitId) {
		if (current != null) {
			current.start(Fruits.get(fruitId));
		}
	}

	private void start(Fruits.FruitDef fruit) {
		if (fruit == null) {
			return;
		}
		result = fruit;
		reel.clear();
		Random random = new Random();
		List<Fruits.FruitDef> all = Fruits.all();
		for (int i = 0; i < REEL_LENGTH; i++) {
			reel.add(i == RESULT_INDEX ? fruit : all.get(random.nextInt(all.size())));
		}
		spinStart = System.currentTimeMillis();
		lastTickIndex = -1;
		revealed = false;
	}

	@Override
	protected void init() {
		current = this;
		boolean onCooldown = data.gachaRemainingMillis() > 0;
		spin = addRenderableWidget(new GoldButton(width / 2 - 60, height / 2 + 50, 120, 20,
				onCooldown ? Component.translatable("npc.grandline.dealer.gacha_wait", data.gachaRemainingMillis() / 60_000 + 1)
						: Component.translatable("screen.grandline.gacha.spin", data.gachaCost()),
				b -> {
					FruitDialogs.send(FruitActionPayload.GACHA_ROLL, "", data.entityId());
					b.active = false;
				}));
		spin.active = !onCooldown && result == null;
		addRenderableWidget(new GoldButton(width / 2 + 120, height / 2 - 78, 50, 16, Component.translatable("screen.grandline.back"),
				b -> NpcScreens.open(data)));
	}

	@Override
	public void removed() {
		if (current == this) {
			current = null;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		int panelWidth = 360;
		int left = width / 2 - panelWidth / 2;
		int top = height / 2 - 80;
		GrandLineUi.panel(graphics, font, left, top, panelWidth, 160, title);

		// Rarity legend (odds are by rarity, not by individual fruit).
		int legendX = left + 10;
		for (FruitRarity rarity : FruitRarity.values()) {
			Component name = Component.translatable(rarity.translationKey());
			graphics.drawString(font, name, legendX, top + 26, rarity.color());
			legendX += font.width(name) + 10;
		}

		int windowLeft = left + 10;
		int windowRight = left + panelWidth - 10;
		int reelY = top + 44;
		graphics.fill(windowLeft, reelY, windowRight, reelY + TILE + 8, 0xFF0A1220);
		int centerX = width / 2;

		if (result != null) {
			double t = Math.min(1.0, (System.currentTimeMillis() - spinStart) / (double) SPIN_MILLIS);
			double eased = 1 - Math.pow(1 - t, 3); // ease-out cubic: fast start, slow landing
			double position = eased * RESULT_INDEX; // index currently under the pointer
			int tickIndex = (int) position;
			if (tickIndex != lastTickIndex) {
				lastTickIndex = tickIndex;
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), 1.6F, 0.4F));
			}

			graphics.enableScissor(windowLeft, reelY, windowRight, reelY + TILE + 8);
			for (int i = 0; i < reel.size(); i++) {
				int x = (int) Math.round(centerX - TILE / 2.0 + (i - position) * (TILE + 4));
				if (x < windowLeft - TILE || x > windowRight) {
					continue;
				}
				Fruits.FruitDef fruit = reel.get(i);
				GrandLineUi.tile(graphics, x, reelY + 4, TILE, TILE, fruit.rarity().color(), false);
				graphics.pose().pushPose();
				graphics.pose().translate(x + 4, reelY + 8, 0);
				graphics.pose().scale(2, 2, 1);
				graphics.renderItem(new ItemStack(fruit.item()), 0, 0);
				graphics.pose().popPose();
			}
			graphics.disableScissor();

			if (t >= 1.0 && !revealed) {
				revealed = true;
				boolean big = result.rarity().ordinal() >= FruitRarity.LEGENDARY.ordinal();
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(big ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE
						: SoundEvents.PLAYER_LEVELUP, 1.0F, 0.8F));
			}
			if (revealed) {
				long since = System.currentTimeMillis() - spinStart - SPIN_MILLIS;
				int alpha = (int) Math.max(0, 140 - since / 6);
				graphics.fill(windowLeft, reelY, windowRight, reelY + TILE + 8, (alpha << 24) | result.rarity().color());
				Component won = Component.translatable("screen.grandline.gacha.won",
						Component.translatable(result.power().translationKey()), Component.translatable(result.rarity().translationKey()));
				GrandLineUi.scaled(graphics, font, won, centerX - font.width(won) * 0.65f, reelY + TILE + 16, 1.3f,
						result.rarity().color(), true);
			}
		} else {
			graphics.drawCenteredString(font, Component.translatable("screen.grandline.gacha.hint"), centerX, reelY + 20, 0xA9B4C4);
		}
		// Pointer.
		graphics.fill(centerX - 1, reelY - 4, centerX + 1, reelY + TILE + 12, 0xFFE8B84A);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
