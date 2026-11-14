package com.grandline.fishing;

import com.grandline.combat.fx.FxEvents;
import com.grandline.data.ModAttachments;
import com.grandline.entity.GrandLineEntities;
import com.grandline.network.payload.FishingEndPayload;
import com.grandline.network.payload.FishingInputPayload;
import com.grandline.network.payload.FishingMasteryPayload;
import com.grandline.network.payload.FishingStartPayload;
import com.grandline.network.payload.FishingStatePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of fishing, one session per player. Cast → the bobber settles on water → after a wait a fish bites (the
 * bobber dips, splashes) → right-click within the hook window → the catch minigame: a fish darts up and down a bar,
 * the player holds to lift their catch zone (it sinks when released); the reel meter fills while the fish is inside
 * the zone and drains while it is out. Full = caught, empty = got away.
 *
 * <p>The minigame runs here, from the player's hold/release input, and the client only draws what the server sends,
 * so a catch cannot be faked. A session ends exactly once (caught, lost, given up, disconnected, rod put away or the
 * bobber gone) and only a caught session pays out.
 */
public final class FishingSessions {
	enum Phase { WAITING, BITE, FIGHT }

	static final class Session {
		final UUID player;
		final BobberEntity bobber;
		final RandomSource random = RandomSource.create();
		Phase phase = Phase.WAITING;
		int timer;
		int groundedTicks;
		FishingRules.Fish fish;
		int mastery;
		double zone = 0.3, zoneVel, zoneSize;
		double fishPos = 0.5, fishVel, fishTarget = 0.5;
		int targetTimer;
		double progress = 0.3;
		boolean holding;
		int fightTicks;
		boolean inZone;

		Session(UUID player, BobberEntity bobber) {
			this.player = player;
			this.bobber = bobber;
		}
	}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	static final int FIGHT_LIMIT = 20 * 60;

	private FishingSessions() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(FishingSessions::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> end(handler.getPlayer().getUUID(), false, null));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncMastery(handler.getPlayer()));
		ServerPlayNetworking.registerGlobalReceiver(FishingInputPayload.TYPE, (packet, player, sender) -> {
			Session s = SESSIONS.get(player.getUUID());
			if (s == null || s.phase != Phase.FIGHT) {
				return;
			}
			if (packet.giveUp()) {
				end(player.getUUID(), false, Component.translatable("message.grandline.fishing.gave_up"));
			} else {
				s.holding = packet.holding();
			}
		});
	}

	public static FishingMastery mastery(ServerPlayer player) {
		return player.getAttachedOrElse(ModAttachments.FISHING_MASTERY, FishingMastery.START);
	}

	public static void syncMastery(ServerPlayer player) {
		FishingMastery m = mastery(player);
		if (ServerPlayNetworking.canSend(player, FishingMasteryPayload.TYPE)) {
			ServerPlayNetworking.send(player, new FishingMasteryPayload(m.level(), m.xp(), FishingRules.xpToNext(m.level()), m.catches()));
		}
	}

	public static boolean fishing(ServerPlayer player) {
		return SESSIONS.containsKey(player.getUUID());
	}

	/** Right-click with the rod: cast, hook a bite, or reel the line back in. */
	public static void use(ServerPlayer player) {
		Session s = SESSIONS.get(player.getUUID());
		if (s == null) {
			cast(player);
		} else if (s.phase == Phase.BITE) {
			hook(player, s);
		} else if (s.phase == Phase.WAITING) {
			end(player.getUUID(), false, null); // reeled in empty
		}
	}

	static void cast(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BobberEntity bobber = GrandLineEntities.BOBBER.create(level);
		if (bobber == null) {
			return;
		}
		Vec3 look = player.getLookAngle();
		Vec3 start = player.getEyePosition().add(look.scale(0.6)).add(0, -0.2, 0);
		bobber.moveTo(start.x, start.y, start.z, 0, 0);
		bobber.setDeltaMovement(look.scale(1.05).add(0, 0.18, 0));
		bobber.setOwner(player);
		level.addFreshEntity(bobber);
		Session s = new Session(player.getUUID(), bobber);
		s.mastery = mastery(player).level();
		s.timer = FishingRules.biteDelay(s.mastery, s.random);
		SESSIONS.put(player.getUUID(), s);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.6F, 0.6F);
		FxEvents.send(player, "fishing_cast", start, look, 0, 0, 0);
	}

	static void hook(ServerPlayer player, Session s) {
		ServerLevel level = player.serverLevel();
		s.fish = FishingRules.roll(level, s.bobber.position(), s.mastery, s.random);
		s.phase = Phase.FIGHT;
		s.zoneSize = Math.max(0.12, FishingRules.zoneSize(s.mastery, s.fish.difficulty()));
		s.zone = 0.3;
		s.fishPos = 0.5;
		s.progress = 0.3;
		s.fightTicks = 0;
		s.bobber.setState(BobberEntity.HOOKED);
		level.playSound(null, s.bobber.getX(), s.bobber.getY(), s.bobber.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8F, 1.3F);
		if (ServerPlayNetworking.canSend(player, FishingStartPayload.TYPE)) {
			ServerPlayNetworking.send(player, new FishingStartPayload(BuiltInRegistries.ITEM.getKey(s.fish.item()).toString(),
					s.fish.tier().ordinal(), (float) s.fish.difficulty(), (float) s.zoneSize));
		}
		FxEvents.send(player, "fishing_reel", s.bobber.position(), Vec3.ZERO, 0, 0, 0);
	}

	/** The bobber was lost (owner gone, too far, timed out). */
	static void lost(BobberEntity bobber) {
		for (Session s : SESSIONS.values()) {
			if (s.bobber == bobber) {
				end(s.player, false, null);
				return;
			}
		}
		bobber.discard();
	}

	private static void tick(MinecraftServer server) {
		if (SESSIONS.isEmpty()) {
			return;
		}
		for (Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator(); it.hasNext(); ) {
			Session s = it.next().getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(s.player);
			if (player == null || !player.isAlive() || s.bobber.isRemoved() || !holdingRod(player)) {
				it.remove();
				finish(server, s, player, false, null);
				continue;
			}
			step(player, s);
		}
	}

	static boolean holdingRod(ServerPlayer player) {
		return player.getMainHandItem().getItem() instanceof StarterRodItem || player.getOffhandItem().getItem() instanceof StarterRodItem;
	}

	private static void step(ServerPlayer player, Session s) {
		ServerLevel level = player.serverLevel();
		BobberEntity b = s.bobber;
		switch (s.phase) {
			case WAITING -> {
				if (b.state() == BobberEntity.GROUNDED) {
					if (++s.groundedTicks > 30) {
						end(s.player, false, Component.translatable("message.grandline.fishing.no_water"));
					}
					return;
				}
				if (!b.onWater()) {
					return;
				}
				if (--s.timer == 20) {
					// A ripple of bubbles runs toward the bobber just before it bites.
					level.sendParticles(ParticleTypes.BUBBLE, b.getX(), b.getY() + 0.1, b.getZ(), 6, 0.4, 0.05, 0.4, 0.02);
				}
				if (s.timer <= 0) {
					s.phase = Phase.BITE;
					s.timer = FishingRules.hookWindow(s.mastery);
					b.setState(BobberEntity.BITE);
					level.sendParticles(ParticleTypes.SPLASH, b.getX(), b.getY() + 0.2, b.getZ(), 18, 0.25, 0.05, 0.25, 0.1);
					level.sendParticles(ParticleTypes.FISHING, b.getX(), b.getY() + 0.1, b.getZ(), 8, 0.15, 0, 0.15, 0.05);
					level.playSound(null, b.getX(), b.getY(), b.getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.5F, 1.0F);
					player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
					player.displayClientMessage(Component.translatable("message.grandline.fishing.bite").withStyle(ChatFormatting.AQUA), true);
				}
			}
			case BITE -> {
				if (--s.timer <= 0) {
					s.phase = Phase.WAITING;
					s.timer = FishingRules.biteDelay(s.mastery, s.random);
					b.setState(BobberEntity.FLOATING);
					player.displayClientMessage(Component.translatable("message.grandline.fishing.missed").withStyle(ChatFormatting.GRAY), true);
				}
			}
			case FIGHT -> fight(player, s);
		}
	}

	/** One tick of the catch minigame. */
	static void fight(ServerPlayer player, Session s) {
		double d = s.fish.difficulty();
		// The fish picks a new spot now and then, in its own style.
		if (--s.targetTimer <= 0) {
			double r = s.random.nextDouble();
			switch (s.fish.pattern()) {
				case FLOATER -> s.fishTarget = 0.45 + r * 0.5;
				case SINKER -> s.fishTarget = 0.05 + r * 0.5;
				case DART -> s.fishTarget = s.fishPos > 0.5 ? 0.05 + r * 0.35 : 0.6 + r * 0.35;
				default -> s.fishTarget = 0.08 + r * 0.84;
			}
			boolean darting = s.fish.pattern() == FishingRules.Pattern.DART
					|| (s.fish.pattern() == FishingRules.Pattern.MIXED && s.random.nextBoolean());
			s.targetTimer = (int) ((darting ? 10 : 22) + s.random.nextInt(darting ? 16 : 28) - d * 2);
		}
		double pull = 0.022 + 0.011 * d * (s.fish.pattern() == FishingRules.Pattern.DART ? 1.6 : 1);
		s.fishVel = s.fishVel * 0.78 + (s.fishTarget - s.fishPos) * pull;
		s.fishPos = Mth.clamp(s.fishPos + s.fishVel, 0.02, 0.98);
		// The catch zone rises while held and sinks when released.
		s.zoneVel = (s.zoneVel + (s.holding ? 0.0058 : -0.0048)) * 0.9;
		double half = s.zoneSize / 2;
		s.zone += s.zoneVel;
		if (s.zone < half || s.zone > 1 - half) {
			s.zone = Mth.clamp(s.zone, half, 1 - half);
			s.zoneVel *= -0.3;
		}
		s.inZone = Math.abs(s.fishPos - s.zone) <= half;
		double decayCut = 1 - Math.min(0.4, s.mastery * 0.004);
		s.progress += s.inZone ? 0.0115 / (1 + 0.08 * d) : -0.0078 * (1 + 0.12 * d) * decayCut;
		s.fightTicks++;
		if (ServerPlayNetworking.canSend(player, FishingStatePayload.TYPE)) {
			ServerPlayNetworking.send(player, new FishingStatePayload((float) s.fishPos, (float) s.zone, (float) Mth.clamp(s.progress, 0, 1), s.inZone));
		}
		if (s.fightTicks % 8 == 0) {
			player.serverLevel().sendParticles(ParticleTypes.SPLASH, s.bobber.getX(), s.bobber.getY() + 0.2, s.bobber.getZ(),
					s.inZone ? 2 : 6, 0.2, 0.05, 0.2, 0.05);
		}
		if (s.progress >= 1) {
			end(s.player, true, null);
		} else if (s.progress <= 0 || s.fightTicks > FIGHT_LIMIT) {
			end(s.player, false, Component.translatable("message.grandline.fishing.escaped", s.fish.item().getDescription()));
		}
	}

	/** Ends a player's session once: removes it, then rewards (if caught) and cleans up. */
	static void end(UUID player, boolean caught, Component message) {
		Session s = SESSIONS.remove(player);
		if (s == null) {
			return;
		}
		MinecraftServer server = s.bobber.getServer();
		finish(server, s, server == null ? null : server.getPlayerList().getPlayer(player), caught, message);
	}

	private static void finish(MinecraftServer server, Session s, ServerPlayer player, boolean caught, Component message) {
		Vec3 at = s.bobber.position();
		if (s.bobber.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.2, at.z, caught ? 24 : 8, 0.3, 0.1, 0.3, 0.15);
			if (caught) {
				level.sendParticles(ParticleTypes.FISHING, at.x, at.y + 0.3, at.z, 12, 0.2, 0.2, 0.2, 0.1);
			}
		}
		s.bobber.discard();
		if (player == null) {
			return;
		}
		boolean levelUp = false;
		long xp = 0;
		if (caught && s.fish != null) {
			ItemStack stack = new ItemStack(s.fish.item());
			if (!player.getInventory().add(stack)) {
				player.serverLevel().addFreshEntity(new ItemEntity(player.serverLevel(), player.getX(), player.getY() + 0.5, player.getZ(), stack));
			}
			FishingMastery before = mastery(player);
			xp = FishingRules.catchXp(s.fish);
			FishingMastery after = before.withCatch(xp);
			player.setAttached(ModAttachments.FISHING_MASTERY, after);
			levelUp = after.level() > before.level();
			player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE,
					SoundSource.PLAYERS, 1.0F, 1.0F);
			player.playNotifySound(levelUp ? SoundEvents.PLAYER_LEVELUP : SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.2F);
			player.displayClientMessage(Component.translatable("message.grandline.fishing.caught", s.fish.item().getDescription(), xp)
					.withStyle(ChatFormatting.GREEN), true);
			if (levelUp) {
				player.sendSystemMessage(Component.translatable("message.grandline.fishing.level_up", after.level()).withStyle(ChatFormatting.AQUA));
			}
			syncMastery(player);
		} else if (message != null) {
			player.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
		}
		if (s.phase == Phase.FIGHT && ServerPlayNetworking.canSend(player, FishingEndPayload.TYPE)) {
			ServerPlayNetworking.send(player, new FishingEndPayload(caught, s.fish == null ? ""
					: BuiltInRegistries.ITEM.getKey(s.fish.item()).toString(), xp, levelUp));
		}
		FxEvents.send(player, "fishing_end", player.position(), Vec3.ZERO, 0, 0, caught ? 1 : 0);
	}

	// ---- test access ----

	/** Starts a fight with a chosen fish (tests). */
	public static void startFightForTest(ServerPlayer player, FishingRules.Fish fish) {
		cast(player);
		Session s = SESSIONS.get(player.getUUID());
		s.phase = Phase.BITE;
		hook(player, s);
		s.fish = fish;
		s.zoneSize = Math.max(0.12, FishingRules.zoneSize(s.mastery, fish.difficulty()));
	}

	/** Runs the fight a tick, holding the reel while the zone is below the fish (a perfect player), or not at all. */
	public static boolean stepForTest(ServerPlayer player, boolean perfect) {
		Session s = SESSIONS.get(player.getUUID());
		if (s == null) {
			return false;
		}
		s.holding = perfect && (s.zone < s.fishPos || (s.zone - s.fishPos < s.zoneSize * 0.2 && s.zoneVel < 0));
		fight(player, s);
		return SESSIONS.containsKey(player.getUUID());
	}
}
