package com.kamerrezz.zoneroyale;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class BattleRoyaleEventHandler {

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BattleRoyaleManager.getInstance().tick();
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        // Verificar si es un jugador del servidor
        if (event.getEntity() instanceof ServerPlayer player) {
            BattleRoyaleManager.getInstance().onPlayerDeath(player);
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BattleRoyaleManager manager = BattleRoyaleManager.getInstance();

            if (manager.isGameActive()) {
                player.setGameMode(GameType.SPECTATOR);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§6[Zone Royale] §eHay un Battle Royale en curso. Estás en modo espectador."));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§6[Zone Royale] §eJugadores restantes: " + manager.getPlayerCount()));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BattleRoyaleManager manager = BattleRoyaleManager.getInstance();

            if (manager.isGameActive()) {
                // Tratar la desconexión como una eliminación
                manager.onPlayerDeath(player);
            }
        }
    }

//    @SubscribeEvent
//    public void onRegisterCommands(RegisterCommandsEvent event) {
//        // Registrar nuestros comandos
//        ZoneRoyaleCommands.register(event.getDispatcher());
//    }
}