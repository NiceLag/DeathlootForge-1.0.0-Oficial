package com.example.examplemod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AegisNetwork {

    public static final int NOTICE_GAINED = 0;
    public static final int NOTICE_PROTECTED = 1;

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ExampleMod.MODID, "aegis"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private AegisNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(AegisSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AegisSyncPacket::encode)
                .decoder(AegisSyncPacket::decode)
                .consumerMainThread(AegisSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(AegisNoticePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AegisNoticePacket::encode)
                .decoder(AegisNoticePacket::decode)
                .consumerMainThread(AegisNoticePacket::handle)
                .add();
    }

    public static void sendSync(ServerPlayer player, int charges) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AegisSyncPacket(charges));
    }

    public static void sendNotice(ServerPlayer player, int type, int value) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AegisNoticePacket(type, value));
    }
}