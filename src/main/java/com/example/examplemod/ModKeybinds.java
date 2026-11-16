
package com.example.examplemod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeybinds {

    public static final KeyMapping HISTORY = new KeyMapping(
            "key.deathlootforge.history",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.deathlootforge");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(HISTORY);
    }
}
