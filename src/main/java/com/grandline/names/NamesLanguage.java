package com.grandline.names;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The game's language with the renamed names on top (see {@link GrandLineNames}): a renamed key returns its new name;
 * any other text has renamed island names swapped in. Everything else is the wrapped language, untouched. A new instance
 * is made whenever the names change, which also makes Minecraft re-translate text it had cached.
 */
public final class NamesLanguage extends Language {
	private final Language base;
	private final Map<String, String> keys;
	private final Map<String, String> islands;
	private final Map<String, String> cache = new ConcurrentHashMap<>();

	NamesLanguage(Language base, GrandLineNames.Active active) {
		this.base = base;
		this.keys = active.keys();
		this.islands = active.islands();
	}

	/** The language with the current names on top ({@code base} itself if it already is one). */
	public static Language wrap(Language base) {
		Language plain = base instanceof NamesLanguage names ? names.base : base;
		return new NamesLanguage(plain, GrandLineNames.active());
	}

	public Language base() {
		return base;
	}

	@Override
	public String getOrDefault(String key, String fallback) {
		String renamed = keys.get(key);
		if (renamed != null) {
			return renamed;
		}
		String text = base.getOrDefault(key, fallback);
		if (islands.isEmpty() || text == null) {
			return text;
		}
		if (text.equals(fallback) && !base.has(key)) {
			return text; // not a translation: leave it alone (and do not fill the cache with it)
		}
		return cache.computeIfAbsent(key, k -> GrandLineNames.replaceIslands(text, islands));
	}

	@Override
	public boolean has(String key) {
		return keys.containsKey(key) || base.has(key);
	}

	@Override
	public boolean isDefaultRightToLeft() {
		return base.isDefaultRightToLeft();
	}

	@Override
	public FormattedCharSequence getVisualOrder(FormattedText text) {
		return base.getVisualOrder(text);
	}

	@Override
	public List<FormattedCharSequence> getVisualOrder(List<FormattedText> texts) {
		return base.getVisualOrder(texts);
	}
}
