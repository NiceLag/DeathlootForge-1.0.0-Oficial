package com.example.examplemod.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.ListTag;

public class DeathStorageBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int SIZE = 54;
    public static final int DEFAULT_COLOR = 0x8FE3FF;

    private static final int[] SLOTS = new int[SIZE];

    static {
        for (int i = 0; i < SIZE; i++) {
            SLOTS[i] = i;
        }
    }

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> new InvWrapper(this));

    private UUID storageId = UUID.randomUUID();
    private UUID ownerId;
    private String ownerName = "";
    private String storageName = "";
    private String waypointIcon = "";
    private long expiresAt;
    private int skullColor = DEFAULT_COLOR;
    private boolean checkpoint = true;
    private boolean opened;
    private int freeExtensions;
    private int extensions;
    private int nameIndex;
    public static final byte ORIGIN_INV = 0;
public static final byte ORIGIN_ARMOR = 1;
public static final byte ORIGIN_OFFHAND = 2;
public static final byte ORIGIN_CURIO = 3;

public static final class Origin {
    public final byte type;
    public final int slot;

    public Origin(byte type, int slot) {
        this.type = type;
        this.slot = slot;
    }
}

private final Map<Integer, Origin> origins = new HashMap<>();

public void setOrigin(int storageSlot, byte type, int slot) {
    origins.put(storageSlot, new Origin(type, slot));
}

public Origin getOrigin(int storageSlot) {
    return origins.get(storageSlot);
}

public void clearOrigin(int storageSlot) {
    origins.remove(storageSlot);
}

public void clearOrigins() {
    origins.clear();
}

    public DeathStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEATH_STORAGE.get(), pos, state);
    }

    // ------------------------------------------------------------------ datos

    public UUID getStorageId() {
        return storageId;
    }

    public void setStorageId(UUID id) {
        this.storageId = id;
        setChanged();
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getStorageName() {
        return storageName.isEmpty() ? Component.translatable("deathlootforge.storage.default", nameIndex).getString() : storageName;
    }
 
    public String getWaypointIcon() {
        return waypointIcon;
    }

    public void setWaypointIcon(String icon) {
    this.waypointIcon = icon == null ? "" : icon;
    sync();
}

    public void setStorageName(String name) {
        this.storageName = name == null ? "" : name.trim();
        sync();
    }

    public int getNameIndex() {
        return nameIndex;
    }

    public void setNameIndex(int index) {
        this.nameIndex = index;
    }

    public void bindOwner(ServerPlayer player, int index) {
        this.ownerId = player.getUUID();
        this.ownerName = player.getGameProfile().getName();
        this.nameIndex = index;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        if (this.expiresAt != expiresAt) {
            this.expiresAt = expiresAt;
            setChanged();
        }
    }

    public long getRemainingMillis() {
        return Math.max(0L, expiresAt - System.currentTimeMillis());
    }

    public boolean isActive() {
        return getRemainingMillis() > 0L;
    }

    public int getSkullColor() {
        return skullColor;
    }

    public void setSkullColor(int rgb) {
        this.skullColor = rgb & 0xFFFFFF;
        sync();
    }

    public boolean isCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(boolean checkpoint) {
        this.checkpoint = checkpoint;
        sync();
    }

    public int getFreeExtensions() {
        return freeExtensions;
    }

    public void setFreeExtensions(int value) {
        this.freeExtensions = value;
        setChanged();
    }

    public int getExtensions() {
        return extensions;
    }

    public void setExtensions(int value) {
        this.extensions = value;
        setChanged();
    }

    /** Primera apertura: se marca recuperado y se retira la calavera. */
    public void onOpened(ServerPlayer player) {
        if (opened) {
            return;
        }
        this.opened = true;
        this.checkpoint = false;
        setChanged();
        sync();

        DeathHistory.get(level.getServer()).markRecovered(ownerId, storageId);

        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 1.4F);
    }

    public boolean isOpened() {
        return opened;
    }

    public NonNullList<ItemStack> collectAll() {
        NonNullList<ItemStack> copy = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) {
            copy.set(i, items.get(i));
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
        return copy;
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ ciclo

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level.isClientSide()) {
            // La calavera se anima; basta con marcar el bloque como cambiado una vez.
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return super.getRenderBoundingBox().inflate(0.6D, 2.2D, 0.6D);
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putUUID("StorageId", storageId);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putString("OwnerName", ownerName);
        tag.putString("Name", storageName);
        tag.putString("WaypointIcon", waypointIcon);
        tag.putInt("NameIndex", nameIndex);
        tag.putLong("ExpiresAt", expiresAt);
        tag.putInt("SkullColor", skullColor);
        tag.putBoolean("Checkpoint", checkpoint);
        tag.putBoolean("Opened", opened);
        tag.putInt("FreeExtensions", freeExtensions);
        tag.putInt("Extensions", extensions);
        ListTag originList = new ListTag();
        origins.forEach((slot, origin) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("S", slot);
            entry.putByte("T", origin.type);
            entry.putInt("O", origin.slot);
            originList.add(entry);
        });
        tag.put("Origins", originList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        if (tag.hasUUID("StorageId")) {
            storageId = tag.getUUID("StorageId");
        }
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        storageName = tag.getString("Name");
        waypointIcon = tag.getString("WaypointIcon");
        nameIndex = tag.getInt("NameIndex");
        expiresAt = tag.getLong("ExpiresAt");
        skullColor = tag.contains("SkullColor") ? tag.getInt("SkullColor") : DEFAULT_COLOR;
        checkpoint = !tag.contains("Checkpoint") || tag.getBoolean("Checkpoint");
        opened = tag.getBoolean("Opened");
        freeExtensions = tag.getInt("FreeExtensions");
        extensions = tag.getInt("Extensions");
        origins.clear();
        ListTag originList = tag.getList("Origins", 10);
        for (int i = 0; i < originList.size(); i++) {
            CompoundTag entry = originList.getCompound(i);
            origins.put(entry.getInt("S"), new Origin(entry.getByte("T"), entry.getInt("O")));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) {
            load(packet.getTag());
        }
    }

    // ------------------------------------------------------------------ container

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.literal(getStorageName());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DeathStorageMenu(id, inventory, this, worldPosition);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }
}