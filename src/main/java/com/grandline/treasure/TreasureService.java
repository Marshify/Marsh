package com.grandline.treasure;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.player.PlayerProfileService;
import com.grandline.world.FirstSeaProgression;
import com.grandline.world.Island;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Opening treasure chests. Each chest is per player: opening it pays that player Beli and starts that player's refill
 * timer for that chest; everyone else can still open it. The reward follows the island's quest reward (so chests stay
 * worth finding all the way up the ladder) times the tier's multiplier; timers use the world's game time, saved with
 * the player. All of it is decided on the server.
 */
public final class TreasureService {
	private TreasureService() {
	}

	static String key(BlockPos pos) {
		return Long.toString(pos.asLong());
	}

	/** Game time at which {@code player} can open the chest at {@code pos} again (0 = now). */
	public static long refillsAt(ServerPlayer player, BlockPos pos) {
		return player.getAttachedOrElse(ModAttachments.TREASURE_REFILLS, Map.of()).getOrDefault(key(pos), 0L);
	}

	/** The Beli a chest of {@code tier} on {@code island} pays before the random spread. */
	public static long baseReward(Island island, TreasureTier tier) {
		FirstSeaProgression.Stage stage = island == null ? null : FirstSeaProgression.stage(island.name(), FirstSeaProgression.LOWER);
		if (stage == null) {
			stage = FirstSeaProgression.STAGES.get(0); // the hub and anything off the ladder pay like the first island
		}
		double mult = GrandLineMod.config().treasureRewardMultipliers[tier.ordinal()];
		return Math.max(1, Math.round(FirstSeaProgression.questCurrency(stage) * mult));
	}

	/** Opens the chest for this player if it is ready; returns the Beli paid (0 when it was still refilling). */
	public static long open(ServerPlayer player, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof TreasureChestBlock chest)) {
			return 0;
		}
		TreasureTier tier = chest.tier();
		ServerLevel level = player.serverLevel();
		long now = level.getGameTime();
		long ready = refillsAt(player, pos);
		Component name = Component.translatable("block.grandline.treasure_chest_" + tier.id()).withStyle(tier.color);
		if (now < ready) {
			long seconds = (ready - now + 19) / 20;
			String left = seconds >= 3600 ? (seconds / 3600) + "h " + (seconds % 3600) / 60 + "m"
					: seconds >= 60 ? (seconds / 60) + "m " + (seconds % 60) + "s" : seconds + "s";
			player.displayClientMessage(Component.translatable("message.grandline.treasure.refilling", name, left).withStyle(ChatFormatting.GRAY), true);
			level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.6f, 1.2f);
			return 0;
		}
		Island island = com.grandline.npc.EnemyRespawns.islandAt(level, pos);
		long base = baseReward(island, tier);
		double spread = GrandLineMod.config().treasureRewardSpread;
		long reward = Math.max(1, Math.round(base * (1 - spread + level.getRandom().nextDouble() * 2 * spread)));
		PlayerProfileService.addCurrency(player, reward);
		Map<String, Long> refills = new HashMap<>(player.getAttachedOrElse(ModAttachments.TREASURE_REFILLS, Map.of()));
		refills.values().removeIf(t -> t <= now); // forget timers that have run out
		refills.put(key(pos), now + 20L * 60 * GrandLineMod.config().treasureRefillMinutes[tier.ordinal()]);
		player.setAttached(ModAttachments.TREASURE_REFILLS, refills);
		player.displayClientMessage(Component.translatable("message.grandline.treasure.opened", name,
				String.format(java.util.Locale.ROOT, "%,d", reward)).withStyle(ChatFormatting.YELLOW), true);
		// Only the opener sees and hears it open (the chest is theirs alone).
		double x = pos.getX() + 0.5, y = pos.getY() + 0.9, z = pos.getZ() + 0.5;
		player.playNotifySound(SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8f, 1.0f);
		player.playNotifySound(tier == TreasureTier.LEGENDARY ? SoundEvents.PLAYER_LEVELUP : SoundEvents.EXPERIENCE_ORB_PICKUP,
				SoundSource.PLAYERS, tier == TreasureTier.LEGENDARY ? 0.7f : 0.6f, tier == TreasureTier.COMMON ? 1.3f : 1.0f);
		int coins = switch (tier) {
			case COMMON -> 10;
			case RARE -> 20;
			case LEGENDARY -> 40;
		};
		level.sendParticles(player, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_NUGGET)), true, x, y, z, coins, 0.25, 0.2, 0.25, 0.12);
		if (tier != TreasureTier.COMMON) {
			level.sendParticles(player, tier == TreasureTier.LEGENDARY ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.GLOW, true, x, y, z,
					tier == TreasureTier.LEGENDARY ? 30 : 12, 0.3, 0.3, 0.3, 0.25);
		}
		GrandLineMod.LOGGER.debug("{} opened a {} chest at {} for {} Beli", player.getGameProfile().getName(), tier.id(), pos, reward);
		return reward;
	}
}
