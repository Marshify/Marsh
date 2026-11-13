package com.grandline.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

/** The player's equipped accessory and two rings, as item ids ("" = empty). Saved with the player. */
public record EquippedGear(String accessory, String ring1, String ring2) {
	public static final EquippedGear EMPTY = new EquippedGear("", "", "");
	public static final int SLOT_ACCESSORY = 0;
	public static final int SLOT_RING_1 = 1;
	public static final int SLOT_RING_2 = 2;

	public static final Codec<EquippedGear> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("accessory", "").forGetter(EquippedGear::accessory),
			Codec.STRING.optionalFieldOf("ring1", "").forGetter(EquippedGear::ring1),
			Codec.STRING.optionalFieldOf("ring2", "").forGetter(EquippedGear::ring2)
	).apply(instance, EquippedGear::new));

	public String get(int slot) {
		return switch (slot) {
			case SLOT_ACCESSORY -> accessory;
			case SLOT_RING_1 -> ring1;
			case SLOT_RING_2 -> ring2;
			default -> "";
		};
	}

	public EquippedGear with(int slot, String itemId) {
		return switch (slot) {
			case SLOT_ACCESSORY -> new EquippedGear(itemId, ring1, ring2);
			case SLOT_RING_1 -> new EquippedGear(accessory, itemId, ring2);
			case SLOT_RING_2 -> new EquippedGear(accessory, ring1, itemId);
			default -> this;
		};
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(accessory, 64);
		buf.writeUtf(ring1, 64);
		buf.writeUtf(ring2, 64);
	}

	public static EquippedGear read(FriendlyByteBuf buf) {
		return new EquippedGear(buf.readUtf(64), buf.readUtf(64), buf.readUtf(64));
	}
}
