package com.example.examplemod;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ExampleMod.MODID);

    public static final RegistryObject<Item> AEGIS = ITEMS.register("aegis",
            () -> new AegisItem(new Item.Properties()
                    .stacksTo(64)
                    .rarity(Rarity.RARE)
                    .fireResistant()
                    .food(new FoodProperties.Builder()
                            .nutrition(0)
                            .saturationMod(0.0F)
                            .alwaysEat()
                            .build())));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}