package com.kamerrezz.zoneroyale.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class PlayerManager {
    private final List<ServerPlayer> alivePlayers = new ArrayList<>();
    private final List<ServerPlayer> deadPlayers = new ArrayList<>();

    void startRound(List<ServerPlayer> allPlayers) {
        alivePlayers.clear();
        deadPlayers.clear();
        alivePlayers.addAll(allPlayers);
    }

    void clear() {
        alivePlayers.clear();
        deadPlayers.clear();
    }

    boolean isAlive(ServerPlayer player) {
        return alivePlayers.contains(player);
    }

    int aliveCount() {
        return alivePlayers.size();
    }

    List<ServerPlayer> getAlive() {
        return Collections.unmodifiableList(alivePlayers);
    }

    void kill(ServerPlayer player) {
        alivePlayers.remove(player);
        deadPlayers.add(player);
        if (player.getHealth() <= 0) {
            player.setHealth(1.0f);
        }
        player.setGameMode(GameType.SPECTATOR);
    }

    void putSpectator(ServerPlayer player) {
        player.setGameMode(GameType.SPECTATOR);
    }

    void spawnAll(ServerLevel level, int centerX, int centerZ, double radius) {
        Random random = new Random();
        List<BlockPos> positions = generateSpawnPositions(alivePlayers.size(), centerX, centerZ, radius, random);

        for (int i = 0; i < alivePlayers.size(); i++) {
            ServerPlayer player = alivePlayers.get(i);
            BlockPos safePos = findSafeSpawnPosition(level, positions.get(i));
            player.teleportTo(level, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, 0, 0);
            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
        }
    }

    void teleportAllToWinner(ServerLevel level, ServerPlayer winner, List<ServerPlayer> allPlayers) {
        BlockPos winnerPos = winner.blockPosition();
        int index = 0;
        for (ServerPlayer player : allPlayers) {
            if (player == winner) continue;
            BlockPos teleportPos = findSafeTeleportPosition(level, winnerPos, index++);
            player.teleportTo(level, teleportPos.getX() + 0.5, teleportPos.getY(), teleportPos.getZ() + 0.5, 0, 0);
        }
    }

    private List<BlockPos> generateSpawnPositions(int playerCount, int centerX, int centerZ, double radius, Random random) {
        List<BlockPos> positions = new ArrayList<>();
        double angleStep = 2 * Math.PI / playerCount;
        double spawnRadius = radius * 0.7;

        for (int i = 0; i < playerCount; i++) {
            double angle = i * angleStep + random.nextDouble() * angleStep * 0.3;
            double distance = spawnRadius * (0.4 + random.nextDouble() * 0.6);

            int x = centerX + (int)(Math.cos(angle) * distance);
            int z = centerZ + (int)(Math.sin(angle) * distance);
            positions.add(new BlockPos(x, 100, z));
        }
        return positions;
    }

    private BlockPos findSafeSpawnPosition(ServerLevel level, BlockPos start) {
        int maxHeight = level.getMaxBuildHeight() - 10;

        for (int y = maxHeight; y > level.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(start.getX(), y, start.getZ());
            if (level.getBlockState(checkPos).isSolid()) {
                BlockPos spawnPos = checkPos.above();
                if (level.getBlockState(spawnPos).isAir()
                        && level.getBlockState(spawnPos.above()).isAir()
                        && hasAccessToSky(level, spawnPos)) {
                    return spawnPos;
                }
            }
        }

        int surfaceY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, start).getY();
        return new BlockPos(start.getX(), surfaceY + 1, start.getZ());
    }

    private boolean hasAccessToSky(ServerLevel level, BlockPos pos) {
        for (int y = pos.getY(); y < level.getMaxBuildHeight(); y++) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (level.getBlockState(checkPos).isSolid()) return false;
        }
        return true;
    }

    private BlockPos findSafeTeleportPosition(ServerLevel level, BlockPos center, int playerIndex) {
        double angle = (playerIndex * 2 * Math.PI) / 8;
        double radius = 3 + (playerIndex / 8) * 2;

        int x = center.getX() + (int)(Math.cos(angle) * radius);
        int z = center.getZ() + (int)(Math.sin(angle) * radius);

        return findSafeSpawnPosition(level, new BlockPos(x, center.getY(), z));
    }
}
