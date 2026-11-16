package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.packet.PacketAction;
import com.example.examplemod.network.packet.PacketCredits;
import com.example.examplemod.network.packet.PacketDetails;
import com.example.examplemod.network.packet.PacketDetailsRequest;
import com.example.examplemod.network.packet.PacketRequestHistory;
import com.example.examplemod.network.packet.PacketResult;
import com.example.examplemod.network.packet.PacketServerHistory;
import com.example.examplemod.network.packet.PacketSyncHistory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import com.example.examplemod.network.packet.PacketEquipAll;

import com.example.examplemod.client.ClientDeathHistory;

public final class DeathLootNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ExampleMod.MODID, "history"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private DeathLootNetwork() {
    }

    public static void register() {
        int id = 0;

        CHANNEL.messageBuilder(PacketRequestHistory.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketRequestHistory::encode).decoder(PacketRequestHistory::decode)
                .consumerMainThread(PacketRequestHistory::handle).add();

        CHANNEL.messageBuilder(PacketAction.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketAction::encode).decoder(PacketAction::decode)
                .consumerMainThread(PacketAction::handle).add();

        CHANNEL.messageBuilder(PacketSyncHistory.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketSyncHistory::encode).decoder(PacketSyncHistory::decode)
                .consumerMainThread(PacketSyncHistory::handle).add();

        CHANNEL.messageBuilder(PacketResult.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketResult::encode).decoder(PacketResult::decode)
                .consumerMainThread(PacketResult::handle).add();

        CHANNEL.messageBuilder(PacketDetails.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketDetails::encode).decoder(PacketDetails::decode)
                .consumerMainThread(PacketDetails::handle).add();

        CHANNEL.messageBuilder(PacketDetailsRequest.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketDetailsRequest::encode).decoder(PacketDetailsRequest::decode)
                .consumerMainThread(PacketDetailsRequest::handle).add();

        CHANNEL.messageBuilder(PacketCredits.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketCredits::encode).decoder(PacketCredits::decode)
                .consumerMainThread(PacketCredits::handle).add();

        CHANNEL.messageBuilder(PacketServerHistory.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PacketServerHistory::encode).decoder(PacketServerHistory::decode)
                .consumerMainThread(PacketServerHistory::handle).add();

        CHANNEL.messageBuilder(PacketEquipAll.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PacketEquipAll::encode).decoder(PacketEquipAll::decode)
                .consumerMainThread(PacketEquipAll::handle).add();
    }

    public static void toServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void toPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void requestHistory(boolean admin) {
    requestHistory(admin, true);
}

public static void requestHistory(boolean admin, boolean open) {
    ClientDeathHistory.expectOpen(open);
    toServer(new PacketRequestHistory(admin));
}

    public static void action(byte action, java.util.UUID recordId, String text, int value) {
        toServer(new PacketAction(action, recordId, text, value));
    }
}