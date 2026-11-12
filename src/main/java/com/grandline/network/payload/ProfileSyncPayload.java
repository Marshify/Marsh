package com.grandline.network.payload;

import com.grandline.GrandLineMod;
import com.grandline.player.PlayerProfile;
import com.grandline.player.ProgressionRules;
import com.grandline.player.StatRules;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server → client: the authoritative profile of the receiving player, plus values derived from the server's config
 * (experience needed for the next level, level cap, stat bonuses) so the client can display them without knowing
 * the server's rules. Display-only; the server never registers a handler for it.
 */
public record ProfileSyncPayload(PlayerProfile profile, long experienceToNextLevel, int maxLevel, int maxPointsPerStat,
		double bonusHealth, double bonusMeleeDamage, double bonusSwordDamage) implements FabricPacket {
	public static final PacketType<ProfileSyncPayload> TYPE = PacketType.create(GrandLineMod.id("profile_sync"), ProfileSyncPayload::read);

	public static ProfileSyncPayload of(PlayerProfile profile, ProgressionRules progression, StatRules stats) {
		return new ProfileSyncPayload(profile, progression.experienceToNextLevel(profile.level()), progression.maxLevel(),
				stats.maxPointsPerStat(), stats.bonusHealth(profile.stats()), stats.bonusMeleeDamage(profile.stats()),
				stats.bonusSwordDamage(profile.stats()));
	}

	private static ProfileSyncPayload read(FriendlyByteBuf buf) {
		return new ProfileSyncPayload(PlayerProfile.read(buf), buf.readVarLong(), buf.readVarInt(), buf.readVarInt(),
				buf.readDouble(), buf.readDouble(), buf.readDouble());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		profile.write(buf);
		buf.writeVarLong(experienceToNextLevel);
		buf.writeVarInt(maxLevel);
		buf.writeVarInt(maxPointsPerStat);
		buf.writeDouble(bonusHealth);
		buf.writeDouble(bonusMeleeDamage);
		buf.writeDouble(bonusSwordDamage);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
