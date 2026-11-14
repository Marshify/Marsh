package com.grandline.player;

import com.grandline.data.ModAttachments;
import com.grandline.network.ServerRequestGuard;
import com.grandline.network.payload.ChooseTeamPayload;
import com.grandline.network.payload.TeamPromptPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Pirates or Marines. Chosen on the Pick a Side screen every time a player joins (the server asks), saved with the
 * player, and applied as a vanilla scoreboard team: names are coloured, and Marines cannot hurt fellow Marines while
 * Pirates can fight anyone, including other Pirates.
 */
public final class TeamService {
	public enum Side {
		PIRATES("grandline_pirates", ChatFormatting.RED, true),
		MARINES("grandline_marines", ChatFormatting.BLUE, false);

		final String teamName;
		final ChatFormatting color;
		final boolean friendlyFire;

		Side(String teamName, ChatFormatting color, boolean friendlyFire) {
			this.teamName = teamName;
			this.color = color;
			this.friendlyFire = friendlyFire;
		}

		public String key() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}

		public static Side byId(int id) {
			return id >= 0 && id < values().length ? values()[id] : null;
		}
	}

	private TeamService() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			Side side = get(player);
			if (side != null) {
				applyScoreboard(player, side);
			}
			if (ServerPlayNetworking.canSend(player, TeamPromptPayload.TYPE)) {
				ServerPlayNetworking.send(player, new TeamPromptPayload(side == null ? -1 : side.ordinal()));
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(ChooseTeamPayload.TYPE, (packet, player, sender) -> {
			if (ServerRequestGuard.check(player, ChooseTeamPayload.TYPE) != ServerRequestGuard.Result.ACCEPTED) {
				return;
			}
			Side side = Side.byId(packet.team());
			if (side == null) {
				ServerRequestGuard.reject(player, ChooseTeamPayload.TYPE, ServerRequestGuard.Result.REJECTED_INVALID);
				return;
			}
			choose(player, side);
		});
	}

	public static Side get(ServerPlayer player) {
		String saved = player.getAttachedOrElse(ModAttachments.TEAM, "");
		for (Side side : Side.values()) {
			if (side.key().equals(saved)) {
				return side;
			}
		}
		return null;
	}

	public static void choose(ServerPlayer player, Side side) {
		Side before = get(player);
		if (before != side && com.grandline.reputation.ReputationService.isTagged(player)) {
			player.displayClientMessage(Component.translatable("message.grandline.team.tagged").withStyle(ChatFormatting.RED), true);
			return;
		}
		player.setAttached(ModAttachments.TEAM, side.key());
		applyScoreboard(player, side);
		com.grandline.world.IslandSpawns.onSideChosen(player);
		com.grandline.reputation.ReputationService.sync(player); // the displayed/earned total follows the side
		if (before != side) {
			player.server.getPlayerList().broadcastSystemMessage(Component.translatable("message.grandline.team.joined",
					player.getDisplayName(), Component.translatable("team.grandline." + side.key()).withStyle(side.color)), false);
		}
	}

	private static void applyScoreboard(ServerPlayer player, Side side) {
		Scoreboard scoreboard = player.server.getScoreboard();
		PlayerTeam team = scoreboard.getPlayerTeam(side.teamName);
		if (team == null) {
			team = scoreboard.addPlayerTeam(side.teamName);
		}
		team.setColor(side.color);
		team.setAllowFriendlyFire(side.friendlyFire);
		team.setDisplayName(Component.translatable("team.grandline." + side.key()));
		scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
	}
}
