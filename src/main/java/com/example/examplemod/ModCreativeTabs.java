package com.example.examplemod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Pestana propia. El nombre de la pestana es lo que aparece como
 * cabecera del tooltip en el inventario creativo, asi que se llama
 * "Informacion:" en lugar de "Materiales".
 */
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ExampleMod.MODID);

    public static final RegistryObject<CreativeModeTab> AEGIS_TAB = TABS.register("aegis",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.deathlootforge.aegis"))
                    .icon(() -> new ItemStack(ModItems.AEGIS.get()))
                    .displayItems((params, output) -> output.accept(ModItems.AEGIS.get()))
                    .build());

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}