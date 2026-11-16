package com.example.examplemod.network.packet;

import com.example.examplemod.client.ClientDeathHistory;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketServerHistory {

    public final ListTag entries;

    public PacketServerHistory(ListTag entries) {
        this.entries = entries;
    }

    public static void encode(PacketServerHistory packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(entries(packet.entries));
    }

    private static net.minecraft.nbt.CompoundTag entries(ListTag list) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.put("L", list);
        return tag;
    }

    public static PacketServerHistory decode(FriendlyByteBuf buffer) {
        return new PacketServerHistory(buffer.readNbt().getList("L", 10));
    }

    public static void handle(PacketServerHistory packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientDeathHistory.serverHistory(packet.entries));
        context.setPacketHandled(true);
    }
}