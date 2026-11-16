package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.ModKeybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientInputHandler {

    private static long shownResult;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (ModKeybinds.HISTORY.consumeClick()) {
    Screen screen = minecraft.screen;

    if (screen instanceof DeathHistoryScreen
            || screen instanceof ServerHistoryScreen
            || screen instanceof DeathDetailsScreen
            || screen instanceof BackpackViewScreen) {
        minecraft.setScreen(null);
    } else if (minecraft.player != null) {
        minecraft.setScreen(new DeathHistoryScreen());
    }
}

        if (ClientDeathHistory.consumeOpen() && minecraft.screen == null) {
            minecraft.setScreen(new DeathHistoryScreen());
        }

        if (ClientDeathHistory.consumeServerHistory() && minecraft.screen == null) {
            minecraft.setScreen(new ServerHistoryScreen(null));
        }

        if (minecraft.screen instanceof DeathHistoryScreen history) {
            var details = ClientDeathHistory.takeDetails();
            if (details != null) {
                minecraft.setScreen(new DeathDetailsScreen(details, history));
            }

            long time = ClientDeathHistory.lastResultTime();
            if (time != shownResult) {
                shownResult = time;
                history.notice(ClientDeathHistory.lastSuccess(), ClientDeathHistory.lastMessage());
            }
        }
    }
}