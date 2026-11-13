package com.grandline.client.screen;

import com.grandline.GrandLineMod;
import com.grandline.client.ClientProfileCache;
import com.grandline.world.Island;
import com.grandline.world.SeaChart;
import com.grandline.world.SeaLayout;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The sea chart (M key). Layers, back to front: ocean background and compass, island art, drifting edge clouds,
 * player marker with facing cone, title and close button, hover tooltip.
 *
 * <p>Everything except text and the player marker is drawn in chart units (the 1440 × 768 background) and scaled to
 * fit the screen with the background's aspect ratio kept. Island anchors and the player marker use the same
 * {@link SeaChart} projection, fitted to the synced island list of the current sea.
 */
public final class WorldMapScreen extends Screen {
	private static final ResourceLocation BACKGROUND = tex("first_sea/first_sea_background");
	private static final ResourceLocation CLOUD_WIDE = tex("first_sea/cloud_wide");
	private static final ResourceLocation CLOUD_TALL = tex("first_sea/cloud_tall");
	private static final int SPRITE_W = 384;
	private static final int SPRITE_H = 288;
	private static final int OLD_ICON_W = 128;
	private static final int OLD_ICON_H = 96;
	/** Island art size on the chart, in chart pixels per sprite pixel. */
	private static final double SPRITE_SCALE = 0.6;
	/** The chart background is drawn at twice the chart's resolution for sharpness. */
	private static final int BACKGROUND_W = 2880;
	private static final int BACKGROUND_H = 1536;
	private static final ResourceLocation FRAME = tex("first_sea/pirate_frame");
	/**
	 * Each island's piece of the shoreline overlay (split from shorelines_overlay.png so the surf can move with its
	 * island when hovered): theme -> {x, y, width, height} in the 2880 x 1536 art.
	 */
	private static final Map<String, int[]> SHORE_PIECES = Map.ofEntries(
			Map.entry("town", new int[]{1263, 800, 430, 72}),
			Map.entry("jungle", new int[]{1216, 494, 434, 81}),
			Map.entry("village", new int[]{1763, 897, 410, 83}),
			Map.entry("brick_town", new int[]{780, 906, 427, 86}),
			Map.entry("desert", new int[]{1720, 1133, 403, 153}),
			Map.entry("snow", new int[]{1750, 469, 437, 93}),
			Map.entry("fortress", new int[]{2351, 714, 395, 166}),
			Map.entry("prison", new int[]{418, 918, 368, 183}),
			Map.entry("colosseum", new int[]{771, 378, 442, 86}),
			Map.entry("volcano", new int[]{199, 1348, 426, 75}),
			Map.entry("harbor", new int[]{1235, 1236, 446, 85}),
			Map.entry("crystal", new int[]{2214, 1338, 390, 69}));
	private static final ResourceLocation UNDERWATER_SURFACE = tex("first_sea/underwater_surface");
	private static final ResourceLocation PALACE_SUBMERGED = tex("first_sea/palace_submerged");
	private static final ResourceLocation SMOKE = tex("first_sea/volcanic_smoke");
	private static final ResourceLocation DOUBLOONS_SCATTERED = tex("first_sea/doubloons_scattered");
	private static final ResourceLocation DOUBLOONS_STACKED = tex("first_sea/doubloons_stacked");
	/** The submerged palace sits this many chart units below the surface sprite's position. */
	private static final double SUBMERGED_DROP = 13;
	/** Fraction of the sprite height, from the top, that sits on the island's world position. */
	private static final double ANCHOR_Y = 0.6;
	/** Edge clouds: texture, x, y, scale, drift phase (chart units). */
	private static final Object[][] CLOUDS = {
			{CLOUD_WIDE, 547.0, 24.0, 0.47, 0.0},
			{CLOUD_TALL, 29.0, 262.0, 0.36, 2.3},
			{CLOUD_WIDE, 1235.0, 184.0, 0.28, 0.9},
			{CLOUD_TALL, 1314.0, 466.0, 0.21, 5.2},
			{CLOUD_WIDE, 425.0, 655.0, 0.37, 1.7},
	};

	private static final Map<ResourceLocation, Boolean> EXISTS = new HashMap<>();
	private static final Set<ResourceLocation> FILTERED = new HashSet<>();

	/** Hover animation progress (0..1) per island id, eased towards 1 while the cursor is over the island. */
	private final Map<Long, Float> hoverProgress = new HashMap<>();
	/** Island boxes from the previous frame, used to find the hovered island before drawing this one. */
	private List<Placed> lastPlaced = List.of();
	private long lastFrameNanos;
	/** How far a hovered island rises (chart units) and how much it grows. */
	private static final double HOVER_RISE = 7;
	private static final double HOVER_GROW = 0.06;

	private double chartScale;
	private double chartLeft;
	private double chartTop;

	public WorldMapScreen() {
		super(Component.translatable("screen.grandline.map.title"));
	}

	private static ResourceLocation tex(String path) {
		return GrandLineMod.id("textures/gui/map/" + path + ".png");
	}

	private int currentSea() {
		return minecraft.player == null ? 1 : SeaLayout.seaAt(minecraft.player.getX());
	}

	private List<Island> islandsInSea(int sea) {
		List<Island> list = new ArrayList<>();
		boolean anyDesigned = ClientProfileCache.islands().stream().anyMatch(Island::designed);
		for (Island island : ClientProfileCache.islands()) {
			if (island.sea() == sea && (!anyDesigned || island.designed())) {
				list.add(island);
			}
		}
		return list;
	}

	/** The First Sea art pack sprite for this island if there is one, else the generic 128 × 96 theme icon. */
	private ResourceLocation sprite(Island island, int sea) {
		if (sea == 1) {
			ResourceLocation art = tex("first_sea/" + island.theme().toLowerCase(Locale.ROOT));
			if (exists(art)) {
				return art;
			}
		}
		return null;
	}

	private boolean exists(ResourceLocation location) {
		return EXISTS.computeIfAbsent(location, l -> minecraft.getResourceManager().getResource(l).isPresent());
	}

	/** Large art is shown much smaller than its source, so sample it smoothly instead of nearest-pixel. */
	private void smooth(ResourceLocation location) {
		if (FILTERED.add(location)) {
			minecraft.getTextureManager().getTexture(location).setFilter(true, false);
		}
	}

	private int screenX(double chartX) {
		return (int) Math.round(chartLeft + chartX * chartScale);
	}

	private int screenY(double chartY) {
		return (int) Math.round(chartTop + chartY * chartScale);
	}

	private void blitChart(GuiGraphics graphics, ResourceLocation texture, double x, double y, double scale, int w, int h) {
		smooth(texture);
		graphics.pose().pushPose();
		graphics.pose().translate(chartLeft + x * chartScale, chartTop + y * chartScale, 0);
		graphics.pose().scale((float) (scale * chartScale), (float) (scale * chartScale), 1);
		graphics.blit(texture, 0, 0, w, h, 0, 0, w, h, w, h);
		graphics.pose().popPose();
	}

	/** Like {@link #blitChart} but with nearest-pixel sampling, for small pixel-art sprites. */
	private void blitCrisp(GuiGraphics graphics, ResourceLocation texture, double x, double y, double scale, int w, int h) {
		graphics.pose().pushPose();
		graphics.pose().translate(chartLeft + x * chartScale, chartTop + y * chartScale, 0);
		graphics.pose().scale((float) (scale * chartScale), (float) (scale * chartScale), 1);
		graphics.blit(texture, 0, 0, w, h, 0, 0, w, h, w, h);
		graphics.pose().popPose();
	}

	/** One island as placed on the chart: anchor and the art's on-screen box. */
	private record Placed(Island island, double ax, double ay, int left, int top, int right, int bottom) {
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		chartScale = Math.min((width - 12) / (double) SeaChart.WIDTH, (height - 12) / (double) SeaChart.HEIGHT);
		chartLeft = (width - SeaChart.WIDTH * chartScale) / 2.0;
		chartTop = (height - SeaChart.HEIGHT * chartScale) / 2.0;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		// Layer 1: ocean, grid, frame and compass.
		blitChart(graphics, BACKGROUND, 0, 0, SeaChart.WIDTH / (double) BACKGROUND_W, BACKGROUND_W, BACKGROUND_H);
		drawCompass(graphics, 1336, 100);

		int sea = currentSea();
		List<Island> islands = islandsInSea(sea);
		float textScale = (float) Mth.clamp(chartScale * 1.6, 0.5, 1.0);
		if (islands.isEmpty()) {
			graphics.drawCenteredString(font, Component.translatable("screen.grandline.map.empty"), width / 2, height / 2, 0xFFFFFF);
			drawChrome(graphics, sea, textScale);
			super.render(graphics, mouseX, mouseY, partialTick);
			return;
		}

		double minX = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;
		double maxZ = -Double.MAX_VALUE;
		for (Island island : islands) {
			minX = Math.min(minX, island.x());
			maxX = Math.max(maxX, island.x());
			minZ = Math.min(minZ, island.z());
			maxZ = Math.max(maxZ, island.z());
		}
		// The First Sea art was drawn for a measured projection; other seas are fitted to their islands.
		SeaChart chart = sea == 1 ? SeaChart.FIRST_SEA_ART : SeaChart.fit(minX, maxX, minZ, maxZ);
		boolean firstSeaArt = sea == 1 && exists(FRAME);

		// Hover animation: find the island under the cursor (from last frame's boxes) and ease every island's progress.
		long now = System.nanoTime();
		float dt = lastFrameNanos == 0 ? 0 : Math.min(0.1f, (now - lastFrameNanos) / 1.0e9f);
		lastFrameNanos = now;
		Placed hoveredBox = hitTest(lastPlaced, mouseX, mouseY);
		for (Island island : islands) {
			float target = hoveredBox != null && hoveredBox.island().id() == island.id() ? 1f : 0f;
			float current = hoverProgress.getOrDefault(island.id(), 0f);
			current += (target - current) * Math.min(1f, dt * 10f);
			hoverProgress.put(island.id(), current);
		}

		// Layer 2: island art, drawn north to south so nearer islands overlap the ones behind them.
		List<Island> ordered = new ArrayList<>(islands);
		ordered.sort((a, b) -> Integer.compare(a.z(), b.z()));
		List<Placed> placed = new ArrayList<>();
		for (Island island : ordered) {
			double ax = chart.toChartX(island.x());
			double ay = chart.toChartY(island.z());
			ResourceLocation art = sprite(island, sea);
			double w = SPRITE_W * SPRITE_SCALE;
			double h = SPRITE_H * SPRITE_SCALE;
			float t = hoverProgress.getOrDefault(island.id(), 0f);
			double eased = t * t * (3 - 2 * t);
			double grow = 1 + HOVER_GROW * eased;
			double rise = HOVER_RISE * eased;
			if (art != null && firstSeaArt && island.theme().equalsIgnoreCase("palace")) {
				// Lotus Palace lies under the sea: the submerged rendering, then the water surface over it.
				blitChart(graphics, PALACE_SUBMERGED, ax - w * grow / 2, ay - h * grow * ANCHOR_Y + SUBMERGED_DROP - rise,
						SPRITE_SCALE * grow, SPRITE_W, SPRITE_H);
			} else if (art != null) {
				// Grows around the island's base point and lifts gently; its surf ring moves with it.
				blitChart(graphics, art, ax - w * grow / 2, ay - h * grow * ANCHOR_Y - rise, SPRITE_SCALE * grow, SPRITE_W, SPRITE_H);
				int[] shore = firstSeaArt ? SHORE_PIECES.get(island.theme().toLowerCase(Locale.ROOT)) : null;
				if (shore != null) {
					double half = SeaChart.WIDTH / (double) BACKGROUND_W; // art pixels -> chart units
					double px = ax + (shore[0] * half - ax) * grow;
					double py = ay + (shore[1] * half - ay) * grow - rise;
					blitChart(graphics, tex("first_sea/shore_" + island.theme().toLowerCase(Locale.ROOT)), px, py, half * grow, shore[2], shore[3]);
				}
			} else if (sea == 1) {
				// No illustration in the First Sea art pack: a small sand-and-grass islet that fits the chart style.
				int sx = screenX(ax);
				int sy = screenY(ay);
				int r = Math.max(3, (int) Math.round(22 * chartScale));
				fillCircle(graphics, sx, sy + 1, r + 1, 0x60000000);
				fillCircle(graphics, sx, sy, r, 0xFFE3C98A);
				fillCircle(graphics, sx, sy - 1, Math.max(2, r * 2 / 3), 0xFF5FA84A);
				w = r * 2.6 / chartScale;
				h = r * 2.6 / chartScale;
			} else {
				// Other seas have no illustrated art yet: the generic theme icon (smaller islets shrink).
				double size = island.radius() < 25 ? 0.65 : 1.0;
				w *= size;
				h *= size;
				blitChart(graphics, tex(island.theme().toLowerCase(Locale.ROOT)), ax - w / 2, ay - h * ANCHOR_Y,
						w / OLD_ICON_W, OLD_ICON_W, OLD_ICON_H);
			}
			placed.add(new Placed(island, ax, ay, screenX(ax - w * 0.4), screenY(ay - h * ANCHOR_Y + h * 0.08),
					screenX(ax + w * 0.4), screenY(ay + h * (1 - ANCHOR_Y) - h * 0.1)));
		}

		double time = (System.currentTimeMillis() % 600_000L) / 1000.0;
		if (firstSeaArt) {
			// Volcanic smoke above Emberfall: rises and fades in a slow loop.
			double phase = (time * 0.35) % 1.0;
			double smokeScale = Math.min(91.0 / 98, 45.0 / 58) * (0.95 + 0.1 * phase);
			graphics.setColor(1, 1, 1, (float) (1.0 - 0.35 * phase));
			blitChart(graphics, SMOKE, 156 - 98 * smokeScale / 2, 542 - 58 * smokeScale / 2 - phase * 6, smokeScale, 98, 58);
			graphics.setColor(1, 1, 1, 1);
		}

		// Layer 3: clouds along the edges, drifting gently, clipped to the ocean.
		graphics.enableScissor(screenX(SeaChart.INNER_LEFT), screenY(SeaChart.INNER_TOP), screenX(SeaChart.INNER_RIGHT), screenY(SeaChart.INNER_BOTTOM));
		for (Object[] cloud : CLOUDS) {
			double drift = Math.sin(time * 0.25 + (double) cloud[4]) * 8;
			boolean wide = cloud[0] == CLOUD_WIDE;
			blitChart(graphics, (ResourceLocation) cloud[0], (double) cloud[1] + drift, (double) cloud[2], (double) cloud[3],
					wide ? 640 : 520, wide ? 240 : 252);
		}
		graphics.disableScissor();

		if (firstSeaArt) {
			// Pirate frame over the sea, doubloons on its bottom corners (pixel art, kept crisp).
			blitChart(graphics, FRAME, 0, 0, SeaChart.WIDTH / (double) BACKGROUND_W, BACKGROUND_W, BACKGROUND_H);
			blitCrisp(graphics, DOUBLOONS_SCATTERED, 2, 678, 2.0, 54, 38);
			blitCrisp(graphics, DOUBLOONS_STACKED, 1320, 680, 2.0, 60, 40);
		}

		int playerLevel = ClientProfileCache.profile() == null ? 1 : ClientProfileCache.profile().level();

		// Layer 5: player marker (face in a gold ring) with a facing cone, both from the same projection.
		if (minecraft.player != null) {
			int px = screenX(chart.toChartX(minecraft.player.getX()));
			int py = screenY(chart.toChartY(minecraft.player.getZ()));
			graphics.enableScissor(screenX(SeaChart.INNER_LEFT), screenY(SeaChart.INNER_TOP), screenX(SeaChart.INNER_RIGHT), screenY(SeaChart.INNER_BOTTOM));
			drawPlayer(graphics, px, py, minecraft.player.getYRot(), textScale);
			graphics.disableScissor();
		}

		drawChrome(graphics, sea, textScale);

		// Hover: island name and level.
		lastPlaced = placed;
		Placed hovered = hitTest(placed, mouseX, mouseY);
		super.render(graphics, mouseX, mouseY, partialTick);
		if (hovered != null) {
			Island island = hovered.island();
			int color = island.level() <= playerLevel ? 0x9CFF9C : island.level() <= playerLevel + 50 ? 0xFFE080 : 0xFF9C9C;
			List<Component> lines = new ArrayList<>(List.of(Component.literal(island.name()),
					Component.translatable("screen.grandline.map.level", island.level()).withStyle(s -> s.withColor(color))));
			if (minecraft.player != null && minecraft.player.isCreative()) {
				lines.add(Component.translatable("screen.grandline.map.teleport").withStyle(net.minecraft.ChatFormatting.AQUA));
			}
			graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
		}
	}

	/** Title plate (top left) and close button (top right). */
	private void drawChrome(GuiGraphics graphics, int sea, float textScale) {
		Component title = Component.translatable("screen.grandline.map.sea_name." + sea);
		String text = title.getString().toUpperCase(Locale.ROOT);
		graphics.pose().pushPose();
		graphics.pose().translate(screenX(43), screenY(45), 0);
		graphics.pose().scale(textScale, textScale, 1);
		int w = font.width(text) + 20;
		graphics.fill(-2, -2, w + 2, 17, 0xFF4A2E14);
		graphics.fill(0, 0, w, 15, 0xFFE3C48C);
		graphics.fill(0, 13, w, 15, 0xFFB8925A);
		graphics.drawString(font, text, 10, 4, 0x3A2410, false);
		graphics.pose().popPose();

		int[] close = closeBox();
		graphics.fill(close[0] - 1, close[1] - 1, close[2] + 1, close[3] + 1, 0xFFFFFFFF);
		graphics.fill(close[0], close[1], close[2], close[3], 0xFFE0453A);
		graphics.drawCenteredString(font, "✕", (close[0] + close[2]) / 2 + 1, (close[1] + close[3]) / 2 - 4, 0xFFFFFF);
	}

	private int[] closeBox() {
		int size = Math.max(12, (int) Math.round(30 * chartScale));
		int right = screenX(1401);
		int top = screenY(34);
		return new int[]{right - size, top, right, top + size};
	}

	/** Compass rose (the background art has none): N-S and E-W points, small diagonals, letters. */
	private void drawCompass(GuiGraphics graphics, double chartX, double chartY) {
		int cx = screenX(chartX);
		int cy = screenY(chartY);
		int r = Math.max(8, (int) Math.round(38 * chartScale));
		int w = Math.max(2, r / 6);
		for (int i = -r; i <= r; i++) {
			int half = Math.round(w * (1 - Math.abs(i) / (float) r));
			graphics.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, 0xE6FFFFFF);
			graphics.fill(cx + i, cy - half, cx + i + 1, cy + half + 1, 0xE6FFFFFF);
		}
		int d = r * 2 / 3;
		for (int i = -d; i <= d; i++) {
			graphics.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, 0x99FFFFFF);
			graphics.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, 0x99FFFFFF);
		}
		float scale = (float) Mth.clamp(chartScale * 1.2, 0.5, 1.0);
		String[] letters = {"N", "E", "S", "W"};
		int[][] offsets = {{0, -r - 7}, {r + 6, 0}, {0, r + 7}, {-r - 6, 0}};
		for (int i = 0; i < 4; i++) {
			graphics.pose().pushPose();
			graphics.pose().translate(cx + offsets[i][0], cy + offsets[i][1] - 4 * scale, 0);
			graphics.pose().scale(scale, scale, 1);
			graphics.drawCenteredString(font, letters[i], 0, 0, 0xFFFFFF);
			graphics.pose().popPose();
		}
	}

	private void drawPlayer(GuiGraphics graphics, int x, int y, float yaw, float scale) {
		float s = Math.max(0.6f, scale);
		// Facing cone: yaw 0 faces +Z (south, down on the chart); rotating the pose turns it with the player.
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().mulPose(Axis.ZP.rotationDegrees(yaw));
		graphics.pose().scale(s, s, 1);
		for (int i = 6; i < 52; i++) {
			int half = 2 + i * 12 / 20;
			int alpha = (int) (190 * (1 - (i - 6) / 46.0));
			graphics.fill(-half, i, half + 1, i + 1, (alpha << 24) | 0xFFE65A);
		}
		graphics.pose().popPose();

		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(s, s, 1);
		fillCircle(graphics, 0, 0, 13, 0xFF2A1A08);
		fillCircle(graphics, 0, 0, 12, 0xFFE8B83A);
		fillCircle(graphics, 0, 0, 10, 0xFF1C2C40);
		PlayerFaceRenderer.draw(graphics, minecraft.player.getSkinTextureLocation(), -6, -6, 12);
		graphics.pose().popPose();
	}

	/** The island whose box contains the cursor (the one whose anchor is closest, where boxes overlap). */
	private Placed hitTest(List<Placed> boxes, int mouseX, int mouseY) {
		Placed hovered = null;
		double best = Double.MAX_VALUE;
		for (Placed p : boxes) {
			if (mouseX >= p.left() && mouseX <= p.right() && mouseY >= p.top() && mouseY <= p.bottom()) {
				double d = Math.hypot(mouseX - screenX(p.ax()), mouseY - screenY(p.ay()));
				if (d < best) {
					best = d;
					hovered = p;
				}
			}
		}
		return hovered;
	}

	private static void fillCircle(GuiGraphics graphics, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int half = (int) Math.round(Math.sqrt(r * r - dy * dy + 0.25 * r));
			graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// Creative only: click an island to teleport there (the server re-checks the game mode).
		if (button == 0 && minecraft.player != null && minecraft.player.isCreative()) {
			Placed hit = hitTest(lastPlaced, (int) mouseX, (int) mouseY);
			if (hit != null && net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(
					com.grandline.network.payload.MapTeleportPayload.TYPE)) {
				net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
						new com.grandline.network.payload.MapTeleportPayload(hit.island().id()));
				onClose();
				return true;
			}
		}
		int[] close = closeBox();
		if (button == 0 && mouseX >= close[0] && mouseX <= close[2] && mouseY >= close[1] && mouseY <= close[3]) {
			onClose();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	/** The map key (M by default) closes the map the same way it opens it. */
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (com.grandline.client.GrandLineKeys.MAP.matches(keyCode, scanCode)) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
