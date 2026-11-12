package com.grandline.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeaChartTest {
	@Test
	void firstSeaFitsInsideTheFrameWithPadding() {
		SeaChart chart = SeaChart.forSea(1);
		for (SeaLayout.Spec spec : SeaLayout.all()) {
			if (spec.sea() != 1) {
				continue;
			}
			double x = chart.toChartX(spec.x());
			double y = chart.toChartY(spec.z());
			assertTrue(x >= SeaChart.INNER_LEFT + SeaChart.PAD_X - 0.5 && x <= SeaChart.INNER_RIGHT - SeaChart.PAD_X + 0.5, spec.name() + " x=" + x);
			assertTrue(y >= SeaChart.INNER_TOP + SeaChart.PAD_TOP - 0.5 && y <= SeaChart.INNER_BOTTOM - SeaChart.PAD_BOTTOM + 0.5, spec.name() + " y=" + y);
		}
	}

	@Test
	void projectionIsUniformAndNorthUp() {
		SeaChart chart = SeaChart.fit(-1500, 1380, -630, 660);
		// Same scale on both axes, larger z is further down (south).
		assertEquals(chart.toChartX(100) - chart.toChartX(0), chart.toChartY(100) - chart.toChartY(0), 1e-9);
		assertTrue(chart.toChartY(660) > chart.toChartY(-630));
		// The player marker uses the same mapping: a point halfway between two islands lands halfway between their anchors.
		assertEquals((chart.toChartX(0) + chart.toChartX(630)) / 2, chart.toChartX(315), 1e-9);
	}
}
