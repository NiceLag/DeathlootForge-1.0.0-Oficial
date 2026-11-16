package com.example.examplemod.client;

import com.example.examplemod.network.packet.PacketResult;
import com.example.examplemod.network.packet.PacketSyncHistory;
import com.example.examplemod.storage.DeathRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.DeathLootNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClientDeathHistory {

    public static final class Active {
        public UUID storageId;
        public UUID recordId;
        public String name = "";
        public String icon = "";
        public int color = 0x8FE3FF;
        public int nameIndex;
        public long remaining;
        public long receivedAt;
        public String dimension = "";
        public BlockPos pos = BlockPos.ZERO;
        public String initial = "";
        public boolean hidden;
    }

    public static final class Lost {
        public String ownerName = "";
        public String dimension = "";
        public BlockPos pos = BlockPos.ZERO;
        public long time;
        public List<net.minecraft.world.item.ItemStack> items = new ArrayList<>();
    }

    private static final List<DeathRecord> RECORDS = new ArrayList<>();
    private static final List<DeathRecord> PINNED = new ArrayList<>();
    private static final List<Active> ACTIVE = new ArrayList<>();
    private static final List<Lost> LOST = new ArrayList<>();

    private static long credits;
    private static boolean creditsAvailable;
    private static int teleportIndex;
    private static int maxHistory = 15;
    private static int maxActive = 10;

    private static DeathRecord pendingDetails;
    private static boolean openRequested;
    private static boolean expectOpen;
    private static boolean openServerHistory;
    private static boolean hideAll = true;

    private static byte lastAction;
    private static boolean lastSuccess;
    private static String lastMessage = "";
    private static long lastResultTime;

    private ClientDeathHistory() {
    }

    public static void accept(PacketSyncHistory packet) {
        RECORDS.clear();
        PINNED.clear();
        ACTIVE.clear();

        for (int i = 0; i < packet.records.size(); i++) {
            RECORDS.add(DeathRecord.fromTag(packet.records.getCompound(i)));
        }
        for (int i = 0; i < packet.pinned.size(); i++) {
            PINNED.add(DeathRecord.fromTag(packet.pinned.getCompound(i)));
        }
        for (int i = 0; i < packet.active.size(); i++) {
            CompoundTag tag = packet.active.getCompound(i);
            Active active = new Active();
            active.storageId = tag.getUUID("Id");
            active.recordId = tag.getUUID("Record");
            active.name = tag.getString("Name");
            active.icon = tag.getString("Icon");
            active.color = tag.contains("Color") ? tag.getInt("Color") : 0x8FE3FF;
            active.nameIndex = tag.getInt("Index");
            active.remaining = tag.getLong("Remaining");
            active.receivedAt = System.currentTimeMillis();
            active.dimension = tag.getString("Dim");
            active.pos = BlockPos.of(tag.getLong("Pos"));
            ACTIVE.add(active);
        }

                for (DeathRecord record : RECORDS) {
            Active active = activeFor(record.recordId);
            if (active != null) {
                active.hidden = record.hidden;
                active.initial = record.initial;
            }
        }

        credits = packet.credits;
        creditsAvailable = packet.creditsAvailable;
        teleportIndex = packet.teleportIndex;
        maxHistory = packet.maxHistory;
        maxActive = packet.maxActive;

        openRequested = expectOpen;
        expectOpen = false;
    }

    public static void expectOpen(boolean value) {
    expectOpen = value;
}

    public static void details(CompoundTag tag) {
        pendingDetails = DeathRecord.fromTag(tag);
    }

    public static void result(PacketResult packet) {
        lastAction = packet.action;
        lastSuccess = packet.success;
        lastMessage = packet.message;
        lastResultTime = System.currentTimeMillis();
    }

    public static void setCredits(long value) {
        credits = value;
    }

    public static void serverHistory(ListTag list) {
        LOST.clear();

        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Lost lost = new Lost();
            lost.ownerName = tag.getString("OwnerName");
            lost.dimension = tag.getString("Dim");
            lost.pos = BlockPos.of(tag.getLong("Pos"));
            lost.time = tag.getLong("Time");
            ListTag items = tag.getList("Items", 10);
            for (int j = 0; j < items.size(); j++) {
                lost.items.add(net.minecraft.world.item.ItemStack.of(items.getCompound(j)));
            }
            LOST.add(lost);
        }

        openServerHistory = true;
    }

    public static List<DeathRecord> records() {
        return RECORDS;
    }

    public static List<DeathRecord> pinned() {
        return PINNED;
    }

    public static List<Active> active() {
        return ACTIVE;
    }

    public static List<Lost> lost() {
        return LOST;
    }

    /** Creditos legibles: el saldo se guarda en milesimas. */
    public static long credits() {
        return credits / 1000L;
    }

    /** Saldo con decimales solo si los hay: "10" o "10,5". */
public static String creditsText() {
    long whole = credits / 1000L;
    int fraction = (int) (credits % 1000L);

    if (fraction == 0) {
        return Long.toString(whole);
    }

    String tail = String.format("%03d", fraction);
    while (tail.endsWith("0")) {
        tail = tail.substring(0, tail.length() - 1);
    }

    return whole + "," + tail;
}

    public static long creditsRaw() {
        return credits;
    }

    public static boolean creditsAvailable() {
        return creditsAvailable;
    }

    public static int teleportIndex() {
        return teleportIndex;
    }

    public static int maxHistory() {
        return maxHistory;
    }

    public static int maxActive() {
        return maxActive;
    }

    public static byte lastAction() {
        return lastAction;
    }

    public static boolean lastSuccess() {
        return lastSuccess;
    }

    public static String lastMessage() {
        return lastMessage;
    }

    public static long lastResultTime() {
        return lastResultTime;
    }

    public static DeathRecord takeDetails() {
        DeathRecord record = pendingDetails;
        pendingDetails = null;
        return record;
    }

    public static boolean consumeOpen() {
        boolean value = openRequested;
        openRequested = false;
        return value;
    }

    public static boolean consumeServerHistory() {
        boolean value = openServerHistory;
        openServerHistory = false;
        return value;
    }

    public static void clear() {
        RECORDS.clear();
        PINNED.clear();
        ACTIVE.clear();
        LOST.clear();
    }

    public static Active activeFor(UUID recordId) {
        for (Active active : ACTIVE) {
            if (active.recordId.equals(recordId)) {
                return active;
            }
        }
        return null;
    }

    public static boolean hideAllWaypoints() {
    return hideAll;
}

public static void toggleHideAll() {
    hideAll = !hideAll;
}

public static void toggleHidden(UUID recordId) {
    for (DeathRecord record : RECORDS) {
        if (record.recordId.equals(recordId)) {
            record.hidden = !record.hidden;
        }
    }
    Active active = activeFor(recordId);
    if (active != null) {
        active.hidden = !active.hidden;
    }
}

    public static long remainingFor(UUID recordId) {
        Active active = activeFor(recordId);
        if (active == null) {
            return 0L;
        }
        return Math.max(0L, active.remaining - (System.currentTimeMillis() - active.receivedAt));
    }

    public static int freeTeleports() {
        return Math.max(0, 2 - teleportIndex);
    }

    public static long teleportPrice() {
        return teleportPrice(teleportIndex);
    }

    public static long teleportPrice(int index) {
        if (index <= 1) {
            return 0L;
        }
        if (index == 9) {
            return 10L;
        }
        if (index < 9) {
            return index - 1L;
        }
        return 10L + 5L * (index - 9);
    }

    public static String clock(long epoch) {
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.ofEpochMilli(epoch));
    }

    public static String duration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        if (seconds >= 3600L) {
            return (seconds / 3600L) + "h " + ((seconds % 3600L) / 60L) + "m";
        }
        return String.format("%d:%02d", seconds / 60L, seconds % 60L);
    }

        @Mod.EventBusSubscriber(
            modid = ExampleMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.FORGE,
            value = Dist.CLIENT
    )
    public static final class RespawnSync {

        private RespawnSync() {
        }

        /** Al reaparecer se pide el historial: el waypoint sale sin abrir nada. */
        @SubscribeEvent
        public static void onClone(ClientPlayerNetworkEvent.Clone event) {
            DeathLootNetwork.requestHistory(false, false);
        }
    }
}