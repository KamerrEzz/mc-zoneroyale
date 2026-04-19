package com.kamerrezz.zoneroyale.game;

import com.kamerrezz.zoneroyale.ZoneRoyaleConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;

class ZoneController {
    private static final double VANILLA_BORDER_SIZE = 60000000;

    private int centerX = 0;
    private int centerZ = 0;
    private double currentRadius = 1000;
    private double targetRadius = 10;

    void start(ServerLevel level, int playerCount) {
        currentRadius = Math.max(
                ZoneRoyaleConfig.initialRadiusMin,
                100 + playerCount * ZoneRoyaleConfig.initialRadiusPerPlayer
        );
        targetRadius = Math.max(
                ZoneRoyaleConfig.targetRadiusMin,
                playerCount * ZoneRoyaleConfig.targetRadiusPerPlayer
        );

        WorldBorder border = level.getWorldBorder();
        border.setCenter(centerX, centerZ);
        border.setSize(currentRadius * 2);
    }

    boolean shrink(ServerLevel level) {
        if (currentRadius <= targetRadius) return false;

        currentRadius = Math.max(targetRadius, currentRadius * ZoneRoyaleConfig.shrinkRatio);
        WorldBorder border = level.getWorldBorder();
        border.lerpSizeBetween(border.getSize(), currentRadius * 2, ZoneRoyaleConfig.shrinkLerpMillis);
        return true;
    }

    void reset(ServerLevel level) {
        level.getWorldBorder().setSize(VANILLA_BORDER_SIZE);
    }

    void setCenter(int x, int z) {
        this.centerX = x;
        this.centerZ = z;
    }

    int getCenterX() { return centerX; }
    int getCenterZ() { return centerZ; }
    double getCurrentRadius() { return currentRadius; }
}
