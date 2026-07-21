package com.sammy.minedevice.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.sammy.minedevice.atm.AtmAccountStore;
import com.sammy.minedevice.atm.AtmConfigStore;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Admin commands for the mod. Currently exposes reading and setting a player's
 * bank balance (the money used by the ATM / phone bank app).
 *
 * <pre>
 *   /minedevice money get &lt;player&gt;
 *   /minedevice money set &lt;player&gt; &lt;amount&gt;
 *   /minedevice money add &lt;player&gt; &lt;amount&gt;
 * </pre>
 */
public final class MinedeviceCommands {
    private MinedeviceCommands() {
    }

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("minedevice")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("money")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> getMoney(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                                .executes(ctx -> setMoney(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        LongArgumentType.getLong(ctx, "amount"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(ctx -> addMoney(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        LongArgumentType.getLong(ctx, "amount")))))))
                .then(Commands.literal("atm")
                        .then(Commands.literal("require-card")
                                .then(Commands.literal("true")
                                        .executes(ctx -> setRequireCard(ctx.getSource(), true)))
                                .then(Commands.literal("false")
                                        .executes(ctx -> setRequireCard(ctx.getSource(), false))))));
    }

    private static AtmAccountStore store(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        return AtmAccountStore.get(server);
    }

    private static int getMoney(CommandSourceStack source, ServerPlayer target) {
        long balance = store(source).getBalance(target.getUUID());
        source.sendSuccess(() -> Component.literal(
                target.getGameProfile().getName() + " has " + balance + " money"), false);
        return (int) Math.min(Integer.MAX_VALUE, balance);
    }

    private static int setMoney(CommandSourceStack source, ServerPlayer target, long amount) {
        long balance = store(source).setBalance(target.getUUID(), amount);
        source.sendSuccess(() -> Component.literal(
                "Set " + target.getGameProfile().getName() + "'s money to " + balance), true);
        return (int) Math.min(Integer.MAX_VALUE, balance);
    }

    private static int addMoney(CommandSourceStack source, ServerPlayer target, long amount) {
        long balance = store(source).deposit(target.getUUID(), amount);
        source.sendSuccess(() -> Component.literal(
                "Added " + amount + " to " + target.getGameProfile().getName()
                        + " (now " + balance + ")"), true);
        return (int) Math.min(Integer.MAX_VALUE, balance);
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
