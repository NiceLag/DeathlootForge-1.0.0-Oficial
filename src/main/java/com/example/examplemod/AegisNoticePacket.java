package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AegisNoticePacket {

    private final int type;
    private final int value;

    public AegisNoticePacket(int type, int value) {
        this.type = type;
        this.value = value;
    }

    public static AegisNoticePacket decode(FriendlyByteBuf buffer) {
        return new AegisNoticePacket(buffer.readByte(), buffer.readVarInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(type);
        buffer.writeVarInt(value);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> AegisNotices.show(type, value));
        context.get().setPacketHandled(true);
    }
}