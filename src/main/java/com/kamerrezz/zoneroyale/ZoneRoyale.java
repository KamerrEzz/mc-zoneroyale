package com.kamerrezz.zoneroyale;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ZoneRoyale.MOD_ID)
public class ZoneRoyale {
    public static final String MOD_ID = "zoneroyale";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ZoneRoyale(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new BattleRoyaleEventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Zone Royale Mod inicializando...");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Zone Royale Mod: Servidor iniciando - Comandos registrados");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Zone Royale Mod: Registrando comandos...");
        ZoneRoyaleCommands.register(event.getDispatcher());
        LOGGER.info("Zone Royale Mod: Comandos registrados exitosamente!");
    }
}