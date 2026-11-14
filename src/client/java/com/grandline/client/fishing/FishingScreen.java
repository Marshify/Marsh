package com.grandline.client.fishing;

import com.grandline.client.ClientFishing;
import com.grandline.network.payload.FishingEndPayload;
import com.grandline.network.payload.FishingInputPayload;
import com.grandline.network.payload.FishingStartPayload;
import com.grandline.network.payload.FishingStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The catch minigame, drawn in Grand Line's navy-and-gold style beside the screen centre: a tall water bar where the
 * fish darts up and down, the green catch net the player lifts by holding the mouse or Space (it sinks when released),
 * and a gold reel meter that fills while the fish is in the net and drains while it is out. The server runs the fight;
 * this screen sends hold/release and draws the server's state, smoothed between ticks. Escape lets the fish go.
 */
public class FishingScreen extends Screen {
	private static final int BAR_W = 28, BAR_H = 150;
	private final FishingStartPayload start;
	private final ItemStack fishIcon;
	private float fish = 0.5f, zone = 0.3f, progress = 0.3f;
	private float prevFish = 0.5f, prevZone = 0.3f, prevProgress = 0.3f;
	private boolean inZone;
	private long lastState = System.currentTimeMillis();
	private boolean holdingMouse, holdingKey, sentHolding;
	private FishingEndPayload result;
	private long resultAt;
	private int ticks;

	public FishingScreen(FishingStartPayload start) {
		super(Component.translatable("screen.grandline.fishing.title"));
		this.start = start;
		this.fishIcon = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(start.item())));
	}

	public void update(FishingStatePayload state) {
		prevFish = fish;
		prevZone = zone;
		prevProgress = progress;
		fish = state.fish();
		zone = state.zone();
		progress = state.progress();
		inZone = state.inZone();
		lastState = System.currentTimeMillis();
	}

	public void finish(FishingEndPayload end) {
		result = end;
		resultAt = System.currentTimeMillis();
		if (end.success()) {
			progress = 1;
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.6F, 0.4F));
		} else {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.FISHING_BOBBER_SPLASH, 0.8F, 0.7F));
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void sendInput(boolean giveUp) {
		boolean holding = holdingMouse || holdingKey;
		if ((holding != sentHolding || giveUp) && ClientPlayNetworking.canSend(FishingInputPayload.TYPE)) {
			ClientPlayNetworking.send(new FishingInputPayload(holding, giveUp));
			sentHolding = holding;
		}
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		holdingMouse = true;
		sendInput(false);
		return true;
	}

	@Override
	public boolean mouseReleased(double x, double y, int button) {
		holdingMouse = false;
		sendInput(false);
		return true;
	}

	@Override
	public boolean keyPressed(int key, int scan, int modifiers) {
		if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) {
			holdingKey = true;
			sendInput(false);
			return true;
		}
		return super.keyPressed(key, scan, modifiers);
	}

	@Override
	public boolean keyReleased(int key, int scan, int modifiers) {
		if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) {
			holdingKey = false;
			sendInput(false);
			return true;
		}
		return super.keyReleased(key, scan, modifiers);
	}

	@Override
	public void onClose() {
		if (result == null) {
			sendInput(true); // Escape: let the fish go
		}
		super.onClose();
	}

	@Override
	public void tick() {
		ticks++;
		if ((holdingMouse || holdingKey) && result == null && ticks % 3 == 0) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WOODEN_BUTTON_CLICK_ON, 1.4F + (ticks % 6) * 0.05F, 0.25F));
		}
		if (result != null && System.currentTimeMillis() - resultAt > 1600) {
			minecraft.setScreen(null);
		}
	}

	private static int argb(int rgb, float a) {
		return (Math.round(Mth.clamp(a, 0, 1) * 255) << 24) | rgb;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		float k = Mth.clamp((System.currentTimeMillis() - lastState) / 50f, 0, 1);
		float f = Mth.lerp(k, prevFish, fish), z = Mth.lerp(k, prevZone, zone), p = Mth.lerp(k, prevProgress, progress);
		int cx = width / 2 + 70, top = height / 2 - BAR_H / 2;
		int left = cx - BAR_W / 2;
		// Panel: shadow, gold frame, navy body.
		int pw = BAR_W + 52, px = left - 8;
		g.fill(px + 3, top - 30 + 3, px + pw + 3, top + BAR_H + 26 + 3, 0x66000000);
		g.fill(px - 1, top - 31, px + pw + 1, top + BAR_H + 27, 0xFFE8C25A);
		g.fillGradient(px, top - 30, px + pw, top + BAR_H + 26, 0xF222304A, 0xF2162034);
		Component title = this.title;
		g.drawCenteredString(font, title, px + pw / 2, top - 24, 0xFFD866);
		Component tier = Component.translatable("fishing.grandline.tier." + start.tier());
		int tierColor = switch (start.tier()) { case 1 -> 0x6FE06F; case 2 -> 0x58A8FF; case 3 -> 0xC070FF; case 4 -> 0xFFB43C; default -> 0xD8D8D8; };
		g.drawCenteredString(font, tier, px + pw / 2, top - 13, tierColor);
		// Water column.
		g.fill(left - 1, top - 1, left + BAR_W + 1, top + BAR_H + 1, 0xFF0B1220);
		g.fillGradient(left, top, left + BAR_W, top + BAR_H, 0xFF2B7FC9, 0xFF0E3463);
		for (int i = 0; i < 6; i++) { // drifting light bands
			int y = top + (int) ((ticks * 0.6f + i * 27) % BAR_H);
			g.fill(left + 2, y, left + BAR_W - 2, y + 1, 0x22FFFFFF);
		}
		// Catch net.
		float half = start.zone() / 2f;
		int zTop = top + Math.round((1 - z - half) * BAR_H), zBot = top + Math.round((1 - z + half) * BAR_H);
		int net = inZone ? 0x8C6BFF8A : 0x6648C060;
		g.fill(left + 1, zTop, left + BAR_W - 1, zBot, net);
		g.fill(left + 1, zTop, left + BAR_W - 1, zTop + 1, inZone ? 0xFFB8FFC8 : 0xFF7FD890);
		g.fill(left + 1, zBot - 1, left + BAR_W - 1, zBot, inZone ? 0xFFB8FFC8 : 0xFF7FD890);
		// The fish (its item icon), wiggling.
		int fy = top + Math.round((1 - f) * BAR_H) - 8;
		float wig = Mth.sin(ticks * 0.9f + partial) * 1.5f;
		g.pose().pushPose();
		g.pose().translate(left + BAR_W / 2f - 8 + wig, fy, 0);
		g.renderItem(fishIcon, 0, 0);
		g.pose().popPose();
		// Reel meter.
		int mx = left + BAR_W + 8, mw = 8;
		g.fill(mx - 1, top - 1, mx + mw + 1, top + BAR_H + 1, 0xFF0B1220);
		g.fill(mx, top, mx + mw, top + BAR_H, 0xFF2A2F3C);
		int fillTop = top + Math.round((1 - p) * BAR_H);
		int meter = p > 0.66f ? 0xFF6BE07F : p > 0.33f ? 0xFFF2C04A : 0xFFE0564A;
		g.fill(mx, fillTop, mx + mw, top + BAR_H, meter);
		g.fill(mx, fillTop, mx + mw, fillTop + 1, 0xFFFFFFFF);
		// Mastery and hint.
		var m = ClientFishing.mastery();
		Component mastery = Component.translatable("screen.grandline.fishing.mastery", m.level());
		g.drawCenteredString(font, mastery, px + pw / 2, top + BAR_H + 5, 0x9CD8FF);
		int xpW = pw - 16;
		g.fill(px + 8, top + BAR_H + 16, px + 8 + xpW, top + BAR_H + 19, 0xFF2A2F3C);
		g.fill(px + 8, top + BAR_H + 16, px + 8 + Math.round(xpW * Math.min(1f, m.xp() / (float) Math.max(1, m.next()))), top + BAR_H + 19, 0xFF58A8FF);
		g.pose().pushPose();
		g.pose().scale(0.75f, 0.75f, 1);
		Component hint = Component.translatable("screen.grandline.fishing.hint");
		g.drawCenteredString(font, hint, Math.round((px + pw / 2f) / 0.75f), Math.round((top - 44) / 0.75f), 0xE6E6E6);
		g.pose().popPose();
		// Result.
		if (result != null) {
			float t = (System.currentTimeMillis() - resultAt) / 1600f;
			Component text = Component.translatable(result.success() ? "screen.grandline.fishing.caught" : "screen.grandline.fishing.lost");
			float s = 1.6f + (1 - Math.min(1, t * 4)) * 0.6f;
			g.pose().pushPose();
			g.pose().translate(width / 2f - 70, height / 2f - 10, 0);
			g.pose().scale(s, s, 1);
			g.drawCenteredString(font, text, 0, 0, argb(result.success() ? 0x7CFF8C : 0xFF8C7C, 1 - Math.max(0, t - 0.7f) / 0.3f));
			g.pose().popPose();
			if (result.success()) {
				g.pose().pushPose();
				g.pose().translate(width / 2f - 70 - 16, height / 2f + 6, 0);
				g.pose().scale(2, 2, 1);
				g.renderItem(fishIcon, 0, 0);
				g.pose().popPose();
				g.drawCenteredString(font, Component.literal("+" + result.xp() + " Fishing Mastery"), width / 2 - 70, height / 2 + 42, 0x9CD8FF);
				if (result.levelUp()) {
					g.drawCenteredString(font, Component.translatable("screen.grandline.fishing.level_up"), width / 2 - 70, height / 2 + 54, 0xFFD866);
				}
			}
		}
	}
}
