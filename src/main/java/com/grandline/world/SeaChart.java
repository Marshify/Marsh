package com.grandline.world;

/**
 * World-to-chart projection for the sea chart (M key). North is up, one uniform scale on both axes, fitted to the
 * bounding box of a sea's island centres with fixed padding. Island anchors and the player marker both go through
 * {@link #toChartX}/{@link #toChartY}, so the chart follows any change to the world layout automatically.
 *
 * <p>Chart units are pixels of the 1440 × 768 chart background; the screen scales the whole chart afterwards.
 */
public final class SeaChart {
	public static final int WIDTH = 1440;
	public static final int HEIGHT = 768;
	/** The ocean area inside the frame of the background art. */
	public static final int INNER_LEFT = 16;
	public static final int INNER_TOP = 16;
	public static final int INNER_RIGHT = 1424;
	public static final int INNER_BOTTOM = 752;
	/** Space kept between the outermost island centres and the frame (room for the island art). */
	public static final int PAD_X = 140;
	/** More room above than below: island art rises above its anchor, and the compass sits in the top corner. */
	public static final int PAD_TOP = 150;
	public static final int PAD_BOTTOM = 100;

	private final double scale;
	private final double scaleZ;
	private final double originX;
	private final double originY;

	private SeaChart(double scale, double originX, double originY) {
		this(scale, scale, originX, originY);
	}

	private SeaChart(double scaleX, double scaleZ, double originX, double originY) {
		this.scale = scaleX;
		this.scaleZ = scaleZ;
		this.originX = originX;
		this.originY = originY;
	}

	/**
	 * The First Sea chart art places its islands (and the shoreline overlay drawn around them) with this projection,
	 * measured from the art itself. Islands and the player marker both use it, so they stay aligned with the art.
	 * If the First Sea layout changes, the art and these numbers must be updated together.
	 */
	public static final SeaChart FIRST_SEA_ART = new SeaChart(0.38643, 0.38257, 739.63, 391.92);

	/** Fits the given island bounds (block coordinates) into the chart. */
	public static SeaChart fit(double minX, double maxX, double minZ, double maxZ) {
		double spanX = Math.max(1, maxX - minX);
		double spanZ = Math.max(1, maxZ - minZ);
		double scale = Math.min((INNER_RIGHT - INNER_LEFT - PAD_X * 2) / spanX, (INNER_BOTTOM - INNER_TOP - PAD_TOP - PAD_BOTTOM) / spanZ);
		double centerX = (INNER_LEFT + INNER_RIGHT) / 2.0;
		double centerY = (INNER_TOP + PAD_TOP + INNER_BOTTOM - PAD_BOTTOM) / 2.0;
		return new SeaChart(scale, centerX - (minX + maxX) / 2.0 * scale, centerY - (minZ + maxZ) / 2.0 * scale);
	}

	/** Fits every island of {@code sea} in {@link SeaLayout}. */
	public static SeaChart forSea(int sea) {
		double minX = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;
		double maxZ = -Double.MAX_VALUE;
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			if (spec.sea() == sea) {
				minX = Math.min(minX, spec.x());
				maxX = Math.max(maxX, spec.x());
				minZ = Math.min(minZ, spec.z());
				maxZ = Math.max(maxZ, spec.z());
			}
		}
		return fit(minX, maxX, minZ, maxZ);
	}

	public double toChartX(double worldX) {
		return originX + worldX * scale;
	}

	public double toChartY(double worldZ) {
		return originY + worldZ * scaleZ;
	}

	/** Chart pixels per block. */
	public double scale() {
		return scale;
	}
}
