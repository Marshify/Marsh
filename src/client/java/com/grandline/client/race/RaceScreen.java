package com.grandline.client.race;

import com.grandline.client.ui.MarshButton;
import com.grandline.client.ui.MarshUi;
import com.grandline.network.payload.RaceActionPayload;
import com.grandline.race.Race;
import com.grandline.race.RaceData;
import com.grandline.race.RaceNpcs;
import com.grandline.race.RaceUpgrades;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * The race menu (U, or talking to a race NPC): race and stage, passives, the V3 ability and V4 awakening with their
 * state, the next step's requirements (from the server), the V4 upgrade tree, and the NPC's actions. Values all come
 * from the server; buttons only ask.
 */
public final class RaceScreen extends Screen {
	private static final int W = 380, H = 236;
	private final CompoundTag npc;
	private int left, top;
	private final List<MarshButton> upgradeButtons = new ArrayList<>();
	private final List<MarshButton> npcButtons = new ArrayList<>();
	private boolean upgradesPage;
	private MarshButton pageButton;
	private boolean confirmReroll;

	public RaceScreen(CompoundTag npc) {
		super(Component.translatable("screen.grandline.race.title"));
		this.npc = npc;
	}

	private String role() {
		return npc == null ? "" : npc.getString("role");
	}

	private int npcId() {
		return npc == null ? -1 : npc.getInt("id");
	}

	@Override
	protected void init() {
		left = width / 2 - W / 2;
		top = Math.max(4, height / 2 - H / 2);
		upgradeButtons.clear();
		npcButtons.clear();
		RaceData d = ClientRace.self();
		pageButton = addRenderableWidget(new MarshButton(left + W - 96, top + 30, 86, 16, Component.translatable("screen.grandline.race.upgrades"),
				MarshUi.Color.YELLOW, () -> {
					upgradesPage = !upgradesPage;
					rebuildWidgets();
				}));
		if (upgradesPage && d != null) {
			List<RaceUpgrades.Upgrade> list = RaceUpgrades.forRace(d.kind());
			for (int i = 0; i < list.size(); i++) {
				RaceUpgrades.Upgrade u = list.get(i);
				int col = i % 2, row = u.tier() - 1;
				int index = i;
				upgradeButtons.add(addRenderableWidget(new MarshButton(left + 14 + col * 180, top + 70 + row * 46, 170, 38,
						Component.translatable("race.grandline.upgrade." + u.id()), MarshUi.Color.YELLOW,
						() -> ClientRace.send(RaceActionPayload.UPGRADE, index))));
			}
			return;
		}
		int by = top + H - 28;
		switch (role()) {
			case "race_mystic" -> {
				npcButtons.add(addRenderableWidget(new MarshButton(left + 10, by, 112, 18, Component.translatable("screen.grandline.race.start_quest"),
						MarshUi.Color.YELLOW, () -> ClientRace.send(RaceNpcs.START_QUEST, npcId()))));
				npcButtons.add(addRenderableWidget(new MarshButton(left + 128, by, 112, 18, Component.translatable("screen.grandline.race.evolve"),
						MarshUi.Color.YELLOW, () -> ClientRace.send(RaceNpcs.EVOLVE, npcId()))));
				npcButtons.add(addRenderableWidget(new MarshButton(left + 246, by, 124, 18, Component.empty(), MarshUi.Color.YELLOW, () -> {
					if (confirmReroll) {
						ClientRace.send(RaceNpcs.REROLL, npcId());
						confirmReroll = false;
					} else {
						confirmReroll = true;
					}
				})));
			}
			case "tinkerer", "gravekeeper" -> npcButtons.add(addRenderableWidget(new MarshButton(left + 10, by, 200, 18, Component.empty(),
					MarshUi.Color.YELLOW, () -> ClientRace.send(RaceNpcs.BECOME, npcId()))));
			case "trial_keeper" -> {
				npcButtons.add(addRenderableWidget(new MarshButton(left + 10, by, 150, 18, Component.translatable("screen.grandline.race.trial"),
						MarshUi.Color.YELLOW, () -> {
							ClientRace.send(RaceNpcs.TRIAL, npcId());
							onClose();
						})));
				npcButtons.add(addRenderableWidget(new MarshButton(left + 166, by, 150, 18, Component.translatable("screen.grandline.race.awaken"),
						MarshUi.Color.YELLOW, () -> ClientRace.send(RaceNpcs.EVOLVE, npcId()))));
			}
			default -> { }
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		MarshUi.window(g, font, left, top, W, H, npc == null ? title : Component.translatable("entity.grandline.islander." + role()));
		RaceData d = ClientRace.self();
		if (d == null) {
			g.drawCenteredString(font, Component.translatable("screen.grandline.stats.waiting"), width / 2, top + 60, 0xAAAAAA);
			super.render(g, mouseX, mouseY, partialTick);
			return;
		}
		Race race = d.kind();
		int x = left + 12, y = top + 30;
		g.drawString(font, Component.translatable("race.grandline." + race.id()).withStyle(ChatFormatting.BOLD), x, y, race.color);
		g.drawString(font, "V" + d.stage(), x + font.width(Component.translatable("race.grandline." + race.id()).withStyle(ChatFormatting.BOLD)) + 6, y, 0xFFE8B84A);
		int dy = y + 12;
		for (net.minecraft.util.FormattedCharSequence line : wrap(Component.translatable("race.grandline." + race.id() + ".desc"), W - 130)) {
			g.drawString(font, line, x, dy, 0xB8C2D0);
			dy += 10;
		}
		int body = Math.max(y + 30, dy + 6);
		pageButton.visible = d.stage() >= 4;
		pageButton.setMessage(Component.translatable(upgradesPage ? "screen.grandline.race.back" : "screen.grandline.race.upgrades"));

		if (upgradesPage) {
			renderUpgrades(g, d);
			super.render(g, mouseX, mouseY, partialTick);
			return;
		}

		// Left: passives, V3 and V4.
		int ly = body;
		g.drawString(font, Component.translatable("screen.grandline.race.passives"), x, ly, 0xFFE8B84A);
		ly += 11;
		for (net.minecraft.util.FormattedCharSequence line : wrap(Component.translatable("race.grandline." + race.id() + ".passive." + d.stage()), 172)) {
			g.drawString(font, line, x, ly, 0xDDE3EA);
			ly += 10;
		}
		ly += 4;
		g.drawString(font, Component.translatable("race.grandline." + race.id() + ".v3"), x, ly, d.stage() >= 3 ? 0xFF8FD3FF : 0xFF6A7380);
		g.drawString(font, v3State(d), x + 100, ly, 0xC9D3E0);
		ly += 10;
		for (net.minecraft.util.FormattedCharSequence line : wrap(Component.translatable("race.grandline." + race.id() + ".v3.desc"), 172)) {
			g.drawString(font, line, x, ly, 0x98A2B0);
			ly += 10;
		}
		ly += 4;
		g.drawString(font, Component.translatable("race.grandline." + race.id() + ".v4"), x, ly, d.stage() >= 4 ? 0xFFD08CFF : 0xFF6A7380);
		ly += 11;
		if (d.stage() >= 4) {
			MarshUi.bar(g, x, ly, 150, 8, ClientRace.v4Ticks() > 0 ? ClientRace.v4Ticks() / 400.0 : d.energy() / 100.0, 2);
			g.drawString(font, ClientRace.v4Ticks() > 0 ? Component.translatable("screen.grandline.race.awakened", ClientRace.v4Ticks() / 20)
					: Component.literal(d.energy() + "%"), x + 156, ly, 0xFFFFFF);
		} else {
			for (net.minecraft.util.FormattedCharSequence line : wrap(Component.translatable("race.grandline." + race.id() + ".v4.desc"), 172)) {
				g.drawString(font, line, x, ly, 0x98A2B0);
				ly += 10;
			}
		}

		// Right: what comes next (requirements from the server) or the NPC's own list.
		int rx = left + 200, ry = body;
		ListTag lines = npc != null && npc.contains("lines") ? npc.getList("lines", Tag.TAG_COMPOUND) : ClientRace.view().getList("view", Tag.TAG_COMPOUND);
		Component heading = npc != null && npc.contains("lines")
				? Component.translatable("screen.grandline.race.unlock", Component.translatable("race.grandline." + (role().equals("tinkerer") ? "cyborg" : "ghoul")))
				: d.stage() >= 4 ? Component.translatable("screen.grandline.race.max") : Component.translatable("screen.grandline.race.next", "V" + (d.stage() + 1));
		g.drawString(font, heading, rx, ry, 0xFFE8B84A);
		ry += 12;
		for (int i = 0; i < lines.size(); i++) {
			CompoundTag line = lines.getCompound(i);
			ListTag args = line.getList("a", Tag.TAG_STRING);
			Object[] a = new Object[args.size()];
			for (int j = 0; j < a.length; j++) {
				a[j] = args.getString(j);
			}
			boolean done = line.getBoolean("d");
			MutableComponent text = Component.literal(done ? "✔ " : "✘ ").withStyle(done ? ChatFormatting.GREEN : ChatFormatting.RED)
					.append(Component.translatable(line.getString("k"), a).withStyle(done ? ChatFormatting.GRAY : ChatFormatting.WHITE));
			for (net.minecraft.util.FormattedCharSequence part : wrap(text, 170)) {
				g.drawString(font, part, rx, ry, 0xFFFFFF);
				ry += 10;
			}
			ry += 1;
		}
		if (d.stage() >= 4 && npc == null) {
			g.drawString(font, Component.translatable("screen.grandline.race.points", ClientRace.view().getInt("points")), rx, ry + 4, 0xFFD08CFF);
		}
		if (npc == null) {
			g.drawString(font, Component.translatable("screen.grandline.race.keys"), left + 12, top + H - 16, 0xFF7A8290);
		}

		// NPC buttons.
		if (role().equals("race_mystic") && npcButtons.size() == 3) {
			npcButtons.get(0).active = (d.stage() == 1 && !d.has("q2")) || (d.stage() == 2 && !d.has("q3"));
			npcButtons.get(1).active = d.stage() < 3 && allDone();
			String cost = String.format(java.util.Locale.ROOT, "%,d", npc.getLong("reroll"));
			npcButtons.get(2).setMessage(confirmReroll ? Component.translatable("screen.grandline.race.reroll_confirm", cost)
					: Component.translatable("screen.grandline.race.reroll", cost));
			var profile = com.grandline.client.ClientProfileCache.profile();
			npcButtons.get(2).active = profile != null && profile.currency() >= npc.getLong("reroll");
		}
		if ((role().equals("tinkerer") || role().equals("gravekeeper")) && !npcButtons.isEmpty()) {
			Race target = role().equals("tinkerer") ? Race.CYBORG : Race.GHOUL;
			boolean owned = npc.getBoolean("owned");
			npcButtons.get(0).setMessage(Component.translatable(owned ? "screen.grandline.race.become" : "screen.grandline.race.unlock_button",
					Component.translatable("race.grandline." + target.id())));
			npcButtons.get(0).active = d.kind() != target && npc.getList("lines", Tag.TAG_COMPOUND).stream().allMatch(t -> ((CompoundTag) t).getBoolean("d"));
		}
		if (role().equals("trial_keeper") && npcButtons.size() == 2) {
			npcButtons.get(0).active = d.stage() >= 4 || (d.stage() == 3 && d.has("lore") && d.has("found_keeper"));
			npcButtons.get(1).active = d.stage() == 3 && allDone();
		}
		super.render(g, mouseX, mouseY, partialTick);
	}

	private void renderUpgrades(GuiGraphics g, RaceData d) {
		int points = ClientRace.view().getInt("points");
		g.drawString(font, Component.translatable("screen.grandline.race.points", points), left + 12, top + 56, 0xFFD08CFF);
		List<RaceUpgrades.Upgrade> list = RaceUpgrades.forRace(d.kind());
		for (int i = 0; i < list.size() && i < upgradeButtons.size(); i++) {
			RaceUpgrades.Upgrade u = list.get(i);
			MarshButton b = upgradeButtons.get(i);
			boolean owned = d.upgrades().contains(u.id());
			b.active = !owned && RaceUpgrades.problem(d.withCounter("awaken_points", d.counter("awaken_points")), u.id()) == null;
			b.setMessage(Component.translatable("race.grandline.upgrade." + u.id()).append(owned ? " ✔" : " (" + u.cost() + ")"));
		}
		for (int t = 1; t <= 3; t++) {
			g.drawString(font, Component.translatable("screen.grandline.race.tier", t), left + W - 50, top + 84 + (t - 1) * 46, 0xFF7A8290);
		}
	}

	private boolean allDone() {
		ListTag lines = ClientRace.view().getList("view", Tag.TAG_COMPOUND);
		return !lines.isEmpty() && lines.stream().allMatch(t -> ((CompoundTag) t).getBoolean("d"));
	}

	private Component v3State(RaceData d) {
		if (d.stage() < 3) {
			return Component.translatable("screen.grandline.race.locked", "V3");
		}
		if (ClientRace.v3Ticks() > 0) {
			return Component.translatable("screen.grandline.race.active", ClientRace.v3Ticks() / 20 + 1);
		}
		if (ClientRace.readyTicks() > 0) {
			return Component.translatable("screen.grandline.race.cooldown", ClientRace.readyTicks() / 20 + 1);
		}
		return Component.translatable("screen.grandline.race.ready");
	}

	private List<net.minecraft.util.FormattedCharSequence> wrap(Component text, int width) {
		return font.split(text, width);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
