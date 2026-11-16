package com.example.examplemod.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DeathStorageMenu extends AbstractContainerMenu {

    public static final int COLUMNS = 9;
    public static final int ROWS = 6;
    public static final int GRID_X = 19;
    public static final int GRID_Y = 28;

    private final Container container;
    private final BlockPos pos;
    @Nullable
    private final DeathStorageBlockEntity storage;

    public DeathStorageMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, resolve(inventory.player, pos), pos);
    }

    private static Container resolve(Player player, BlockPos pos) {
        return player.level().getBlockEntity(pos) instanceof DeathStorageBlockEntity be
                ? be
                : new SimpleContainer(DeathStorageBlockEntity.SIZE);
    }

    public DeathStorageMenu(int id, Inventory inventory, Container container, BlockPos pos) {
        super(ModMenus.DEATH_STORAGE.get(), id);
        this.container = container;
        this.pos = pos;
        this.storage = container instanceof DeathStorageBlockEntity be ? be : null;

        container.startOpen(inventory.player);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(new Slot(container, col + row * COLUMNS, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, GRID_X + col * 18, 150 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, GRID_X + col * 18, 208));
        }
    }

    @Nullable
    public DeathStorageBlockEntity getStorage() {
        return storage;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return storage != null ? storage.stillValid(player) : true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (!slot.hasItem()) {
            return result;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();

        int storageEnd = ROWS * COLUMNS;

        if (index < storageEnd) {
            if (!moveItemStackTo(stack, storageEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, storageEnd, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}