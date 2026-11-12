package com.grandline.fruit;

import com.grandline.GrandLineMod;
import com.grandline.player.PlayerProfile;
import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Server-authoritative fruit economy: eating, the dealer shop (rotating stock), the gacha (weighted by rarity, with a
 * per-player cooldown saved in the profile), the personal stash and natural fruit spawns. Every method re-validates
 * from server state; client packets only say what the player wants to do.
 */
public final class FruitService {
	public enum Result {
		SUCCESS,
		NOT_ENOUGH_CURRENCY,
		LEVEL_TOO_LOW,
		ON_COOLDOWN,
		NOT_IN_STOCK,
		UNKNOWN_FRUIT,
		STASH_FULL,
		NOT_IN_STASH,
		NOT_HOLDING_FRUIT,
		ALREADY_HAVE_FRUIT
	}

	/** Gacha outcome: the result code and, on success, the fruit won. */
	public record GachaRoll(Result result, Fruits.FruitDef fruit) {
	}

	/** Natural fruits despawn after 20 minutes. */
	public static final int NATURAL_FRUIT_LIFETIME_TICKS = 20 * 60 * 20;

	private record SpawnedFruit(ItemEntity entity, int despawnTick) {
	}

	private static final List<SpawnedFruit> NATURAL_FRUITS = new ArrayList<>();

	private FruitService() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(FruitService::tickNaturalSpawns);
		ServerTickEvents.END_SERVER_TICK.register(server -> NATURAL_FRUITS.removeIf(spawned -> {
			if (spawned.entity().isRemoved()) {
				return true; // picked up or otherwise gone
			}
			if (server.getTickCount() >= spawned.despawnTick()) {
				spawned.entity().discard();
				return true;
			}
			return false;
		}));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> NATURAL_FRUITS.clear());
	}

	// ------------------------------------------------------------------------------------------------ eating

	/** Eats {@code item}: the eaten fruit replaces any current fruit. Returns false if it is already the player's fruit. */
	public static boolean eat(ServerPlayer player, FruitItem item) {
		String id = item.powerId().toString();
		PlayerProfile profile = PlayerProfileService.get(player);
		if (id.equals(profile.powers().fruit())) {
			player.displayClientMessage(Component.translatable("message.grandline.fruit.already").withStyle(ChatFormatting.RED), true);
			return false;
		}

		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withFruit(id)));
		Fruits.FruitDef def = Fruits.get(id);
		Component name = Component.translatable(def.power().translationKey()).withStyle(def.rarity().formatting());
		player.sendSystemMessage(Component.translatable("message.grandline.fruit.eaten", name).withStyle(ChatFormatting.GOLD));
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 0.8F);
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
		player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 0.8, 0.5, 0.5);
		return true;
	}

	/** Handles the Eat / Store / Drop choice from the fruit dialog for the fruit in {@code mainHand ? main : off} hand. */
	public static Result useHeldFruit(ServerPlayer player, boolean mainHand, FruitAction action) {
		ItemStack stack = mainHand ? player.getMainHandItem() : player.getOffhandItem();
		if (!(stack.getItem() instanceof FruitItem item)) {
			return Result.NOT_HOLDING_FRUIT;
		}

		switch (action) {
			case EAT -> {
				if (!eat(player, item)) {
					return Result.ALREADY_HAVE_FRUIT;
				}
				if (!player.getAbilities().instabuild) {
					stack.shrink(1);
				}
			}
			case STORE -> {
				Result result = storeInStash(player, item.powerId().toString());
				if (result != Result.SUCCESS) {
					return result;
				}
				stack.shrink(1);
			}
			case DROP -> {
				player.drop(stack.copyWithCount(1), false, true);
				stack.shrink(1);
			}
		}
		return Result.SUCCESS;
	}

	public enum FruitAction {
		EAT, STORE, DROP;

		public static FruitAction byId(int id) {
			FruitAction[] values = values();
			return id >= 0 && id < values.length ? values[id] : null;
		}
	}

	// ------------------------------------------------------------------------------------------------ shop

	public static long price(Fruits.FruitDef fruit) {
		return GrandLineMod.config().fruitPrices[fruit.rarity().ordinal()];
	}

	/** The dealer's current stock. Deterministic per rotation window and world seed, so it needs no saving. */
	public static List<Fruits.FruitDef> dealerStock(MinecraftServer server) {
		long window = System.currentTimeMillis() / (GrandLineMod.config().dealerRotationMinutes * 60_000L);
		Random random = new Random(server.overworld().getSeed() * 31 + window);
		List<Fruits.FruitDef> pool = new ArrayList<>(Fruits.all());
		List<Fruits.FruitDef> stock = new ArrayList<>();
		while (stock.size() < GrandLineMod.config().dealerStockSize && !pool.isEmpty()) {
			Fruits.FruitDef pick = weightedPick(pool, random::nextInt);
			stock.add(pick);
			pool.remove(pick);
		}
		stock.sort((a, b) -> Integer.compare(a.rarity().ordinal(), b.rarity().ordinal()));
		return stock;
	}

	/** Milliseconds until the dealer restocks. */
	public static long millisUntilRestock() {
		long rotation = GrandLineMod.config().dealerRotationMinutes * 60_000L;
		return rotation - System.currentTimeMillis() % rotation;
	}

	public static Result buy(ServerPlayer player, String fruitId) {
		Fruits.FruitDef fruit = Fruits.get(fruitId);
		if (fruit == null) {
			return Result.UNKNOWN_FRUIT;
		}
		if (!dealerStock(player.server).contains(fruit)) {
			return Result.NOT_IN_STOCK;
		}
		long price = price(fruit);
		if (PlayerProfileService.get(player).currency() < price) {
			return Result.NOT_ENOUGH_CURRENCY;
		}

		PlayerProfileService.addCurrency(player, -price);
		give(player, fruit);
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 1.0F, 1.0F);
		return Result.SUCCESS;
	}

	// ------------------------------------------------------------------------------------------------ gacha

	public static long gachaMillisRemaining(ServerPlayer player) {
		return Math.max(0L, PlayerProfileService.get(player).powers().gachaReadyAtMillis() - System.currentTimeMillis());
	}

	/** Gacha price for a player of {@code level}: base + perLevel × (level − 1). */
	public static long gachaCost(int level) {
		long cost = GrandLineMod.config().gachaCost + GrandLineMod.config().gachaCostPerLevel * Math.max(0, level - 1L);
		return Math.min(cost, GrandLineMod.config().maxCurrency);
	}

	public static long gachaCost(ServerPlayer player) {
		return gachaCost(PlayerProfileService.get(player).level());
	}

	/** Pays for and performs a gacha roll. The fruit is given immediately; the client only animates the reveal. */
	public static GachaRoll rollGacha(ServerPlayer player) {
		if (PlayerProfileService.get(player).level() < GrandLineMod.config().gachaMinLevel) {
			return new GachaRoll(Result.LEVEL_TOO_LOW, null);
		}
		if (gachaMillisRemaining(player) > 0) {
			return new GachaRoll(Result.ON_COOLDOWN, null);
		}
		long cost = gachaCost(player);
		if (PlayerProfileService.get(player).currency() < cost) {
			return new GachaRoll(Result.NOT_ENOUGH_CURRENCY, null);
		}

		Fruits.FruitDef fruit = weightedPick(Fruits.all(), bound -> player.getRandom().nextInt(bound));
		long readyAt = System.currentTimeMillis() + GrandLineMod.config().gachaCooldownMinutes * 60_000L;
		PlayerProfileService.addCurrency(player, -cost);
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withGachaReadyAt(readyAt)));
		give(player, fruit);

		if (fruit.rarity().ordinal() >= FruitRarity.LEGENDARY.ordinal()) {
			Component name = Component.translatable(fruit.power().translationKey()).withStyle(fruit.rarity().formatting());
			player.server.getPlayerList().broadcastSystemMessage(Component.translatable("message.grandline.gacha.broadcast",
					player.getDisplayName(), name).withStyle(ChatFormatting.GOLD), false);
		}
		return new GachaRoll(Result.SUCCESS, fruit);
	}

	// ------------------------------------------------------------------------------------------------ stash

	public static Result storeInStash(ServerPlayer player, String fruitId) {
		if (Fruits.get(fruitId) == null) {
			return Result.UNKNOWN_FRUIT;
		}
		PlayerProfile profile = PlayerProfileService.get(player);
		if (profile.powers().stashTotal() >= GrandLineMod.config().stashCapacity) {
			return Result.STASH_FULL;
		}
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withStashCount(fruitId, p.powers().stashCount(fruitId) + 1)));
		return Result.SUCCESS;
	}

	public static Result withdrawFromStash(ServerPlayer player, String fruitId) {
		Fruits.FruitDef fruit = Fruits.get(fruitId);
		if (fruit == null) {
			return Result.UNKNOWN_FRUIT;
		}
		if (PlayerProfileService.get(player).powers().stashCount(fruitId) <= 0) {
			return Result.NOT_IN_STASH;
		}
		PlayerProfileService.update(player, p -> p.withPowers(p.powers().withStashCount(fruitId, p.powers().stashCount(fruitId) - 1)));
		give(player, fruit);
		return Result.SUCCESS;
	}

	// ------------------------------------------------------------------------------------------------ helpers

	/** Puts a fruit item in the player's inventory, dropping it at their feet if the inventory is full. */
	public static void give(ServerPlayer player, Fruits.FruitDef fruit) {
		ItemStack stack = new ItemStack(fruit.item());
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	interface IntBound {
		int next(int bound);
	}

	static Fruits.FruitDef weightedPick(List<Fruits.FruitDef> pool, IntBound random) {
		int total = 0;
		for (Fruits.FruitDef fruit : pool) {
			total += fruit.rarity().gachaWeight();
		}
		int roll = random.next(total);
		for (Fruits.FruitDef fruit : pool) {
			roll -= fruit.rarity().gachaWeight();
			if (roll < 0) {
				return fruit;
			}
		}
		return pool.get(pool.size() - 1);
	}

	/** Every so often a fruit appears on the ground somewhere near a random online overworld player. */
	private static void tickNaturalSpawns(MinecraftServer server) {
		int minutes = GrandLineMod.config().naturalFruitSpawnMinutes;
		if (minutes <= 0 || server.getTickCount() % 200 != 0) {
			return; // check every 10 seconds
		}
		// Chance per check so that the average interval equals the configured minutes.
		RandomSource random = server.overworld().getRandom();
		if (random.nextInt(minutes * 6) != 0) {
			return;
		}
		List<ServerPlayer> players = server.overworld().players();
		if (players.isEmpty()) {
			return;
		}
		ServerPlayer near = players.get(random.nextInt(players.size()));
		spawnNaturalFruit(near.serverLevel(), near.blockPosition(), random);
	}

	/** Spawns a random fruit on the surface 20–40 blocks from {@code around}. Returns the item entity, or null. */
	public static ItemEntity spawnNaturalFruit(ServerLevel level, BlockPos around, RandomSource random) {
		double angle = random.nextDouble() * Math.PI * 2;
		int distance = 20 + random.nextInt(21);
		int x = around.getX() + (int) (Math.cos(angle) * distance);
		int z = around.getZ() + (int) (Math.sin(angle) * distance);
		BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
		if (!level.getFluidState(top.below()).isEmpty()) {
			return null; // don't drop fruits into the sea
		}

		Fruits.FruitDef fruit = weightedPick(Fruits.all(), random::nextInt);
		ItemEntity entity = new ItemEntity(level, top.getX() + 0.5, top.getY() + 0.5, top.getZ() + 0.5, new ItemStack(fruit.item()));
		// Vanilla items despawn after 5 minutes; natural fruits last 20. Make the item permanent and remove it ourselves.
		entity.setUnlimitedLifetime();
		NATURAL_FRUITS.add(new SpawnedFruit(entity, level.getServer().getTickCount() + NATURAL_FRUIT_LIFETIME_TICKS));
		level.addFreshEntity(entity);
		level.getServer().getPlayerList().broadcastSystemMessage(
				Component.translatable("message.grandline.fruit.spawned").withStyle(ChatFormatting.GREEN), false);
		return entity;
	}
}
