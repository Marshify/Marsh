package com.grandline.technique;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A player's movement and combat techniques (persistent, kept through death). Dash needs no entry: everyone has it.
 *
 * @param flashStep    Flash Step bought
 * @param aura         Aura bought
 * @param auraXp       Aura experience (the stage follows from it)
 * @param instinct     Instinct bought
 * @param instinctXp   Instinct experience (one per evaded attack; the maximum charges follow from it)
 * @param dodges       Instinct dodge charges left
 * @param regenTicks   ticks played toward the next regenerated charge (only counts while online)
 */
public record Techniques(boolean flashStep, boolean aura, long auraXp, boolean instinct, long instinctXp, int dodges, int regenTicks) {
	public static final Techniques NONE = new Techniques(false, false, 0, false, 0, 0, 0);

	public static final Codec<Techniques> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("flash_step", false).forGetter(Techniques::flashStep),
			Codec.BOOL.optionalFieldOf("aura", false).forGetter(Techniques::aura),
			Codec.LONG.optionalFieldOf("aura_xp", 0L).forGetter(Techniques::auraXp),
			Codec.BOOL.optionalFieldOf("instinct", false).forGetter(Techniques::instinct),
			Codec.LONG.optionalFieldOf("instinct_xp", 0L).forGetter(Techniques::instinctXp),
			Codec.INT.optionalFieldOf("dodges", 0).forGetter(Techniques::dodges),
			Codec.INT.optionalFieldOf("regen_ticks", 0).forGetter(Techniques::regenTicks)
	).apply(i, Techniques::new));

	public Techniques {
		auraXp = Math.max(0, auraXp);
		instinctXp = Math.max(0, instinctXp);
		dodges = Math.max(0, dodges);
		regenTicks = Math.max(0, regenTicks);
	}

	public Techniques withFlashStep(boolean v) {
		return new Techniques(v, aura, auraXp, instinct, instinctXp, dodges, regenTicks);
	}

	public Techniques withAura(boolean v) {
		return new Techniques(flashStep, v, auraXp, instinct, instinctXp, dodges, regenTicks);
	}

	public Techniques withAuraXp(long v) {
		return new Techniques(flashStep, aura, v, instinct, instinctXp, dodges, regenTicks);
	}

	public Techniques withInstinct(boolean v) {
		return new Techniques(flashStep, aura, auraXp, v, instinctXp, dodges, regenTicks);
	}

	public Techniques withInstinctXp(long v) {
		return new Techniques(flashStep, aura, auraXp, instinct, v, dodges, regenTicks);
	}

	public Techniques withDodges(int v, int regen) {
		return new Techniques(flashStep, aura, auraXp, instinct, instinctXp, v, regen);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(flashStep);
		buf.writeBoolean(aura);
		buf.writeVarLong(auraXp);
		buf.writeBoolean(instinct);
		buf.writeVarLong(instinctXp);
		buf.writeVarInt(dodges);
		buf.writeVarInt(regenTicks);
	}

	public static Techniques read(FriendlyByteBuf buf) {
		return new Techniques(buf.readBoolean(), buf.readBoolean(), buf.readVarLong(), buf.readBoolean(), buf.readVarLong(), buf.readVarInt(),
				buf.readVarInt());
	}
}
