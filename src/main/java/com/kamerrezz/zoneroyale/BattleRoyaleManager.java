package com.kamerrezz.zoneroyale;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class BattleRoyaleManager {
    private static BattleRoyaleManager instance;

    // Estado del juego
    private boolean gameActive = false;
    private List<ServerPlayer> alivePlayers = new ArrayList<>();
    private List<ServerPlayer> deadPlayers = new ArrayList<>();

    // Configuración de la zona
    private int centerX = 0;
    private int centerZ = 0;
    private double currentRadius = 1000;
    private double targetRadius = 10;

    // Timer para reducir zona
    private int shrinkTimer = 0;
    private final int SHRINK_INTERVAL = 30 * 20; // 30 segundos en ticks (20 ticks = 1 segundo)

    private BattleRoyaleManager() {}

    public static BattleRoyaleManager getInstance() {
        if (instance == null) {
            instance = new BattleRoyaleManager();
        }
        return instance;
    }

    public void startGame() {
        if (gameActive) {
            return; // Ya hay un juego activo
        }

        gameActive = true;
        alivePlayers.clear();
        deadPlayers.clear();
        shrinkTimer = 0;

        MinecraftServer server = getServer();
        if (server == null) return;

        List<ServerPlayer> allPlayers = new ArrayList<>(server.getPlayerList().getPlayers());
        alivePlayers.addAll(allPlayers);

        if (alivePlayers.isEmpty()) {
            broadcastMessage("No hay jugadores para iniciar el Battle Royale!");
            stopGame();
            return;
        }

        calculateInitialRadius(allPlayers.size());
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level != null) {
            teleportPlayersRandomly(level);
            setupWorldBorder(level);
        }

        hidePlayerNametags();
        showTitleToAll("§6¡BATTLE ROYALE!", "§a¡Que tengan una buena partida!");
        broadcastMessage("¡Battle Royale iniciado! Jugadores: " + allPlayers.size());
        broadcastMessage("Radio inicial: " + (int)currentRadius + " bloques");
    }

    public void stopGame() {
        gameActive = false;
        alivePlayers.clear();
        deadPlayers.clear();

        MinecraftServer server = getServer();
        if (server != null) {
            ServerLevel level = server.getLevel(Level.OVERWORLD);
            if (level != null) {
                WorldBorder border = level.getWorldBorder();
                border.setSize(60000000); // Tamaño por defecto de Minecraft
            }
        }

        showPlayerNametags();
        broadcastMessage("Battle Royale terminado!");
    }

    private void calculateInitialRadius(int playerCount) {
        // Fórmula: Radio mínimo de 200 + 50 bloques por jugador adicional
        currentRadius = Math.max(200, 100 + (playerCount * 50));
        // Radio final: mínimo 50 + 10 por jugador
        targetRadius = Math.max(50, playerCount * 10);
    }

    private void teleportPlayersRandomly(ServerLevel level) {
        Random random = new Random();
        List<BlockPos> spawnPositions = generateSpawnPositions(alivePlayers.size(), random);

        for (int i = 0; i < alivePlayers.size(); i++) {
            ServerPlayer player = alivePlayers.get(i);
            BlockPos spawnPos = spawnPositions.get(i);

            BlockPos safePos = findSafeSpawnPosition(level, spawnPos);
            player.teleportTo(level, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, 0, 0);

            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
        }
    }

    private List<BlockPos> generateSpawnPositions(int playerCount, Random random) {
        List<BlockPos> positions = new ArrayList<>();
        double angleStep = 2 * Math.PI / playerCount;
        double spawnRadius = currentRadius * 0.7; // Spawn dentro del 70% del área inicial

        for (int i = 0; i < playerCount; i++) {
            double angle = i * angleStep + random.nextDouble() * angleStep * 0.3;
            double distance = spawnRadius * (0.4 + random.nextDouble() * 0.6);

            int x = centerX + (int)(Math.cos(angle) * distance);
            int z = centerZ + (int)(Math.sin(angle) * distance);
            positions.add(new BlockPos(x, 100, z)); // Empezar desde Y=100
        }

        return positions;
    }

    private BlockPos findSafeSpawnPosition(ServerLevel level, BlockPos start) {
        int maxHeight = level.getMaxBuildHeight() - 10; // Un poco por debajo del límite

        for (int y = maxHeight; y > level.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(start.getX(), y, start.getZ());

            if (level.getBlockState(checkPos).isSolid()) {
                BlockPos spawnPos = checkPos.above();
                if (level.getBlockState(spawnPos).isAir() &&
                        level.getBlockState(spawnPos.above()).isAir()) {
                    if (hasAccessToSky(level, spawnPos)) {
                        return spawnPos;
                    }
                }
            }
        }

        int surfaceY = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, start).getY();
        return new BlockPos(start.getX(), surfaceY + 1, start.getZ());
    }

    private boolean hasAccessToSky(ServerLevel level, BlockPos pos) {
        for (int y = pos.getY(); y < level.getMaxBuildHeight(); y++) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (level.getBlockState(checkPos).isSolid()) {
                return false;
            }
        }
        return true;
    }
    private void setupWorldBorder(ServerLevel level) {
        WorldBorder border = level.getWorldBorder();
        border.setCenter(centerX, centerZ);
        border.setSize(currentRadius * 2); // Diámetro = radio * 2
    }

    public void tick() {
        if (!gameActive) return;

        shrinkTimer++;
        if (shrinkTimer >= SHRINK_INTERVAL) {
            shrinkZone();
            shrinkTimer = 0;
        }
        checkWinCondition();
    }

    private void shrinkZone() {
        if (currentRadius <= targetRadius) {
            return; // Ya llegamos al tamaño mínimo
        }

        MinecraftServer server = getServer();
        if (server == null) return;

        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) return;

        // Reducir radio gradualmente (20% cada vez)
        double reductionRate = 0.8;
        currentRadius = Math.max(targetRadius, currentRadius * reductionRate);

        WorldBorder border = level.getWorldBorder();
        // Reducir gradualmente durante 20 segundos
        border.lerpSizeBetween(border.getSize(), currentRadius * 2, 20000);

        broadcastMessage("¡La zona se está reduciendo! Nuevo radio: " + (int)currentRadius + " bloques");
        broadcastMessage("Tienes 20 segundos para moverte a la zona segura!");
//        showTitleToAll("¡La zona se está reduciendo!", "Tienes 20 segundos para moverte a la zona segura!");
    }

    public void onPlayerDeath(ServerPlayer player) {
        if (!gameActive || !alivePlayers.contains(player)) {
            return;
        }
        alivePlayers.remove(player);
        deadPlayers.add(player);

        if (player.getHealth() <= 0) {
            player.setHealth(1.0f);
        }

        player.setGameMode(GameType.SPECTATOR);
        broadcastMessage(player.getName().getString() + " ha sido eliminado! Jugadores restantes: " + alivePlayers.size());
        checkWinCondition();
    }

    private void checkWinCondition() {
        if (alivePlayers.size() == 1) {
            ServerPlayer winner = alivePlayers.get(0);
            broadcastMessage("¡¡¡" + winner.getName().getString() + " ha ganado el Battle Royale!!!");
//            broadcastMessage("¡Felicidades por ser el último superviviente!");
            showTitle(winner, "§6¡VICTORIA!", "§eEres el último superviviente");
            winner.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
            showPlayerNametags();
            stopGame();
        } else if (alivePlayers.isEmpty()) {
            broadcastMessage("¡Empate! Todos los jugadores fueron eliminados.");
            stopGame();
        }
    }

    private void teleportAllToWinner(ServerPlayer winner) {
        MinecraftServer server = getServer();
        if (server == null) return;

        ServerLevel level = winner.serverLevel();
        BlockPos winnerPos = winner.blockPosition();

        List<ServerPlayer> allPlayers = server.getPlayerList().getPlayers();

        for (ServerPlayer player : allPlayers) {
            if (player != winner) {
                BlockPos teleportPos = findSafeTeleportPosition(level, winnerPos, allPlayers.indexOf(player));
                player.teleportTo(level, teleportPos.getX() + 0.5, teleportPos.getY(), teleportPos.getZ() + 0.5, 0, 0);
            }
        }
    }

    private BlockPos findSafeTeleportPosition(ServerLevel level, BlockPos center, int playerIndex) {
        double angle = (playerIndex * 2 * Math.PI) / 8; // Máximo 8 posiciones
        double radius = 3 + (playerIndex / 8) * 2; // Radio aumenta con más jugadores

        int x = center.getX() + (int)(Math.cos(angle) * radius);
        int z = center.getZ() + (int)(Math.sin(angle) * radius);

        BlockPos targetPos = new BlockPos(x, center.getY(), z);
        return findSafeSpawnPosition(level, targetPos);
    }

    private void broadcastMessage(String message) {
        MinecraftServer server = getServer();
        if (server != null) {
            Component chatMessage = Component.literal("§6[Zone Royale] §f" + message);
            server.getPlayerList().broadcastSystemMessage(chatMessage, false);
        }
    }

    private void showTitle(ServerPlayer player, String title, String subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
    }

    private void showTitleToAll(String title, String subtitle) {
        MinecraftServer server = getServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                showTitle(player, title, subtitle);
            }
        }
    }

    private void hidePlayerNametags() {
        MinecraftServer server = getServer();
        if (server == null) return;

        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) return;

        Scoreboard scoreboard = level.getScoreboard();

        PlayerTeam team = scoreboard.getPlayerTeam("battleRoyaleTeam");
        if (team == null) {
            team = scoreboard.addPlayerTeam("battleRoyaleTeam");
        }

        team.setNameTagVisibility(Team.Visibility.NEVER);

        for (ServerPlayer player : alivePlayers) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        }
    }

    private void showPlayerNametags() {
        MinecraftServer server = getServer();
        if (server == null) return;

        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) return;

        Scoreboard scoreboard = level.getScoreboard();

        PlayerTeam team = scoreboard.getPlayerTeam("battleRoyaleTeam");
        if (team != null) {
            for (String playerName : new ArrayList<>(team.getPlayers())) {
                scoreboard.removePlayerFromTeam(playerName, team);
            }

            scoreboard.removePlayerTeam(team);
        }
    }

    private MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public void setCenter(int x, int z) {
        this.centerX = x;
        this.centerZ = z;
    }

    public boolean isGameActive() {
        return gameActive;
    }

    public List<ServerPlayer> getAlivePlayers() {
        return new ArrayList<>(alivePlayers);
    }

    public int getPlayerCount() {
        return alivePlayers.size();
    }

    public double getCurrentRadius() {
        return currentRadius;
    }
}