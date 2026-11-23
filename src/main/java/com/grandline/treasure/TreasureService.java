package com.grandline.treasure;

import com.grandline.GrandLineMod;
import com.grandline.data.ModAttachments;
import com.grandline.player.PlayerProfileService;
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
 * timer for that chest; everyone else can still open it. The Beli is random within a fixed range set only by the chest's
 * sea and rarity (config treasureRewards): island level, player level and enemy levels play no part, so a common chest
 * pays the same anywhere in its sea. Rewards are rolled when a chest is opened, so chests already placed in old worlds
 * follow the same table. Timers use the world's game time, saved with the player. All of it is decided on the server.
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

	/**
	 * The Beli range {min, max} of a chest: set only by its sea and rarity (config treasureRewards), so every chest of the
	 * same rarity in the same sea can pay exactly the same amounts. Seas past the table use its last sea.
	 */
	public static long[] rewardRange(int sea, TreasureTier tier) {
		long[][][] table = GrandLineMod.config().treasureRewards;
		long[] range = table[Math.max(0, Math.min(table.length - 1, sea - 1))][tier.ordinal()];
		return new long[]{range[0], range[1]};
	}

	/** A random payout for a chest of {@code tier} in {@code sea}: uniform over its range, both ends included. */
	public static long rollReward(int sea, TreasureTier tier, net.minecraft.util.RandomSource random) {
		long[] r = rewardRange(sea, tier);
		return r[0] + (long) Math.floor(random.nextDouble() * (r[1] - r[0] + 1));
	}

	/** The sea a chest is in: its island's sea, else the sea of the column it stands in. */
	public static int seaOf(Island island, BlockPos pos) {
		return island != null ? island.sea() : com.grandline.world.SeaLayout.seaAt(pos.getX());
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
		long reward = rollReward(seaOf(island, pos), tier, level.getRandom());
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
