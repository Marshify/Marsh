package com.grandline.client.race;

import com.grandline.client.ui.MarshUi;
import com.grandline.race.RaceData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** The race lines in the top-left status block: race and stage, the V3 ability's state, the awakening meter. */
public final class RaceHud {
	private RaceHud() {
	}

	/** Draws at (x, y) within {@code width}; returns the y below what was drawn. */
	public static int render(GuiGraphics g, Font font, int x, int y, int width) {
		RaceData d = ClientRace.self();
		if (d == null) {
			return y;
		}
		Component name = Component.translatable("race.grandline." + d.kind().id()).append(" V" + d.stage());
		MarshUi.outlined(g, font, name, x, y, d.kind().color);
		if (d.stage() >= 3) {
			Component state;
			int color;
			if (ClientRace.v3Ticks() > 0) {
				state = Component.literal(ClientRace.v3Ticks() / 20 + 1 + "s");
				color = 0x7CFC7C;
			} else if (ClientRace.readyTicks() > 0) {
				state = Component.literal(ClientRace.readyTicks() / 20 + 1 + "s");
				color = 0xA0A8B4;
			} else {
				state = Component.translatable("hud.grandline.race.ready");
				color = 0xFFE8B84A;
			}
			Component v3 = Component.literal("[").append(ClientRace.ABILITY.getTranslatedKeyMessage()).append("] ").append(state);
			MarshUi.outlined(g, font, v3, x + width - font.width(v3), y, color);
		}
		y += 11;
		if (d.stage() >= 4) {
			boolean on = ClientRace.v4Ticks() > 0;
			double fill = on ? Math.min(1, ClientRace.v4Ticks() / 600.0) : d.energy() / 100.0;
			MarshUi.bar(g, x, y, width, 8, fill, on ? 0 : 1);
			Component label = on ? Component.translatable("hud.grandline.race.awakened", ClientRace.v4Ticks() / 20 + 1)
					: d.energy() >= 100 ? Component.literal("[").append(ClientRace.AWAKEN.getTranslatedKeyMessage()).append("] ")
							.append(Component.translatable("hud.grandline.race.awaken")).withStyle(ChatFormatting.LIGHT_PURPLE)
					: Component.literal(d.energy() + "%");
			g.pose().pushPose();
			g.pose().translate(x + width / 2f - font.width(label) * 0.35f, y + 1, 0);
			g.pose().scale(0.7f, 0.7f, 1);
			MarshUi.outlined(g, font, label, 0, 0, 0xFFFFFF);
			g.pose().popPose();
			y += 10;
		}
		return y;
	}
}
