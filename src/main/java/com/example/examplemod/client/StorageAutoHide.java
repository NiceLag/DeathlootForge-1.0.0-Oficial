package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.DeathLootNetwork;
import com.example.examplemod.network.packet.PacketAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Al abrir un almacen por primera vez su waypoint pasa a oculto. */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class StorageAutoHide {

    private static Screen previous;

    private StorageAutoHide() {
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;

        if (screen == previous) {
            return;
        }
        previous = screen;

        if (screen == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (!screen.getClass().getSimpleName().contains("DeathStorage")) {
            return;
        }

        String dimension = minecraft.level.dimension().location().toString();

        ClientDeathHistory.Active nearest = null;
        double best = 12.0D * 12.0D;

        for (ClientDeathHistory.Active active : ClientDeathHistory.active()) {
            if (active.hidden || !dimension.equals(active.dimension)) {
                continue;
            }

            double distance = active.pos.distSqr(minecraft.player.blockPosition());
            if (distance <= best) {
                best = distance;
                nearest = active;
            }
        }

        if (nearest == null) {
            return;
        }

        ClientDeathHistory.toggleHidden(nearest.recordId);
        DeathLootNetwork.action(PacketAction.HIDE, nearest.recordId, "", 1);
    }
}