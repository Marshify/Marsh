package com.grandline.test;

import com.grandline.GrandLineMod;
import com.grandline.client.ClientProfileCache;
import com.grandline.data.ModAttachments;
import com.grandline.network.ServerRequestGuard;
import com.grandline.network.payload.AbilityActivationRequestPayload;
import com.grandline.network.payload.RequestProfileSyncPayload;
import com.grandline.player.PlayerProfile;
import com.grandline.player.PlayerProfileService;
import com.grandline.registry.ModItems;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * End-to-end test that boots the real Minecraft client (run with {@code ./gradlew runClientGameTest}, under Xvfb on a
 * headless machine) and exercises the mod against both an integrated and a dedicated server.
 */
public final class GrandLineClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		testIntegratedServer(context);
		testDedicatedServer(context);
	}

	private static void testIntegratedServer(ClientGameTestContext context) {
		TestWorldSave save;
		PlayerProfile expected;

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			save = singleplayer.getWorldSave();
			TestServerContext server = singleplayer.getServer();

			// Item registration, as seen from the client's registry.
			check(context.computeOnClient(client -> BuiltInRegistries.ITEM.getValue(GrandLineMod.id("grand_line_crystal")) == ModItems.GRAND_LINE_CRYSTAL),
					"client sees the registered Grand Line Crystal");

			// Commands executed as the player.
			check(server.computeOnServer(s -> runAsPlayer(s, "grandline test", false)) == 1, "/grandline test reports the item as registered");
			check(server.computeOnServer(s -> runAsPlayer(s, "grandline profile", false)) == 1, "/grandline profile returns level 1");
			check(server.computeOnServer(s -> runAsPlayer(s, "grandline admin addcurrency @s 5", false)) == -1,
					"non-operators cannot run admin commands");

			// Admin commands modify the authoritative profile and the client receives the sync.
			server.runCommand("grandline admin addxp @a 260");
			server.runCommand("grandline admin addcurrency @a 777");
			server.runCommand("give @a grandline:grand_line_crystal 3");
			singleplayer.getConnection().waitForClientboundPackets();

			expected = server.computeOnServer(s -> PlayerProfileService.get(firstPlayer(s)));
			check(expected.level() == 3 && expected.currency() == 777 && expected.unallocatedStatPoints() == 6,
					"server profile after admin commands: " + expected);
			assertClientMatches(context, singleplayer.getConnection(), expected);

			// A client cannot modify authoritative data: send raw (even invalid) requests and verify nothing changed.
			long rejectedBefore = server.computeOnServer(s -> ServerRequestGuard.rejectedCount());
			context.runOnClient(client -> {
				ClientPlayNetworking.send(new AbilityActivationRequestPayload(9999));
				ClientPlayNetworking.send(RequestProfileSyncPayload.INSTANCE);
			});
			singleplayer.getConnection().waitForServerboundPackets();
			context.waitTicks(5);
			check(server.computeOnServer(s -> ServerRequestGuard.rejectedCount()) > rejectedBefore, "invalid ability request was rejected");
			check(server.computeOnServer(s -> PlayerProfileService.get(firstPlayer(s))).equals(expected), "client requests did not change the profile");

			context.takeScreenshot("grandline_integrated_world");
		}

		// Closing the world shut the integrated server down and saved it. Reopen from disk.
		try (TestSingleplayerContext reopened = save.open()) {
			reopened.getConnection().waitForChunksRender();
			PlayerProfile reloaded = reopened.getServer().computeOnServer(s -> PlayerProfileService.get(firstPlayer(s)));
			check(reloaded.equals(expected), "profile persisted across integrated server restart: " + reloaded);
			assertClientMatches(context, reopened.getConnection(), expected);
		}
	}

	private static void testDedicatedServer(ClientGameTestContext context) {
		try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
			check(server.computeOnServer(MinecraftServer::isDedicatedServer), "server is a dedicated server");

			PlayerProfile expected;
			try (TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				server.runCommand("grandline admin addcurrency @a 4242");
				server.runCommand("grandline admin setlevel @a 10");
				connection.waitForClientboundPackets();

				expected = server.computeOnServer(s -> PlayerProfileService.get(firstPlayer(s)));
				check(expected.level() == 10 && expected.currency() == 4242, "dedicated server profile: " + expected);
				assertClientMatches(context, connection, expected);
				context.takeScreenshot("grandline_dedicated_server");
			}

			// Logging out saves player data to disk. Read the raw file to prove the profile is on disk.
			server.waitFor(s -> s.getPlayerList().getPlayers().isEmpty());
			PlayerProfile onDisk = server.computeOnServer(GrandLineClientGameTest::readSingleProfileFromDisk);
			check(expected.equals(onDisk), "profile written to the dedicated server's player data file: " + onDisk);

			// Log back in: the profile is loaded from disk and re-synced to the client.
			try (TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				PlayerProfile reloaded = server.computeOnServer(s -> PlayerProfileService.get(firstPlayer(s)));
				check(expected.equals(reloaded), "profile reloaded after reconnect: " + reloaded);
				assertClientMatches(context, connection, expected);
			}
		}
	}

	private static void assertClientMatches(ClientGameTestContext context, TestServerConnection connection, PlayerProfile expected) {
		connection.waitForClientboundPackets();
		context.waitFor(client -> expected.equals(ClientProfileCache.profile()), 100);
		PlayerProfile onClient = context.computeOnClient(client -> ClientProfileCache.profile());
		check(expected.equals(onClient), "client cache matches server profile; client has " + onClient);
	}

	private static ServerPlayer firstPlayer(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	/** Executes a command as the (only) player, optionally stripped of permissions. Returns -1 on failure. */
	private static int runAsPlayer(MinecraftServer server, String command, boolean asOperator) {
		var source = firstPlayer(server).createCommandSourceStack()
				.withPermission(asOperator ? PermissionSet.ALL_PERMISSIONS : PermissionSet.NO_PERMISSIONS);
		try {
			return server.getCommands().getDispatcher().execute(command, source);
		} catch (CommandSyntaxException e) {
			return -1;
		}
	}

	private static PlayerProfile readSingleProfileFromDisk(MinecraftServer server) {
		try {
			return readSingleProfileFromDisk(server.getWorldPath(LevelResource.PLAYER_DATA_DIR));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static PlayerProfile readSingleProfileFromDisk(Path dir) throws IOException {
		Path file;
		try (var files = Files.list(dir)) {
			file = files.filter(p -> p.toString().endsWith(".dat")).findFirst()
					.orElseThrow(() -> new AssertionError("no player data file in " + dir));
		}

		CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
		CompoundTag attachments = root.getCompoundOrEmpty(AttachmentTarget.NBT_ATTACHMENT_KEY);
		var profileTag = attachments.get(ModAttachments.PLAYER_PROFILE.identifier().toString());
		check(profileTag != null, "player data file " + file + " contains the profile attachment; keys: " + attachments.keySet());
		return PlayerProfile.CODEC.parse(NbtOps.INSTANCE, profileTag).getOrThrow();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError("Grand Line client test failed: " + message);
		}
		GrandLineMod.LOGGER.info("[client gametest] OK: {}", message);
	}
}
