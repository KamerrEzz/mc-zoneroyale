package com.kamerrezz.zoneroyale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ZoneRoyaleCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("zoneroyale")
                .requires(source -> source.hasPermission(2)) // Requiere permisos de operador (nivel 2)
                .then(Commands.literal("start")
                        .executes(ZoneRoyaleCommands::startGame)
                )
                .then(Commands.literal("stop")
                        .executes(ZoneRoyaleCommands::stopGame)
                )
                .then(Commands.literal("setcenter")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(ZoneRoyaleCommands::setCenter)
                                ))
                )
                .then(Commands.literal("status")
                        .executes(ZoneRoyaleCommands::showStatus)
                )
                .then(Commands.literal("help")
                        .executes(ZoneRoyaleCommands::showHelp)
                )
        );
    }

    private static int startGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BattleRoyaleManager manager = BattleRoyaleManager.getInstance();

        if (manager.isGameActive()) {
            context.getSource().sendFailure(Component.literal("§c¡Ya hay un Battle Royale en curso!"));
            return 0;
        }

        manager.startGame();
//        context.getSource().sendSuccess(
//                () -> Component.literal("§a¡Battle Royale iniciado exitosamente!"),
//                true
//        );
        return 1;
    }

    private static int stopGame(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BattleRoyaleManager manager = BattleRoyaleManager.getInstance();

        if (!manager.isGameActive()) {
            context.getSource().sendFailure(Component.literal("§cNo hay ningún Battle Royale activo."));
            return 0;
        }

        manager.stopGame();
        context.getSource().sendSuccess(
                () -> Component.literal("§aBattle Royale detenido."),
                true
        );
        return 1;
    }

    private static int setCenter(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int x = IntegerArgumentType.getInteger(context, "x");
        int z = IntegerArgumentType.getInteger(context, "z");

        BattleRoyaleManager.getInstance().setCenter(x, z);

        context.getSource().sendSuccess(
                () -> Component.literal("§aCentro de la zona establecido en: §f" + x + ", " + z),
                true
        );
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BattleRoyaleManager manager = BattleRoyaleManager.getInstance();

        if (!manager.isGameActive()) {
            context.getSource().sendSuccess(
                    () -> Component.literal("§eNo hay ningún Battle Royale activo."),
                    false
            );
        } else {
            context.getSource().sendSuccess(
                    () -> Component.literal("§aBattle Royale ACTIVO"),
                    false
            );
            context.getSource().sendSuccess(
                    () -> Component.literal("§eJugadores vivos: §f" + manager.getPlayerCount()),
                    false
            );
            context.getSource().sendSuccess(
                    () -> Component.literal("§eRadio actual: §f" + (int)manager.getCurrentRadius() + " bloques"),
                    false
            );
        }
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        context.getSource().sendSuccess(
                () -> Component.literal("§6=== Zone Royale Commands ==="),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§e/zoneroyale start §7- Inicia un nuevo Battle Royale"),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§e/zoneroyale stop §7- Detiene el Battle Royale actual"),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§e/zoneroyale setcenter <x> <z> §7- Establece el centro de la zona"),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§e/zoneroyale status §7- Muestra el estado actual del juego"),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§e/zoneroyale help §7- Muestra esta ayuda"),
                false
        );
        return 1;
    }
}