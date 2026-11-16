package com.example.examplemod.network.packet;

import com.example.examplemod.client.ClientDeathHistory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketCredits {

    public final long credits;

    public PacketCredits(long credits) {
        this.credits = credits;
    }

    public static void encode(PacketCredits packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.credits);
    }

    public static PacketCredits decode(FriendlyByteBuf buffer) {
        return new PacketCredits(buffer.readLong());
    }

    public static void handle(PacketCredits packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientDeathHistory.setCredits(packet.credits));
        context.setPacketHandled(true);
    }
}