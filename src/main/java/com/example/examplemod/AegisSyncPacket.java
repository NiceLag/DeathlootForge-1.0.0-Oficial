package com.example.examplemod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AegisSyncPacket {

    private final int charges;

    public AegisSyncPacket(int charges) {
        this.charges = charges;
    }

    public static AegisSyncPacket decode(FriendlyByteBuf buffer) {
        return new AegisSyncPacket(buffer.readVarInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(charges);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> ClientAegisData.setCharges(charges));
        context.get().setPacketHandled(true);
    }
}