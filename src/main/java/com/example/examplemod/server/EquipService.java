package com.example.examplemod.server;

import com.example.examplemod.storage.DeathStorageBlockEntity;
import com.example.examplemod.storage.DeathStorageMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

/** Devuelve cada objeto a su slot original. Lo que no quepa se reubica o cae. */
public final class EquipService {

    private EquipService() {
    }

    public static void equipAll(ServerPlayer player) {
        if (!(player.containerMenu instanceof DeathStorageMenu menu)) {
            return;
        }

        DeathStorageBlockEntity storage = menu.getStorage();
        if (storage == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        List<ItemStack> overflow = new ArrayList<>();
        boolean curios = ModList.get().isLoaded("curios");

        for (int i = 0; i < DeathStorageBlockEntity.SIZE; i++) {
            ItemStack stack = storage.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            DeathStorageBlockEntity.Origin origin = storage.getOrigin(i);
            boolean placed = origin != null && place(player, inventory, curios, stack, origin);

            if (!placed) {
                overflow.add(stack);
            }

            storage.setItem(i, ItemStack.EMPTY);
            storage.clearOrigin(i);
        }

        for (ItemStack stack : overflow) {
            if (!inventory.add(stack)) {
                player.drop(stack, false);
            }
        }

        storage.sync();
        player.containerMenu.broadcastChanges();
        player.playNotifySound(SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static boolean place(ServerPlayer player, Inventory inventory, boolean curios,
                                 ItemStack stack, DeathStorageBlockEntity.Origin origin) {
        switch (origin.type) {
            case DeathStorageBlockEntity.ORIGIN_ARMOR -> {
                if (origin.slot >= 0 && origin.slot < 4 && inventory.armor.get(origin.slot).isEmpty()) {
                    inventory.armor.set(origin.slot, stack);
                    return true;
                }
            }
            case DeathStorageBlockEntity.ORIGIN_OFFHAND -> {
                if (inventory.offhand.get(0).isEmpty()) {
                    inventory.offhand.set(0, stack);
                    return true;
                }
            }
            case DeathStorageBlockEntity.ORIGIN_CURIO -> {
                if (curios && placeCurio(player, origin.slot, stack)) {
                    return true;
                }
            }
            case DeathStorageBlockEntity.ORIGIN_INV -> {
                if (origin.slot >= 0 && origin.slot < 36 && inventory.getItem(origin.slot).isEmpty()) {
                    inventory.setItem(origin.slot, stack);
                    return true;
                }
            }
            default -> {
            }
        }

        return false;
    }

    private static boolean placeCurio(ServerPlayer player, int slot, ItemStack stack) {
        var handler = CuriosApi.getCuriosInventory(player).orElse(null);
        if (handler == null) {
            return false;
        }

        IItemHandlerModifiable equipped = handler.getEquippedCurios();
        if (slot < 0 || slot >= equipped.getSlots() || !equipped.getStackInSlot(slot).isEmpty()) {
            return false;
        }

        equipped.setStackInSlot(slot, stack);
        return true;
    }
}