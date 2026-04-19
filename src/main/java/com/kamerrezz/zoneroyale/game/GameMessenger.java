package com.kamerrezz.zoneroyale.game;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

class GameMessenger {
    private static final String TEAM_NAME = "battleRoyaleTeam";
    private static final String PREFIX = "§6[Zone Royale] §f";

    void broadcast(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        Component chatMessage = Component.literal(PREFIX + message);
        server.getPlayerList().broadcastSystemMessage(chatMessage, false);
    }

    void tell(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(PREFIX + message));
    }

    void title(ServerPlayer player, String title, String subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
    }

    void titleAll(MinecraftServer server, String title, String subtitle) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            title(player, title, subtitle);
        }
    }

    void playVictorySound(ServerPlayer player) {
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
    }

    void hideNametags(ServerLevel level, List<ServerPlayer> players) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_NAME);
        }
        team.setNameTagVisibility(Team.Visibility.NEVER);
        for (ServerPlayer player : players) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        }
    }

    void showNametags(ServerLevel level) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) return;
        for (String playerName : new ArrayList<>(team.getPlayers())) {
            scoreboard.removePlayerFromTeam(playerName, team);
        }
        scoreboard.removePlayerTeam(team);
    }
}
