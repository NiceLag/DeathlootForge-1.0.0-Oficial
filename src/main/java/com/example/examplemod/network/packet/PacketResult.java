package com.example.examplemod.network.packet;

import com.example.examplemod.client.ClientDeathHistory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketResult {

    public final byte action;
    public final boolean success;
    public final String message;

    public PacketResult(byte action, boolean success, String message) {
        this.action = action;
        this.success = success;
        this.message = message;
    }

    public static void encode(PacketResult packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.action);
        buffer.writeBoolean(packet.success);
        buffer.writeUtf(packet.message, 128);
    }

    public static PacketResult decode(FriendlyByteBuf buffer) {
        return new PacketResult(buffer.readByte(), buffer.readBoolean(), buffer.readUtf(128));
    }

    public static void handle(PacketResult packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientDeathHistory.result(packet));
        context.setPacketHandled(true);
    }
}