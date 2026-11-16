package com.example.examplemod.network.packet;

import com.example.examplemod.client.ClientDeathHistory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncHistory {

    public final ListTag records;
    public final ListTag pinned;
    public final ListTag active;
    public final long credits;
    public final boolean creditsAvailable;
    public final int teleportIndex;
    public final int maxHistory;
    public final int maxActive;

    public PacketSyncHistory(ListTag records, ListTag pinned, ListTag active, long credits,
                             boolean creditsAvailable, int teleportIndex, int maxHistory, int maxActive) {
        this.records = records;
        this.pinned = pinned;
        this.active = active;
        this.credits = credits;
        this.creditsAvailable = creditsAvailable;
        this.teleportIndex = teleportIndex;
        this.maxHistory = maxHistory;
        this.maxActive = maxActive;
    }

    public static void encode(PacketSyncHistory packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(compound(packet.records, "R"));
        buffer.writeNbt(compound(packet.pinned, "R"));
        buffer.writeNbt(compound(packet.active, "A"));
        buffer.writeLong(packet.credits);
        buffer.writeBoolean(packet.creditsAvailable);
        buffer.writeVarInt(packet.teleportIndex);
        buffer.writeVarInt(packet.maxHistory);
        buffer.writeVarInt(packet.maxActive);
    }

    private static CompoundTag compound(ListTag list, String key) {
        CompoundTag tag = new CompoundTag();
        tag.put(key, list);
        return tag;
    }

    public static PacketSyncHistory decode(FriendlyByteBuf buffer) {
        return new PacketSyncHistory(
                buffer.readNbt().getList("R", 10),
                buffer.readNbt().getList("R", 10),
                buffer.readNbt().getList("A", 10),
                buffer.readLong(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt());
    }

    public static void handle(PacketSyncHistory packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientDeathHistory.accept(packet));
        context.setPacketHandled(true);
    }
}