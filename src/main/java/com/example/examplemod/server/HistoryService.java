package com.example.examplemod.server;

import com.example.examplemod.Config;
import com.example.examplemod.client.WaypointIcon;
import com.example.examplemod.currency.Credits;
import com.example.examplemod.network.DeathLootNetwork;
import com.example.examplemod.network.packet.PacketAction;
import com.example.examplemod.network.packet.PacketCredits;
import com.example.examplemod.network.packet.PacketDetails;
import com.example.examplemod.network.packet.PacketResult;
import com.example.examplemod.network.packet.PacketServerHistory;
import com.example.examplemod.network.packet.PacketSyncHistory;
import com.example.examplemod.storage.DeathHistory;
import com.example.examplemod.storage.DeathRecord;
import com.example.examplemod.storage.DeathStorageBlockEntity;
import com.example.examplemod.storage.DeathStorageManager;
import com.example.examplemod.storage.SafePlacement;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public final class HistoryService {

    private static final String TP_KEY = "deathloot_teleports";

    private HistoryService() {
    }

    public static void sendHistory(ServerPlayer player, boolean admin) {
        if (admin) {
            if (!player.hasPermissions(2)) {
                return;
            }
            DeathLootNetwork.toPlayer(player, new PacketServerHistory(lostTags(player)));
            return;
        }

        DeathHistory history = DeathHistory.get(player.server);
        List<DeathRecord> records = history.of(player.getUUID());
        ListTag plain = new ListTag();
        ListTag pinned = new ListTag();

        for (DeathRecord record : records) {
            DeathStorageBlockEntity live = storageOf(player, record);
            record.freeExtensions = live == null ? 0 : live.getFreeExtensions();

            if (record.pinned) {
                pinned.add(record.toTag(true));
            } else {
                plain.add(record.toTag(true));
            }
        }

        ListTag active = new ListTag();
        DeathStorageManager manager = DeathStorageManager.get(player.server);

        for (DeathStorageManager.Entry entry : manager.activeOf(player.getUUID())) {
            ServerLevel level = player.server.getLevel(entry.dimension);
            if (level == null || !(level.getBlockEntity(entry.pos) instanceof DeathStorageBlockEntity storage)) {
                continue;
            }

            DeathRecord record = history.byStorage(player.getUUID(), entry.storageId);
            if (record == null) {
                continue;
            }

            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", entry.storageId);
            tag.putUUID("Record", record.recordId);
            tag.putString("Name", storage.getStorageName());
            tag.putString("Icon", storage.getWaypointIcon());
            tag.putInt("Color", storage.getSkullColor());
            tag.putInt("Index", storage.getNameIndex());
            tag.putLong("Remaining", storage.getRemainingMillis());
            tag.putString("Dim", entry.dimension.location().toString());
            tag.putLong("Pos", entry.pos.asLong());
            active.add(tag);
        }

        DeathLootNetwork.toPlayer(player, new PacketSyncHistory(
                plain, pinned, active,
                Credits.get(player), Credits.available(),
                player.getPersistentData().getInt(TP_KEY),
                Config.maxHistoryEntries, Config.maxActiveStorages));
    }

    private static ListTag lostTags(ServerPlayer player) {
        ListTag list = new ListTag();

        for (DeathHistory.Lost lost : DeathHistory.get(player.server).lost()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("OwnerName", lost.ownerName);
            tag.putString("Dim", lost.dimension);
            tag.putLong("Pos", lost.pos.asLong());
            tag.putLong("Time", lost.time);
            ListTag items = new ListTag();
            for (ItemStack stack : lost.items) {
                items.add(stack.save(new CompoundTag()));
            }
            tag.put("Items", items);
            list.add(tag);
        }

        return list;
    }

    public static void sendDetails(ServerPlayer player, UUID recordId) {
        DeathRecord record = DeathHistory.get(player.server).byId(player.getUUID(), recordId);
        if (record == null) {
            return;
        }
        DeathLootNetwork.toPlayer(player, new PacketDetails(record.toTag()));
    }

    public static void handle(ServerPlayer player, byte action, UUID recordId, String text, int value) {
        DeathHistory history = DeathHistory.get(player.server);
        DeathRecord record = history.byId(player.getUUID(), recordId);

        if (record == null) {
            result(player, action, false, "deathlootforge.result.missing");
            return;
        }

        switch (action) {
            case PacketAction.EXTEND -> extend(player, record, value, action);
            case PacketAction.RENAME -> rename(player, record, text, action);
            case PacketAction.TELEPORT -> teleport(player, record, action);
            case PacketAction.PIN -> pin(player, record, action);
            case PacketAction.DELETE -> delete(player, record, action);
            case PacketAction.CHECKPOINT -> checkpoint(player, record, action);
            case PacketAction.COLOR -> color(player, record, value, action);
            case PacketAction.ICON -> icon(player, record, text, action);
            case PacketAction.HIDE -> {
    record.hidden = value != 0;
    history.setDirty();
    result(player, action, true, record.hidden
            ? "deathlootforge.result.hidden" : "deathlootforge.result.shown");
}
case PacketAction.INITIAL -> {
    String initial = text.trim();
    if (initial.length() > 4) {
        initial = initial.substring(0, 4);
    }
    record.initial = initial;
    history.setDirty();
    result(player, action, true, "deathlootforge.result.initialset");
}
            default -> {
            }
        }
    }

    private static DeathStorageBlockEntity storageOf(ServerPlayer player, DeathRecord record) {
        if (record.storageId == null) {
            return null;
        }
        DeathStorageManager.Entry entry = DeathStorageManager.get(player.server).get(record.storageId);
        if (entry == null) {
            return null;
        }
        ServerLevel level = player.server.getLevel(entry.dimension);
        if (level == null) {
            return null;
        }
        return level.getBlockEntity(entry.pos) instanceof DeathStorageBlockEntity storage ? storage : null;
    }

    private static void extend(ServerPlayer player, DeathRecord record, int minutes, byte action) {
        if (minutes != 5 && minutes != 10 && minutes != 30 && minutes != 60 && minutes != 300 && minutes != 600) {
            result(player, action, false, "deathlootforge.result.badtime");
            return;
        }

        DeathStorageBlockEntity storage = storageOf(player, record);
        if (storage == null) {
            result(player, action, false, "deathlootforge.result.gone");
            return;
        }

        boolean freePick = (minutes == 5 || minutes == 10) && storage.getFreeExtensions() < 2;
        long cost = freePick ? 0L : price(minutes);

                if (cost > 0L) {
            if (Credits.get(player) < cost * 1000L) {
                result(player, action, false, "deathlootforge.result.nocredits");
                DeathLootNetwork.toPlayer(player, new PacketCredits(Credits.get(player)));
                return;
            }
            ShopBridge.charge(player, "DeathLoot: ampliar tiempo (" + minutes + "m)", cost * 1000L);
        }

        if (freePick) {
            storage.setFreeExtensions(storage.getFreeExtensions() + 1);
        } else {
            storage.setExtensions(storage.getExtensions() + 1);
        }

        storage.setExpiresAt(storage.getExpiresAt() + minutes * 60_000L);

        DeathStorageManager.Entry entry = DeathStorageManager.get(player.server).get(record.storageId);
        if (entry != null) {
            entry.expiresAt = storage.getExpiresAt();
        }

        record.freeExtensions = storage.getFreeExtensions();

        result(player, action, true, "deathlootforge.result.extended");
        DeathLootNetwork.toPlayer(player, new PacketCredits(Credits.get(player)));
        sendHistory(player, false);
    }

    private static long price(int minutes) {
        return switch (minutes) {
            case 5 -> 1L;
            case 10 -> 2L;
            case 30 -> 4L;
            case 60 -> 6L;
            case 300 -> 25L;
            case 600 -> 40L;
            default -> 1L;
        };
    }

    private static void rename(ServerPlayer player, DeathRecord record, String name, byte action) {
        String clean = name.trim();
        if (clean.isEmpty() || clean.length() > 24) {
            result(player, action, false, "deathlootforge.result.badname");
            return;
        }

        DeathStorageBlockEntity storage = storageOf(player, record);
        if (storage == null) {
            result(player, action, false, "deathlootforge.result.gone");
            return;
        }

        storage.setStorageName(clean);
        record.label = clean;
        DeathHistory.get(player.server).setDirty();
        result(player, action, true, "deathlootforge.result.renamed");
        sendHistory(player, false);
    }

    private static void icon(ServerPlayer player, DeathRecord record, String icon, byte action) {
        String clean = WaypointIcon.valid(icon) ? icon : "";

        record.icon = clean;
        DeathHistory.get(player.server).setDirty();

        DeathStorageBlockEntity storage = storageOf(player, record);
        if (storage != null) {
            storage.setWaypointIcon(clean);
        }

        result(player, action, true, "deathlootforge.result.iconset");
        sendHistory(player, false);
    }

    private static void pin(ServerPlayer player, DeathRecord record, byte action) {
        record.pinned = !record.pinned;
        DeathHistory.get(player.server).setDirty();
        result(player, action, true, record.pinned ? "deathlootforge.result.pinned" : "deathlootforge.result.unpinned");
        sendHistory(player, false);
    }

    private static void checkpoint(ServerPlayer player, DeathRecord record, byte action) {
        record.checkpoint = !record.checkpoint;
        DeathHistory.get(player.server).setDirty();

        DeathStorageBlockEntity storage = storageOf(player, record);
        if (storage != null) {
            storage.setCheckpoint(record.checkpoint);
        }

        result(player, action, true, record.checkpoint
                ? "deathlootforge.result.checkon" : "deathlootforge.result.checkoff");
        sendHistory(player, false);
    }

    private static void color(ServerPlayer player, DeathRecord record, int rgb, byte action) {
        record.skullColor = rgb & 0xFFFFFF;
        DeathHistory.get(player.server).setDirty();

        DeathStorageBlockEntity storage = storageOf(player, record);
        if (storage != null) {
            storage.setSkullColor(record.skullColor);
        }
    }

    private static void delete(ServerPlayer player, DeathRecord record, byte action) {
        DeathStorageBlockEntity storage = storageOf(player, record);

        if (storage != null && record.storageId != null) {
            DeathStorageManager manager = DeathStorageManager.get(player.server);
            DeathStorageManager.Entry entry = manager.get(record.storageId);
            ServerLevel level = entry == null ? null : player.server.getLevel(entry.dimension);

            if (entry != null && level != null) {
                manager.expire(level, storage, entry, true);
                manager.remove(entry.storageId);
            }
        }

        DeathHistory.get(player.server).remove(player.getUUID(), record.recordId);
        result(player, action, true, "deathlootforge.result.deleted");
        sendHistory(player, false);
    }

    private static void teleport(ServerPlayer player, DeathRecord record, byte action) {
        if (record.storageId == null) {
            result(player, action, false, "deathlootforge.result.gone");
            return;
        }

        DeathStorageManager.Entry entry = DeathStorageManager.get(player.server).get(record.storageId);
        if (entry == null) {
            result(player, action, false, "deathlootforge.result.gone");
            return;
        }

        ServerLevel level = player.server.getLevel(entry.dimension);
        if (level == null) {
            result(player, action, false, "deathlootforge.result.gone");
            return;
        }

        int index = player.getPersistentData().getInt(TP_KEY);
        long cost = teleportPrice(index);

                if (cost > 0L) {
            if (Credits.get(player) < cost * 1000L) {
                result(player, action, false, "deathlootforge.result.nocredits");
                DeathLootNetwork.toPlayer(player, new PacketCredits(Credits.get(player)));
                return;
            }
            ShopBridge.charge(player, "DeathLoot: teletransporte a Muerte " + record.storageNameIndex, cost * 1000L);
        }

        player.getPersistentData().putInt(TP_KEY, index + 1);

        BlockPos landing = SafePlacement.prepareLanding(level, entry.pos);

player.teleportTo(level,
        landing.getX() + 0.5D,
        landing.getY(),
        landing.getZ() + 0.5D,
        player.getYRot(),
        player.getXRot());
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
        player.connection.resetPosition();

        level.playSound(null, landing, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
        result(player, action, true, cost <= 0L ? "deathlootforge.result.tp.free" : "deathlootforge.result.tp");
        DeathLootNetwork.toPlayer(player, new PacketCredits(Credits.get(player)));
        sendHistory(player, false);
    }

    private static long teleportPrice(int index) {
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

    private static void result(ServerPlayer player, byte action, boolean success, String key) {
        String message = Component.translatable(key).getString();

        player.playNotifySound(success ? SoundEvents.NOTE_BLOCK_PLING.value() : SoundEvents.VILLAGER_NO,
                SoundSource.MASTER, 0.9F, success ? 1.6F : 0.9F);

        DeathLootNetwork.toPlayer(player, new PacketResult(action, success, message));
    }
}