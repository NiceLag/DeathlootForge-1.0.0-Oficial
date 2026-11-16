package com.example.examplemod.storage;

import com.example.examplemod.AegisData;
import com.example.examplemod.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

import java.util.UUID;

import com.example.examplemod.ExampleMod;

/** Con 0% de perdida los objetos no caen: se guardan en un almacen en el sitio de la muerte. */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeathStorageHandler {

    private static Boolean curiosLoaded;

    private static boolean curiosLoaded() {
        if (curiosLoaded == null) {
            curiosLoaded = ModList.get().isLoaded("curios");
        }
        return curiosLoaded;
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (AegisData.hasProtection(player)) {
            return;
        }
        if (Config.deathLootChance > 0) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        createStorage(player, level);
    }

    private static void createStorage(ServerPlayer player, ServerLevel level) {
        BlockPos pos = SafePlacement.find(level, player.blockPosition());
        if (pos == null) {
            return;
        }

        DeathRecord record = snapshot(player, level, pos);

        BlockState state = ModBlocks.DEATH_STORAGE.get().defaultBlockState();
        level.setBlock(pos, state, Block.UPDATE_ALL);

        if (!(level.getBlockEntity(pos) instanceof DeathStorageBlockEntity storage)) {
            return;
        }

        DeathStorageManager manager = DeathStorageManager.get(level.getServer());
        manager.enforceLimit(level.getServer(), player.getUUID());

        UUID storageId = UUID.randomUUID();
        long expiresAt = System.currentTimeMillis() + Config.storageMinutes * 60_000L;
        int index = manager.nextNameIndex(player.getUUID());

        storage.setStorageId(storageId);
        storage.bindOwner(player, index);
        storage.setExpiresAt(expiresAt);
        storage.setSkullColor(record.skullColor);

        record.storageId = storageId;
        record.storageNameIndex = index;

        moveInventory(player, storage);
        DeathHistory.get(level.getServer()).add(record);
        manager.register(storageId, player.getUUID(), level.dimension(), pos, expiresAt);

        level.playSound(null, pos, SoundEvents.SOUL_ESCAPE, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                24, 0.35D, 0.4D, 0.35D, 0.02D);

        player.displayClientMessage(Component.translatable("deathlootforge.storage.created", index), false);
    }

    /** Guarda todo antes de vaciar, para que la instantanea sea exacta. */
    private static DeathRecord snapshot(ServerPlayer player, ServerLevel level, BlockPos pos) {
        DeathRecord record = new DeathRecord();
        record.ownerId = player.getUUID();
        record.ownerName = player.getGameProfile().getName();
        record.dimension = level.dimension().location().toString();
        record.pos = pos;
        record.cause = player.getCombatTracker().getDeathMessage().getString();

        Inventory inventory = player.getInventory();
        record.selectedSlot = inventory.selected;

        for (int slot = 0; slot < 36; slot++) {
            record.inventory.add(inventory.getItem(slot).copy());
        }
        for (int slot = 0; slot < 4; slot++) {
            record.armor.add(inventory.armor.get(slot).copy());
        }
        record.offhand = inventory.offhand.get(0).copy();

        if (curiosLoaded()) {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                IItemHandlerModifiable equipped = handler.getEquippedCurios();
                for (int slot = 0; slot < equipped.getSlots(); slot++) {
                    record.curios.add(equipped.getStackInSlot(slot).copy());
                }
            });
        }

        collectBackpacks(player, record);

        return record;
    }

        /** Copia icono y contenido de cada mochila para poder revisarla despues. */
    private static void collectBackpacks(ServerPlayer player, DeathRecord record) {
        if (!ModList.get().isLoaded("sophisticatedbackpacks")) {
            return;
        }

        Inventory inventory = player.getInventory();

        for (int slot = 0; slot < 36; slot++) {
            addBackpack(record, inventory.getItem(slot), slot, false);
        }
        for (int slot = 0; slot < 4; slot++) {
            addBackpack(record, inventory.armor.get(slot), slot, false);
        }
        addBackpack(record, inventory.offhand.get(0), 0, false);

        if (curiosLoaded()) {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                IItemHandlerModifiable equipped = handler.getEquippedCurios();
                for (int slot = 0; slot < equipped.getSlots(); slot++) {
                    addBackpack(record, equipped.getStackInSlot(slot), slot, true);
                }
            });
        }
    }

    private static void addBackpack(DeathRecord record, ItemStack stack, int slot, boolean curios) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BackpackItem)) {
            return;
        }

        DeathRecord.Backpack backpack = new DeathRecord.Backpack();
        backpack.slot = slot;
        backpack.curiosSlot = curios;
        backpack.icon = stack.copy();

        var handler = new BackpackWrapper(stack).getInventoryHandler();
        for (int i = 0; i < handler.getSlots(); i++) {
            backpack.contents.add(handler.getStackInSlot(i).copy());
        }

        record.backpacks.add(backpack);
    }

    private static void moveInventory(ServerPlayer player, DeathStorageBlockEntity storage) {
    Inventory inventory = player.getInventory();
    int index = 0;

    for (int slot = 0; slot < 36; slot++) {
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty()) {
            continue;
        }
        storage.setItem(index, stack);
        storage.setOrigin(index, DeathStorageBlockEntity.ORIGIN_INV, slot);
        inventory.setItem(slot, ItemStack.EMPTY);
        index++;
    }

    for (int slot = 0; slot < 4; slot++) {
        ItemStack stack = inventory.armor.get(slot);
        if (stack.isEmpty()) {
            continue;
        }
        storage.setItem(index, stack);
        storage.setOrigin(index, DeathStorageBlockEntity.ORIGIN_ARMOR, slot);
        inventory.armor.set(slot, ItemStack.EMPTY);
        index++;
    }

    ItemStack offhand = inventory.offhand.get(0);
    if (!offhand.isEmpty()) {
        storage.setItem(index, offhand);
        storage.setOrigin(index, DeathStorageBlockEntity.ORIGIN_OFFHAND, 0);
        inventory.offhand.set(0, ItemStack.EMPTY);
        index++;
    }

    if (curiosLoaded()) {
        int[] cursor = {index};
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable equipped = handler.getEquippedCurios();
            for (int slot = 0; slot < equipped.getSlots() && cursor[0] < DeathStorageBlockEntity.SIZE; slot++) {
                ItemStack stack = equipped.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                storage.setItem(cursor[0], stack);
                storage.setOrigin(cursor[0], DeathStorageBlockEntity.ORIGIN_CURIO, slot);
                equipped.setStackInSlot(slot, ItemStack.EMPTY);
                cursor[0]++;
            }
        });
    }
}
}