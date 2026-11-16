package com.example.examplemod.network.packet;

import com.example.examplemod.server.HistoryService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketAction {

    public static final byte EXTEND = 0;
    public static final byte RENAME = 1;
    public static final byte TELEPORT = 2;
    public static final byte PIN = 3;
    public static final byte DELETE = 4;
    public static final byte CHECKPOINT = 5;
    public static final byte COLOR = 6;
    public static final byte ICON = 7;
    public static final byte HIDE = 8;
    public static final byte INITIAL = 9;

    private final byte action;
    private final UUID recordId;
    private final String text;
    private final int value;

    public PacketAction(byte action, UUID recordId, String text, int value) {
        this.action = action;
        this.recordId = recordId;
        this.text = text == null ? "" : text;
        this.value = value;
    }

    public static void encode(PacketAction packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.action);
        buffer.writeUUID(packet.recordId);
        buffer.writeUtf(packet.text, 64);
        buffer.writeVarInt(packet.value);
    }

    public static PacketAction decode(FriendlyByteBuf buffer) {
        return new PacketAction(buffer.readByte(), buffer.readUUID(), buffer.readUtf(64), buffer.readVarInt());
    }

    public static void handle(PacketAction packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }
        context.enqueueWork(() -> HistoryService.handle(player, packet.action, packet.recordId, packet.text, packet.value));
        context.setPacketHandled(true);
    }
}