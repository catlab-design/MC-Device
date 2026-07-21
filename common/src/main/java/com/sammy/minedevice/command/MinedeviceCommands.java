package com.sammy.minedevice.command;

import com.mojang.brigadier.CommandDispatcher;
import com.sammy.minedevice.atm.AtmConfigStore;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class MinedeviceCommands {
    private MinedeviceCommands() {
    }

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("minedevice")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("atm")
                        .then(Commands.literal("require-card")
                                .then(Commands.literal("true")
                                        .executes(ctx -> setRequireCard(ctx.getSource(), true)))
                                .then(Commands.literal("false")
                                        .executes(ctx -> setRequireCard(ctx.getSource(), false))))));
    }

    private static int setRequireCard(CommandSourceStack source, boolean required) {
        MinecraftServer server = source.getServer();
        AtmConfigStore config = AtmConfigStore.get(server);
        config.setCardRequired(required);
        source.sendSuccess(() -> Component.literal(
                "ATM card requirement set to " + required), true);
        return 1;
    }
}
