
package com.example.examplemod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER =
            new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue DEATH_LOOT_CHANCE =
            BUILDER
                    .comment("Chance (0-100) to lose affected inventory items on death")
                    .defineInRange("deathLootChance", 100, 0, 100);

                    private static final ForgeConfigSpec.IntValue STORAGE_MINUTES =
        BUILDER
                .comment("Minutos que un almacen de muerte permanece antes de expirar")
                .defineInRange("storageMinutes", 5, 1, 1440);

private static final ForgeConfigSpec.IntValue MAX_ACTIVE_STORAGES =
        BUILDER
                .comment("Almacenes activos maximos por jugador")
                .defineInRange("maxActiveStorages", 10, 1, 50);

private static final ForgeConfigSpec.IntValue MAX_HISTORY_ENTRIES =
        BUILDER
                .comment("Entradas de historial no fijadas antes de descartar las mas antiguas")
                .defineInRange("maxHistoryEntries", 15, 1, 15);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int deathLootChance = 100;
    public static int storageMinutes = 5;
    public static int maxActiveStorages = 10;
    public static int maxHistoryEntries = 15;

    private static ModConfig registeredConfig;

    public static void setDeathLootChance(int chance) {
        if (chance < 0 || chance > 100) {
            throw new IllegalArgumentException(
                    "La probabilidad debe estar entre 0 y 100"
            );
        }

        DEATH_LOOT_CHANCE.set(chance);
        deathLootChance = chance;

        if (registeredConfig != null) {
            registeredConfig.save();
        }
    }

    @SubscribeEvent
    static void onConfigLoad(ModConfigEvent.Loading event) {
    if (event.getConfig().getSpec() == SPEC) {
        registeredConfig = event.getConfig();

        deathLootChance = DEATH_LOOT_CHANCE.get();
        storageMinutes = STORAGE_MINUTES.get();
        maxActiveStorages = MAX_ACTIVE_STORAGES.get();
        maxHistoryEntries = MAX_HISTORY_ENTRIES.get();
    }
}

    @SubscribeEvent
    static void onConfigReload(ModConfigEvent.Reloading event) {
    if (event.getConfig().getSpec() == SPEC) {
        deathLootChance = DEATH_LOOT_CHANCE.get();
        storageMinutes = STORAGE_MINUTES.get();
        maxActiveStorages = MAX_ACTIVE_STORAGES.get();
        maxHistoryEntries = MAX_HISTORY_ENTRIES.get();
    }
}
}
