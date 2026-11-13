package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/** Server to client - top Bounty and top Honor holders (request with an empty one; the server answers with lists). */
public record LeaderboardPayload(List<Row> bounty, List<Row> honor) implements FabricPacket {
	public record Row(String name, long value) {
	}

	public static final PacketType<LeaderboardPayload> TYPE = PacketType.create(GrandLineMod.id("leaderboard"),
			buf -> new LeaderboardPayload(readRows(buf), readRows(buf)));

	private static List<Row> readRows(FriendlyByteBuf buf) {
		return buf.readList(b -> new Row(b.readUtf(32), b.readVarLong()));
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeCollection(bounty, (b, r) -> {
			b.writeUtf(r.name(), 32);
			b.writeVarLong(r.value());
		});
		buf.writeCollection(honor, (b, r) -> {
			b.writeUtf(r.name(), 32);
			b.writeVarLong(r.value());
		});
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
