package com.kamerrezz.zoneroyale.game;

import com.kamerrezz.zoneroyale.ZoneRoyaleConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;

public class GameManager {
    private static GameManager instance;

    private boolean gameActive = false;
    private int shrinkTimer = 0;

    private final ZoneController zone = new ZoneController();
    private final PlayerManager players = new PlayerManager();
    private final GameMessenger messenger = new GameMessenger();

    private GameManager() {}

    public static GameManager getInstance() {
        if (instance == null) instance = new GameManager();
        return instance;
    }

    public void startGame() {
        if (gameActive) return;

        MinecraftServer server = getServer();
        if (server == null) return;

        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) return;

        List<ServerPlayer> allPlayers = server.getPlayerList().getPlayers();
        if (allPlayers.isEmpty()) {
            messenger.broadcast("No hay jugadores para iniciar el Battle Royale!");
            return;
        }

        gameActive = true;
        shrinkTimer = 0;

        players.startRound(allPlayers);
        zone.start(level, allPlayers.size());
        players.spawnAll(level, zone.getCenterX(), zone.getCenterZ(), zone.getCurrentRadius());
        messenger.hideNametags(level, players.getAlive());
        messenger.titleAll(server, "§6¡BATTLE ROYALE!", "§a¡Que tengan una buena partida!");
        messenger.broadcast("¡Battle Royale iniciado! Jugadores: " + allPlayers.size());
        messenger.broadcast("Radio inicial: " + (int) zone.getCurrentRadius() + " bloques");
    }

    public void stopGame() {
        gameActive = false;
        players.clear();

        MinecraftServer server = getServer();
        if (server != null) {
            ServerLevel level = server.getLevel(Level.OVERWORLD);
            if (level != null) {
                zone.reset(level);
                messenger.showNametags(level);
            }
        }

        messenger.broadcast("Battle Royale terminado!");
    }

    public void tick() {
        if (!gameActive) return;

        shrinkTimer++;
        int intervalTicks = ZoneRoyaleConfig.shrinkIntervalSeconds * 20;
        if (shrinkTimer >= intervalTicks) {
            MinecraftServer server = getServer();
            if (server != null) {
                ServerLevel level = server.getLevel(Level.OVERWORLD);
                if (level != null && zone.shrink(level)) {
                    int lerpSeconds = ZoneRoyaleConfig.shrinkLerpMillis / 1000;
                    messenger.broadcast("¡La zona se está reduciendo! Nuevo radio: " + (int) zone.getCurrentRadius() + " bloques");
                    messenger.broadcast("Tienes " + lerpSeconds + " segundos para moverte a la zona segura!");
                }
            }
            shrinkTimer = 0;
        }
        checkWinCondition();
    }

    public void onPlayerDeath(ServerPlayer player) {
        if (!gameActive || !players.isAlive(player)) return;

        players.kill(player);
        messenger.broadcast(player.getName().getString() + " ha sido eliminado! Jugadores restantes: " + players.aliveCount());
        checkWinCondition();
    }

    public void onPlayerJoin(ServerPlayer player) {
        if (!gameActive) return;
        players.putSpectator(player);
        messenger.tell(player, "§eHay un Battle Royale en curso. Estás en modo espectador.");
        messenger.tell(player, "§eJugadores restantes: " + players.aliveCount());
    }

    private void checkWinCondition() {
        if (players.aliveCount() == 1) {
            ServerPlayer winner = players.getAlive().get(0);
            messenger.broadcast("¡¡¡" + winner.getName().getString() + " ha ganado el Battle Royale!!!");
            messenger.title(winner, "§6¡VICTORIA!", "§eEres el último superviviente");
            messenger.playVictorySound(winner);

            MinecraftServer server = getServer();
            if (server != null) {
                ServerLevel level = server.getLevel(Level.OVERWORLD);
                if (level != null) {
                    players.teleportAllToWinner(level, winner, server.getPlayerList().getPlayers());
                }
            }
            stopGame();
        } else if (players.aliveCount() == 0) {
            messenger.broadcast("¡Empate! Todos los jugadores fueron eliminados.");
            stopGame();
        }
    }

    public boolean isActive() {
        return gameActive;
    }

    public boolean isAlivePlayer(ServerPlayer player) {
        return players.isAlive(player);
    }

    public int aliveCount() {
        return players.aliveCount();
    }

    public double getCurrentRadius() {
        return zone.getCurrentRadius();
    }

    public void setCenter(int x, int z) {
        zone.setCenter(x, z);
    }

    private MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}
