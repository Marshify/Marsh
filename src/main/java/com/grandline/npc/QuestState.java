package com.grandline.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

/**
 * The player's active quest (one at a time). {@code target} is an {@link com.grandline.entity.EnemyKind} name;
 * {@code x}/{@code z} point at the quest island for the HUD waypoint.
 */
public record QuestState(String target, String islandName, int required, int progress, long rewardExperience, long rewardCurrency,
		int x, int z, boolean boss) {
	public static final Codec<QuestState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("target").forGetter(QuestState::target),
			Codec.STRING.fieldOf("island").forGetter(QuestState::islandName),
			Codec.INT.fieldOf("required").forGetter(QuestState::required),
			Codec.INT.fieldOf("progress").forGetter(QuestState::progress),
			Codec.LONG.fieldOf("reward_xp").forGetter(QuestState::rewardExperience),
			Codec.LONG.fieldOf("reward_currency").forGetter(QuestState::rewardCurrency),
			Codec.INT.fieldOf("x").forGetter(QuestState::x),
			Codec.INT.fieldOf("z").forGetter(QuestState::z),
			Codec.BOOL.optionalFieldOf("boss", false).forGetter(QuestState::boss)
	).apply(instance, QuestState::new));

	public QuestState withProgress(int newProgress) {
		return new QuestState(target, islandName, required, newProgress, rewardExperience, rewardCurrency, x, z, boss);
	}

	public boolean complete() {
		return progress >= required;
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(target, 64);
		buf.writeUtf(islandName, 64);
		buf.writeVarInt(required);
		buf.writeVarInt(progress);
		buf.writeVarLong(rewardExperience);
		buf.writeVarLong(rewardCurrency);
		buf.writeVarInt(x);
		buf.writeVarInt(z);
		buf.writeBoolean(boss);
	}

	public static QuestState read(FriendlyByteBuf buf) {
		return new QuestState(buf.readUtf(64), buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), buf.readVarLong(),
				buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
	}
}
