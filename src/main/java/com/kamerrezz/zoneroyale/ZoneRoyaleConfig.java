package com.kamerrezz.zoneroyale;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ZoneRoyale.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZoneRoyaleConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue SHRINK_INTERVAL_SECONDS = BUILDER
            .comment("Segundos entre cada reducción de la zona.")
            .defineInRange("shrinkIntervalSeconds", 30, 5, 600);

    private static final ForgeConfigSpec.DoubleValue SHRINK_RATIO = BUILDER
            .comment("Factor aplicado al radio en cada reducción (0.8 = reduce 20%).")
            .defineInRange("shrinkRatio", 0.8, 0.1, 0.99);

    private static final ForgeConfigSpec.IntValue SHRINK_LERP_MILLIS = BUILDER
            .comment("Duración (ms) de la transición visual del WorldBorder al reducir.")
            .defineInRange("shrinkLerpMillis", 20000, 1000, 120000);

    private static final ForgeConfigSpec.IntValue INITIAL_RADIUS_MIN = BUILDER
            .comment("Radio inicial mínimo (bloques).")
            .defineInRange("initialRadiusMin", 200, 50, 10000);

    private static final ForgeConfigSpec.IntValue INITIAL_RADIUS_PER_PLAYER = BUILDER
            .comment("Bloques adicionales de radio inicial por cada jugador. Radio = max(initialRadiusMin, 100 + players * este valor).")
            .defineInRange("initialRadiusPerPlayer", 50, 0, 500);

    private static final ForgeConfigSpec.IntValue TARGET_RADIUS_MIN = BUILDER
            .comment("Radio final mínimo (bloques).")
            .defineInRange("targetRadiusMin", 50, 5, 10000);

    private static final ForgeConfigSpec.IntValue TARGET_RADIUS_PER_PLAYER = BUILDER
            .comment("Bloques de radio final por cada jugador. Radio = max(targetRadiusMin, players * este valor).")
            .defineInRange("targetRadiusPerPlayer", 10, 0, 500);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int shrinkIntervalSeconds;
    public static double shrinkRatio;
    public static int shrinkLerpMillis;
    public static int initialRadiusMin;
    public static int initialRadiusPerPlayer;
    public static int targetRadiusMin;
    public static int targetRadiusPerPlayer;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        shrinkIntervalSeconds = SHRINK_INTERVAL_SECONDS.get();
        shrinkRatio = SHRINK_RATIO.get();
        shrinkLerpMillis = SHRINK_LERP_MILLIS.get();
        initialRadiusMin = INITIAL_RADIUS_MIN.get();
        initialRadiusPerPlayer = INITIAL_RADIUS_PER_PLAYER.get();
        targetRadiusMin = TARGET_RADIUS_MIN.get();
        targetRadiusPerPlayer = TARGET_RADIUS_PER_PLAYER.get();
    }
}
