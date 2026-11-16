package com.example.examplemod.network.packet;

import com.example.examplemod.server.EquipService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketEquipAll {

    public static void encode(PacketEquipAll packet, FriendlyByteBuf buffer) {
    }

    public static PacketEquipAll decode(FriendlyByteBuf buffer) {
        return new PacketEquipAll();
    }

    public static void handle(PacketEquipAll packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }
        context.enqueueWork(() -> EquipService.equipAll(player));
        context.setPacketHandled(true);
    }
}