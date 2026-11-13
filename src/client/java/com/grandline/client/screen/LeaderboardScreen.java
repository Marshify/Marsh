package com.grandline.client.screen;

import com.grandline.client.ClientReputation;
import com.grandline.client.ui.MarshButton;
import com.grandline.client.ui.MarshUi;
import com.grandline.network.payload.LeaderboardPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Top Bounty (Pirates) and top Honor (Marines) holders, with a tab for each; your own side's tab opens first. */
public final class LeaderboardScreen extends Screen {
	private static final int WIDTH = 260;
	private static final int HEIGHT = 230;
	private boolean bounty;
	private int left;
	private int top;

	public LeaderboardScreen() {
		super(Component.translatable("screen.grandline.leaderboard.title"));
		this.bounty = ClientReputation.state().side() != 1;
	}

	@Override
	protected void init() {
		left = width / 2 - WIDTH / 2;
		top = Math.max(4, height / 2 - HEIGHT / 2);
		addRenderableWidget(new MarshButton(left + 10, top + MarshUi.TITLE_HEIGHT + 6, 116, 18,
				Component.translatable("reputation.grandline.bounty"), MarshUi.Color.RED, () -> bounty = true));
		addRenderableWidget(new MarshButton(left + WIDTH - 126, top + MarshUi.TITLE_HEIGHT + 6, 116, 18,
				Component.translatable("reputation.grandline.honor"), MarshUi.Color.BLUE, () -> bounty = false));
		if (ClientPlayNetworking.canSend(LeaderboardPayload.TYPE)) {
			ClientPlayNetworking.send(new LeaderboardPayload(List.of(), List.of())); // request
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		MarshUi.window(g, font, left, top, WIDTH, HEIGHT, title);
		int y = top + MarshUi.TITLE_HEIGHT + 30;
		Component heading = Component.translatable(bounty ? "screen.grandline.leaderboard.bounty" : "screen.grandline.leaderboard.honor");
		MarshUi.outlined(g, font, heading, left + WIDTH / 2 - font.width(heading) / 2, y, bounty ? 0xFF7070 : 0x7FB0FF);
		y += 14;
		LeaderboardPayload board = ClientReputation.board();
		List<LeaderboardPayload.Row> rows = board == null ? List.of() : bounty ? board.bounty() : board.honor();
		if (rows.isEmpty()) {
			g.drawCenteredString(font, Component.translatable(board == null ? "screen.grandline.leaderboard.loading" : "screen.grandline.leaderboard.empty"),
					left + WIDTH / 2, y + 30, 0xA9B2C0);
		}
		NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.ROOT);
		for (int i = 0; i < rows.size(); i++) {
			LeaderboardPayload.Row row = rows.get(i);
			MarshUi.row(g, left + 8, y, WIDTH - 16, 15, i == 0 ? (bounty ? MarshUi.Row.MELEE : MarshUi.Row.DEFENSE) : MarshUi.Row.DARK);
			g.drawString(font, (i + 1) + ".", left + 14, y + 4, 0xFFD25A, false);
			g.drawString(font, row.name(), left + 34, y + 4, 0xFFFFFF, false);
			String value = "$" + numbers.format(row.value());
			g.drawString(font, value, left + WIDTH - 14 - font.width(value), y + 4, 0x9CFF9C, false);
			y += 16;
		}
		super.render(g, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
