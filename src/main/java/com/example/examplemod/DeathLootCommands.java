
package com.example.examplemod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class DeathLootCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("deathloot")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("chance")
                                .then(Commands.argument(
                                                "numero",
                                                IntegerArgumentType.integer(0, 100)
                                        )
                                        .executes(context -> {
                                            int chance =
                                                    IntegerArgumentType.getInteger(
                                                            context, "numero"
                                                    );

                                            Config.setDeathLootChance(chance);

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "Probabilidad de pérdida de objetos: "
                                                                    + chance + "%"
                                                    ),
                                                    true
                                            );

                                            return 1;
                                        })
                                )
                        )
        );
    }
}
