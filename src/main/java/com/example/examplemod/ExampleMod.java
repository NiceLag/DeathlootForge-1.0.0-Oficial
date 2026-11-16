package com.example.examplemod;

import com.example.examplemod.storage.ModBlockEntities;
import com.example.examplemod.storage.ModBlocks;
import com.example.examplemod.storage.ModMenus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import com.example.examplemod.network.DeathLootNetwork;

@Mod(ExampleMod.MODID)
public class ExampleMod {

    public static final String MODID = "deathlootforge";

    public ExampleMod(
            net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context
    ) {
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ModItems.register(context.getModEventBus());
        ModCreativeTabs.register(context.getModEventBus());
        ModBlocks.register(context.getModEventBus());
        ModBlockEntities.register(context.getModEventBus());
        ModMenus.register(context.getModEventBus());
        AegisNetwork.register();
        DeathLootNetwork.register();
    }
}