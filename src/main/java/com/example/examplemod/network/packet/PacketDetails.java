package com.example.examplemod.network.packet;

import com.example.examplemod.client.ClientDeathHistory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketDetails {

    public final CompoundTag record;

    public PacketDetails(CompoundTag record) {
        this.record = record;
    }

    public static void encode(PacketDetails packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.record);
    }

    public static PacketDetails decode(FriendlyByteBuf buffer) {
        return new PacketDetails(buffer.readNbt());
    }

    public static void handle(PacketDetails packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientDeathHistory.details(packet.record));
        context.setPacketHandled(true);
    }
}