package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server fruit intents. The server validates each against its own state:
 * <ul>
 *     <li>{@code HELD_EAT / HELD_STORE / HELD_DROP}: act on the fruit item in the given hand ({@code arg} 0 = main hand).</li>
 *     <li>{@code STASH_WITHDRAW}: take one {@code fruitId} out of the stash.</li>
 *     <li>{@code SHOP_BUY}: buy {@code fruitId} from a fruit dealer ({@code arg} = dealer entity id; must be close).</li>
 *     <li>{@code GACHA_ROLL}: roll the gacha at a fruit dealer ({@code arg} = dealer entity id).</li>
 *     <li>{@code REFUND_STATS}: pay to reset all stat points.</li>
 * </ul>
 */
public record FruitActionPayload(int action, String fruitId, int arg) implements FabricPacket {
	public static final int HELD_EAT = 0;
	public static final int HELD_STORE = 1;
	public static final int HELD_DROP = 2;
	public static final int STASH_WITHDRAW = 3;
	public static final int SHOP_BUY = 4;
	public static final int GACHA_ROLL = 5;
	public static final int REFUND_STATS = 6;

	public static final PacketType<FruitActionPayload> TYPE = PacketType.create(GrandLineMod.id("fruit_action"),
			buf -> new FruitActionPayload(buf.readVarInt(), buf.readUtf(256), buf.readVarInt()));

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(action);
		buf.writeUtf(fruitId, 256);
		buf.writeVarInt(arg);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
