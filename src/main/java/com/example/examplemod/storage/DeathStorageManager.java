package com.example.examplemod.storage;

import com.example.examplemod.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Indice de almacenes activos. La fuente de verdad del inventario es la entidad
 * de bloque; aqui solo viven los datos de control (dueno, dimension, posicion,
 * caducidad) para poder expirar y tepear sin cargar chunks.
 */
public class DeathStorageManager extends SavedData {

    private static final String NAME = "deathloot_storages";

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    private final Map<UUID, Integer> nameCounters = new HashMap<>();

    public static final class Entry {
        public final UUID storageId;
        public final UUID owner;
        public final ResourceKey<Level> dimension;
        public final BlockPos pos;
        public long expiresAt;

        Entry(UUID storageId, UUID owner, ResourceKey<Level> dimension, BlockPos pos, long expiresAt) {
            this.storageId = storageId;
            this.owner = owner;
            this.dimension = dimension;
            this.pos = pos;
            this.expiresAt = expiresAt;
        }
    }

    public static DeathStorageManager get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(DeathStorageManager::load, DeathStorageManager::new, NAME);
    }

    public static DeathStorageManager load(CompoundTag tag) {
        DeathStorageManager manager = new DeathStorageManager();
        ListTag list = tag.getList("Entries", 10);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Id") || !entry.hasUUID("Owner")) {
                continue;
            }
            ResourceLocation dim = ResourceLocation.tryParse(entry.getString("Dim"));
            if (dim == null) {
                continue;
            }
            manager.entries.put(entry.getUUID("Id"), new Entry(
                    entry.getUUID("Id"),
                    entry.getUUID("Owner"),
                    ResourceKey.create(Registries.DIMENSION, dim),
                    BlockPos.of(entry.getLong("Pos")),
                    entry.getLong("ExpiresAt")));
        }

        CompoundTag counters = tag.getCompound("Counters");
        for (String key : counters.getAllKeys()) {
            try {
                manager.nameCounters.put(UUID.fromString(key), counters.getInt(key));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (Entry entry : entries.values()) {
            CompoundTag out = new CompoundTag();
            out.putUUID("Id", entry.storageId);
            out.putUUID("Owner", entry.owner);
            out.putString("Dim", entry.dimension.location().toString());
            out.putLong("Pos", entry.pos.asLong());
            out.putLong("ExpiresAt", entry.expiresAt);
            list.add(out);
        }

        tag.put("Entries", list);

        CompoundTag counters = new CompoundTag();
        nameCounters.forEach((key, value) -> counters.putInt(key.toString(), value));
        tag.put("Counters", counters);

        return tag;
    }

    public int nextNameIndex(UUID owner) {
        int value = nameCounters.getOrDefault(owner, 0) + 1;
        nameCounters.put(owner, value);
        setDirty();
        return value;
    }

    public void register(UUID storageId, UUID owner, ResourceKey<Level> dimension, BlockPos pos, long expiresAt) {
        entries.put(storageId, new Entry(storageId, owner, dimension, pos, expiresAt));
        setDirty();
    }

    @Nullable
    public Entry get(UUID storageId) {
        return entries.get(storageId);
    }

    public void remove(UUID storageId) {
    entries.remove(storageId);
    setDirty();
}

    public List<Entry> activeOf(UUID owner) {
        List<Entry> out = new ArrayList<>();
        for (Entry entry : entries.values()) {
            if (entry.owner.equals(owner)) {
                out.add(entry);
            }
        }
        out.sort(Comparator.comparingLong(entry -> entry.expiresAt));
        return out;
    }

    public void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        boolean second = server.getTickCount() % 20 == 0;
        Iterator<Entry> iterator = entries.values().iterator();

        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            ServerLevel level = server.getLevel(entry.dimension);

            if (level == null || !level.isLoaded(entry.pos)) {
                continue;
            }

            if (!(level.getBlockEntity(entry.pos) instanceof DeathStorageBlockEntity storage)) {
                iterator.remove();
                setDirty();
                continue;
            }

            storage.setExpiresAt(entry.expiresAt);

            if (now >= entry.expiresAt) {
                expire(level, storage, entry, true);
                iterator.remove();
                setDirty();
                continue;
            }

            if (second) {
                storage.sync();
            }
        }
    }

    /** Expira el almacen: manda su contenido al historial de admin y lo elimina. */
    public void expire(ServerLevel level, DeathStorageBlockEntity storage, Entry entry, boolean toHistory) {
        List<ItemStack> items = new ArrayList<>();

        for (int slot = 0; slot < DeathStorageBlockEntity.SIZE; slot++) {
            ItemStack stack = storage.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        if (toHistory && !items.isEmpty()) {
            DeathHistory.get(level.getServer()).addLost(
                    entry.owner,
                    storage.getOwnerName(),
                    entry.dimension.location().toString(),
                    entry.pos,
                    items);
        }

        level.playSound(null, entry.pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F);
        level.removeBlock(entry.pos, false);
        setDirty();
    }

    public void expireNow(ServerLevel level, BlockPos pos, boolean toHistory) {
        if (!(level.getBlockEntity(pos) instanceof DeathStorageBlockEntity storage)) {
            return;
        }

        Entry entry = entries.get(storage.getStorageId());
        if (entry != null) {
            expire(level, storage, entry, toHistory);
            entries.remove(entry.storageId);
        } else {
            expire(level, storage, new Entry(storage.getStorageId(),
                    storage.getOwnerId() == null ? new UUID(0, 0) : storage.getOwnerId(),
                    level.dimension(), pos, 0L), toHistory);
        }

        setDirty();
    }

    /** Respeta el tope: si el jugador ya tiene demasiados, expira el mas antiguo. */
    public void enforceLimit(MinecraftServer server, UUID owner) {
        List<Entry> active = activeOf(owner);

        while (active.size() >= Config.maxActiveStorages) {
            Entry oldest = active.remove(0);
            ServerLevel level = server.getLevel(oldest.dimension);

            if (level != null && level.getBlockEntity(oldest.pos) instanceof DeathStorageBlockEntity storage) {
                expire(level, storage, oldest, true);
            }

            entries.remove(oldest.storageId);
            setDirty();
        }
    }
}