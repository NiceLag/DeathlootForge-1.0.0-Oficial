package com.example.examplemod.network.packet;

import com.example.examplemod.server.HistoryService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketDetailsRequest {

    private final UUID recordId;

    public PacketDetailsRequest(UUID recordId) {
        this.recordId = recordId;
    }

    public static void encode(PacketDetailsRequest packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.recordId);
    }

    public static PacketDetailsRequest decode(FriendlyByteBuf buffer) {
        return new PacketDetailsRequest(buffer.readUUID());
    }

    public static void handle(PacketDetailsRequest packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }
        context.enqueueWork(() -> HistoryService.sendDetails(player, packet.recordId));
        context.setPacketHandled(true);
    }
}