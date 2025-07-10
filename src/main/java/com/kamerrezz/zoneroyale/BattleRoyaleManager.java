package com.kamerrezz.zoneroyale;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
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
    private double targetRadius = 50;

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
    }

    public void onPlayerDeath(ServerPlayer player) {
        if (!gameActive || !alivePlayers.contains(player)) {
            return;
        }
        alivePlayers.remove(player);
        deadPlayers.add(player);
        player.setGameMode(GameType.SPECTATOR);
        broadcastMessage(player.getName().getString() + " ha sido eliminado! Jugadores restantes: " + alivePlayers.size());
        checkWinCondition();
    }

    private void checkWinCondition() {
        if (alivePlayers.size() == 1) {
            ServerPlayer winner = alivePlayers.get(0);
            broadcastMessage("¡¡¡" + winner.getName().getString() + " ha ganado el Battle Royale!!!");
            broadcastMessage("¡Felicidades por ser el último superviviente!");
            stopGame();
        } else if (alivePlayers.isEmpty()) {
            broadcastMessage("¡Empate! Todos los jugadores fueron eliminados.");
            stopGame();
        }
    }

    private void broadcastMessage(String message) {
        MinecraftServer server = getServer();
        if (server != null) {
            Component chatMessage = Component.literal("§6[Zone Royale] §f" + message);
            server.getPlayerList().broadcastSystemMessage(chatMessage, false);
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