package com.example.examplemod.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Instantanea de una muerte. Se guarda antes de mover nada. */
public class DeathRecord {

    public static final int AUSENTE = 0;
    public static final int RECUPERADO = 1;

    public UUID recordId = UUID.randomUUID();
    public UUID ownerId;
    public String ownerName = "";
    public UUID storageId;
    public int storageNameIndex;
    public long timestamp = System.currentTimeMillis();
    public String dimension = "minecraft:overworld";
    public BlockPos pos = BlockPos.ZERO;
    public String cause = "";
    public int status = AUSENTE;
    public boolean pinned;
    public boolean checkpoint = true;
    public int skullColor = DeathStorageBlockEntity.DEFAULT_COLOR;
    public int selectedSlot;

    public String label = "";
    public String icon = "";
    public int freeExtensions;
    public String initial = "";
    public boolean hidden;

    public List<ItemStack> inventory = new ArrayList<>();
    public List<ItemStack> armor = new ArrayList<>();
    public ItemStack offhand = ItemStack.EMPTY;
    public List<ItemStack> curios = new ArrayList<>();
    public List<Backpack> backpacks = new ArrayList<>();

    public static class Backpack {
        public int slot;
        public boolean curiosSlot;
        public ItemStack icon = ItemStack.EMPTY;
        public List<ItemStack> contents = new ArrayList<>();
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("RecordId", recordId);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putString("OwnerName", ownerName);
        if (storageId != null) {
            tag.putUUID("StorageId", storageId);
        }
        tag.putInt("NameIndex", storageNameIndex);
        tag.putLong("Time", timestamp);
        tag.putString("Dim", dimension);
        tag.putLong("Pos", pos.asLong());
        tag.putString("Cause", cause);
        tag.putInt("Status", status);
        tag.putBoolean("Pinned", pinned);
        tag.putBoolean("Checkpoint", checkpoint);
        tag.putInt("SkullColor", skullColor);
        tag.putInt("Selected", selectedSlot);
        tag.putString("Label", label);
        tag.putString("Icon", icon);
        tag.putInt("Free", freeExtensions);
        tag.putString("Initial", initial);
        tag.putBoolean("Hidden", hidden);

        tag.put("Inv", writeList(inventory));
        tag.put("Armor", writeList(armor));
        tag.put("Curios", writeList(curios));

        CompoundTag off = new CompoundTag();
        if (!offhand.isEmpty()) {
            offhand.save(off);
        }
        tag.put("Offhand", off);

        ListTag bags = new ListTag();
        for (Backpack backpack : backpacks) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", backpack.slot);
            entry.putBoolean("Curio", backpack.curiosSlot);
            CompoundTag icon = new CompoundTag();
            backpack.icon.save(icon);
            entry.put("Icon", icon);
            entry.put("Contents", writeList(backpack.contents));
            bags.add(entry);
        }
        tag.put("Backpacks", bags);

        return tag;
    }

    /** Version sin inventario para el listado; conserva armadura y mano para el muneco. */
    public CompoundTag toTag(boolean light) {
        CompoundTag tag = toTag();

        if (light) {
            tag.remove("Inv");
            tag.remove("Curios");
            tag.remove("Offhand");
            tag.remove("Backpacks");

            CompoundTag main = new CompoundTag();
            ItemStack hand = mainHand(selectedSlot);
            if (!hand.isEmpty()) {
                hand.save(main);
            }
            tag.put("Mainhand", main);
        }

        return tag;
    }

    public static DeathRecord fromTag(CompoundTag tag) {
        DeathRecord record = new DeathRecord();
        if (tag.hasUUID("RecordId")) {
            record.recordId = tag.getUUID("RecordId");
        }
        record.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        record.ownerName = tag.getString("OwnerName");
        record.storageId = tag.hasUUID("StorageId") ? tag.getUUID("StorageId") : null;
        record.storageNameIndex = tag.getInt("NameIndex");
        record.timestamp = tag.getLong("Time");
        record.dimension = tag.getString("Dim");
        record.pos = BlockPos.of(tag.getLong("Pos"));
        record.cause = tag.getString("Cause");
        record.status = tag.getInt("Status");
        record.pinned = tag.getBoolean("Pinned");
        record.checkpoint = !tag.contains("Checkpoint") || tag.getBoolean("Checkpoint");
        record.skullColor = tag.contains("SkullColor") ? tag.getInt("SkullColor") : DeathStorageBlockEntity.DEFAULT_COLOR;
        record.selectedSlot = tag.getInt("Selected");
        record.label = tag.getString("Label");
        record.icon = tag.getString("Icon");
        record.freeExtensions = tag.getInt("Free");
        record.initial = tag.getString("Initial");
        record.hidden = tag.getBoolean("Hidden");

        record.inventory = readList(tag.getList("Inv", 10));
        record.armor = readList(tag.getList("Armor", 10));
        record.curios = readList(tag.getList("Curios", 10));

        if (tag.contains("Offhand")) {
            CompoundTag off = tag.getCompound("Offhand");
            record.offhand = off.isEmpty() ? ItemStack.EMPTY : ItemStack.of(off);
        }

        ListTag bags = tag.getList("Backpacks", 10);
        for (int i = 0; i < bags.size(); i++) {
            CompoundTag entry = bags.getCompound(i);
            Backpack backpack = new Backpack();
            backpack.slot = entry.getInt("Slot");
            backpack.curiosSlot = entry.getBoolean("Curio");
            backpack.icon = ItemStack.of(entry.getCompound("Icon"));
            backpack.contents = readList(entry.getList("Contents", 10));
            record.backpacks.add(backpack);
        }

        if (tag.contains("Mainhand")) {
            CompoundTag main = tag.getCompound("Mainhand");
            if (!main.isEmpty()) {
                while (record.inventory.size() <= record.selectedSlot) {
                    record.inventory.add(ItemStack.EMPTY);
                }
                record.inventory.set(record.selectedSlot, ItemStack.of(main));
            }
        }

        return record;
    }

    private static ListTag writeList(List<ItemStack> stacks) {
        ListTag list = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) i);
            stack.save(entry);
            list.add(entry);
        }
        return list;
    }

    private static List<ItemStack> readList(ListTag list) {
        List<ItemStack> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 255;
            while (out.size() <= slot) {
                out.add(ItemStack.EMPTY);
            }
            out.set(slot, ItemStack.of(entry));
        }
        return out;
    }

    public ItemStack armorPiece(int index) {
        return index < armor.size() ? armor.get(index) : ItemStack.EMPTY;
    }

    public ItemStack mainHand(int selectedSlot) {
        return selectedSlot < inventory.size() ? inventory.get(selectedSlot) : ItemStack.EMPTY;
    }

    public String displayName() {
        return label.isEmpty()
                ? Component.translatable("deathlootforge.storage.default", storageNameIndex).getString()
                : label;
    }

    /** Texto del pin: la inicial si existe, si no el numero de la muerte. */
public String badgeText() {
    String value = initial == null ? "" : initial.trim();
    if (!value.isEmpty()) {
        return value.length() > 4 ? value.substring(0, 4) : value;
    }
    return Integer.toString(storageNameIndex);
}
}