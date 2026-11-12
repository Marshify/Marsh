package com.grandline.client.screen;

import com.grandline.npc.QuestState;
import net.minecraft.network.chat.Component;

/** Public access to quest descriptions for the HUD. */
public final class NpcScreensAccess {
	private NpcScreensAccess() {
	}

	public static Component describe(QuestState quest) {
		return NpcScreens.quest(quest);
	}
}
