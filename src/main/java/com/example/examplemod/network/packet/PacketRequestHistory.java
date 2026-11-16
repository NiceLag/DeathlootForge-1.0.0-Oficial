package com.example.examplemod.network.packet;

import com.example.examplemod.server.HistoryService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketRequestHistory {

    private final boolean admin;

    public PacketRequestHistory(boolean admin) {
        this.admin = admin;
    }

    public static void encode(PacketRequestHistory packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.admin);
    }

    public static PacketRequestHistory decode(FriendlyByteBuf buffer) {
        return new PacketRequestHistory(buffer.readBoolean());
    }

    public static void handle(PacketRequestHistory packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }
        context.enqueueWork(() -> HistoryService.sendHistory(player, packet.admin));
        context.setPacketHandled(true);
    }
}