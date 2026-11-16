package com.example.examplemod.storage;

import com.example.examplemod.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Historial de muertes por jugador (15 entradas, las fijadas no cuentan) y registro de perdidas para admin. */
public class DeathHistory extends SavedData {

    private static final String NAME = "deathloot_history";

    private final Map<UUID, List<DeathRecord>> players = new HashMap<>();
    private final List<Lost> lost = new ArrayList<>();

    public static class Lost {
        public UUID owner;
        public String ownerName = "";
        public String dimension = "minecraft:overworld";
        public BlockPos pos = BlockPos.ZERO;
        public long time = System.currentTimeMillis();
        public List<ItemStack> items = new ArrayList<>();
    }

    public static DeathHistory get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(DeathHistory::load, DeathHistory::new, NAME);
    }

    public static DeathHistory load(CompoundTag tag) {
        DeathHistory history = new DeathHistory();
        CompoundTag players = tag.getCompound("Players");

        for (String key : players.getAllKeys()) {
            try {
                UUID owner = UUID.fromString(key);
                ListTag list = players.getList(key, 10);
                List<DeathRecord> records = new ArrayList<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    records.add(DeathRecord.fromTag(list.getCompound(i)));
                }
                history.players.put(owner, records);
            } catch (IllegalArgumentException ignored) {
            }
        }

        ListTag lost = tag.getList("Lost", 10);
        for (int i = 0; i < lost.size(); i++) {
            CompoundTag entry = lost.getCompound(i);
            Lost record = new Lost();
            record.owner = entry.hasUUID("Owner") ? entry.getUUID("Owner") : null;
            record.ownerName = entry.getString("OwnerName");
            record.dimension = entry.getString("Dim");
            record.pos = BlockPos.of(entry.getLong("Pos"));
            record.time = entry.getLong("Time");
            ListTag items = entry.getList("Items", 10);
            for (int j = 0; j < items.size(); j++) {
                record.items.add(ItemStack.of(items.getCompound(j)));
            }
            history.lost.add(record);
        }

        return history;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();

        this.players.forEach((owner, records) -> {
            ListTag list = new ListTag();
            for (DeathRecord record : records) {
                list.add(record.toTag());
            }
            players.put(owner.toString(), list);
        });

        tag.put("Players", players);

        ListTag lost = new ListTag();
        for (Lost record : this.lost) {
            CompoundTag entry = new CompoundTag();
            if (record.owner != null) {
                entry.putUUID("Owner", record.owner);
            }
            entry.putString("OwnerName", record.ownerName);
            entry.putString("Dim", record.dimension);
            entry.putLong("Pos", record.pos.asLong());
            entry.putLong("Time", record.time);
            ListTag items = new ListTag();
            for (ItemStack stack : record.items) {
                items.add(stack.save(new CompoundTag()));
            }
            entry.put("Items", items);
            lost.add(entry);
        }

        tag.put("Lost", lost);
        return tag;
    }

    public List<DeathRecord> of(UUID owner) {
        return players.computeIfAbsent(owner, key -> new ArrayList<>());
    }

    public void add(DeathRecord record) {
        if (record.ownerId == null) {
            return;
        }

        List<DeathRecord> records = of(record.ownerId);
        records.add(0, record);

        int unpinned = 0;
        for (int i = records.size() - 1; i >= 0; i--) {
            DeathRecord entry = records.get(i);
            if (!entry.pinned) {
                unpinned++;
                if (unpinned > Config.maxHistoryEntries) {
                    records.remove(i);
                }
            }
        }

        setDirty();
    }

    @Nullable
    public DeathRecord byStorage(UUID owner, UUID storageId) {
        if (owner == null || storageId == null) {
            return null;
        }
        for (DeathRecord record : of(owner)) {
            if (storageId.equals(record.storageId)) {
                return record;
            }
        }
        return null;
    }

    @Nullable
    public DeathRecord byId(UUID owner, UUID recordId) {
        for (DeathRecord record : of(owner)) {
            if (record.recordId.equals(recordId)) {
                return record;
            }
        }
        return null;
    }

    public void remove(UUID owner, UUID recordId) {
        of(owner).removeIf(record -> record.recordId.equals(recordId));
        setDirty();
    }

    public void markRecovered(UUID owner, UUID storageId) {
        DeathRecord record = byStorage(owner, storageId);
        if (record != null) {
            record.status = DeathRecord.RECUPERADO;
            record.checkpoint = false;
            setDirty();
        }
    }

    public void addLost(UUID owner, String ownerName, String dimension, BlockPos pos, List<ItemStack> items) {
        Lost record = new Lost();
        record.owner = owner;
        record.ownerName = ownerName;
        record.dimension = dimension;
        record.pos = pos;
        record.items = new ArrayList<>(items);
        lost.add(0, record);

        while (lost.size() > 200) {
            lost.remove(lost.size() - 1);
        }

        setDirty();
    }

    public List<Lost> lost() {
        return lost;
    }
}