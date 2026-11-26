package com.grandline.client.fx;

import com.grandline.GrandLineMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom effect meshes for VFX "mesh" layers: Wavefront OBJ files ({@code assets/<ns>/vfx/meshes/<name>.obj}) with
 * positions, optional UVs and triangle or quad faces (larger polygons are fanned). Loaded once, cached until the next
 * resource reload; a broken file logs once and draws nothing.
 */
public final class VfxMeshes {
	/** A face: 4 corners (triangles repeat their last corner), positions xyz and uvs. */
	public record Mesh(List<float[]> positions, List<float[]> uvs, double radius) {
	}

	private static final Map<ResourceLocation, Mesh> CACHE = new HashMap<>();

	private VfxMeshes() {
	}

	static void clear() {
		CACHE.clear();
	}

	public static Mesh get(ResourceLocation id) {
		return CACHE.computeIfAbsent(id, VfxMeshes::load);
	}

	private static Mesh load(ResourceLocation id) {
		List<float[]> v = new ArrayList<>(), vt = new ArrayList<>(), outP = new ArrayList<>(), outU = new ArrayList<>();
		double radius = 0;
		try (BufferedReader r = Minecraft.getInstance().getResourceManager().getResourceOrThrow(id).openAsReader()) {
			String line;
			while ((line = r.readLine()) != null) {
				String[] s = line.trim().split("\\s+");
				switch (s[0]) {
					case "v" -> {
						float[] p = {Float.parseFloat(s[1]), Float.parseFloat(s[2]), Float.parseFloat(s[3])};
						v.add(p);
						radius = Math.max(radius, Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]));
					}
					case "vt" -> vt.add(new float[]{Float.parseFloat(s[1]), 1 - Float.parseFloat(s[2])});
					case "f" -> {
						int n = s.length - 1;
						int[][] idx = new int[n][2];
						for (int i = 0; i < n; i++) {
							String[] parts = s[i + 1].split("/");
							idx[i][0] = index(parts[0], v.size());
							idx[i][1] = parts.length > 1 && !parts[1].isEmpty() ? index(parts[1], vt.size()) : -1;
						}
						for (int k = 1; k + 1 < n; k += 2) { // fan into quads (triangles repeat the last corner)
							int[][] q = {idx[0], idx[k], idx[k + 1], k + 2 < n ? idx[k + 2] : idx[k + 1]};
							float[] pos = new float[12], uv = new float[8];
							for (int c = 0; c < 4; c++) {
								float[] p = v.get(q[c][0]);
								System.arraycopy(p, 0, pos, c * 3, 3);
								float[] t = q[c][1] >= 0 ? vt.get(q[c][1]) : new float[]{c == 1 || c == 2 ? 1 : 0, c >= 2 ? 1 : 0};
								uv[c * 2] = t[0];
								uv[c * 2 + 1] = t[1];
							}
							outP.add(pos);
							outU.add(uv);
						}
					}
					default -> { }
				}
			}
		} catch (Exception e) {
			GrandLineMod.LOGGER.warn("VFX mesh {} failed to load: {}", id, e.toString());
		}
		return new Mesh(outP, outU, radius);
	}

	private static int index(String s, int size) {
		int i = Integer.parseInt(s);
		return i < 0 ? size + i : i - 1;
	}
}
