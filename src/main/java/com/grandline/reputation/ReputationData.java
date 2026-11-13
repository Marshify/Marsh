package com.grandline.reputation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * World-wide reputation state (data/grandline_reputation.dat): the PvP pair history used by the anti-farming limit,
 * leaderboard entries (name, Bounty, Honor) for players who are offline, and reputation changes owed to players who
 * were offline when they were credited.
 */
public final class ReputationData extends SavedData {
	public record Entry(String name, long bounty, long honor) {
	}

	public record Pending(long bounty, long honor, String notice) {
	}

	final Map<String, List<Long>> pairs = new HashMap<>();
	final Map<UUID, Entry> board = new HashMap<>();
	final Map<UUID, Pending> pending = new HashMap<>();

	/** Order-independent key for two players. */
	static String pairKey(UUID a, UUID b) {
		return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
	}

	/** Results counted for this pair within the window ending at {@code now} (old entries are dropped). */
	int pairCount(UUID a, UUID b, long now, long windowMillis) {
		List<Long> list = pairs.get(pairKey(a, b));
		if (list == null) {
			return 0;
		}
		list.removeIf(t -> now - t >= windowMillis);
		return list.size();
	}

	void recordPair(UUID a, UUID b, long now) {
		pairs.computeIfAbsent(pairKey(a, b), k -> new ArrayList<>()).add(now);
		setDirty();
	}

	void updateBoard(UUID id, String name, long bounty, long honor) {
		board.put(id, new Entry(name, bounty, honor));
		setDirty();
	}

	void addPending(UUID id, long bounty, long honor, String notice) {
		Pending p = pending.getOrDefault(id, new Pending(0, 0, ""));
		pending.put(id, new Pending(p.bounty() + bounty, p.honor() + honor, notice.isEmpty() ? p.notice() : notice));
		setDirty();
	}

	Pending takePending(UUID id) {
		Pending p = pending.remove(id);
		if (p != null) {
			setDirty();
		}
		return p;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CompoundTag pairTag = new CompoundTag();
		pairs.forEach((k, v) -> {
			ListTag list = new ListTag();
			v.forEach(t -> list.add(LongTag.valueOf(t)));
			pairTag.put(k, list);
		});
		tag.put("pairs", pairTag);
		CompoundTag boardTag = new CompoundTag();
		board.forEach((id, e) -> {
			CompoundTag c = new CompoundTag();
			c.putString("name", e.name());
			c.putLong("bounty", e.bounty());
			c.putLong("honor", e.honor());
			boardTag.put(id.toString(), c);
		});
		tag.put("board", boardTag);
		CompoundTag pendingTag = new CompoundTag();
		pending.forEach((id, p) -> {
			CompoundTag c = new CompoundTag();
			c.putLong("bounty", p.bounty());
			c.putLong("honor", p.honor());
			c.putString("notice", p.notice());
			pendingTag.put(id.toString(), c);
		});
		tag.put("pending", pendingTag);
		return tag;
	}

	static ReputationData load(CompoundTag tag) {
		ReputationData data = new ReputationData();
		CompoundTag pairTag = tag.getCompound("pairs");
		for (String k : pairTag.getAllKeys()) {
			List<Long> list = new ArrayList<>();
			pairTag.getList(k, Tag.TAG_LONG).forEach(t -> list.add(((LongTag) t).getAsLong()));
			data.pairs.put(k, list);
		}
		CompoundTag boardTag = tag.getCompound("board");
		for (String k : boardTag.getAllKeys()) {
			CompoundTag c = boardTag.getCompound(k);
			data.board.put(UUID.fromString(k), new Entry(c.getString("name"), c.getLong("bounty"), c.getLong("honor")));
		}
		CompoundTag pendingTag = tag.getCompound("pending");
		for (String k : pendingTag.getAllKeys()) {
			CompoundTag c = pendingTag.getCompound(k);
			data.pending.put(UUID.fromString(k), new Pending(c.getLong("bounty"), c.getLong("honor"), c.getString("notice")));
		}
		return data;
	}
}
