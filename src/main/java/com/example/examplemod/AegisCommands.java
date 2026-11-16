package com.example.examplemod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AegisCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("aegis")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("get")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int charges = AegisData.getCharges(player);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Protecciones de muerte: " + charges), false);
                                    return charges;
                                }))
                        .then(Commands.literal("set")
                                .then(Commands.argument("cantidad", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(context, "cantidad");
                                            AegisData.setCharges(player, amount);
                                            AegisNetwork.sendSync(player, amount);
                                            return amount;
                                        })))
                        .then(Commands.literal("add")
                                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(context, "cantidad");
                                            int total = AegisData.addCharges(player, amount);
                                            AegisNetwork.sendSync(player, total);
                                            return total;
                                        })))
        );
    }
}