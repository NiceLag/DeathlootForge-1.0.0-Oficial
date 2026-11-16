package com.example.examplemod;

import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayDeque;
import java.util.Deque;

@Mod.EventBusSubscriber(
        modid = ExampleMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class DeathLootHandler {

    private static final ItemStack EMPTY = ItemStack.EMPTY;

        private static Boolean curiosLoaded;

    private static boolean curiosLoaded() {
        if (curiosLoaded == null) {
            curiosLoaded = ModList.get().isLoaded("curios");
        }
        return curiosLoaded;
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // No afectar a jugadores en creativo ni espectadores.
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        // Con una carga de Aegis el handler de Aegis conserva todo.
        if (AegisData.hasProtection(player)) {
            return;
        }

        final int chance = Config.deathLootChance;

        // Con 0%, no se pierde ningún objeto.
        if (chance <= 0) {
            return;
        }

        // Todo lo constante se resuelve una sola vez fuera de los loops:
        // sin lookups repetidos y sin RNG cuando la pérdida es segura.
        final RandomSource random = player.getRandom();
        final boolean certain = chance >= 100;
        final Inventory inventory = player.getInventory();
        final Deque<ItemStack> backpacks = new ArrayDeque<>();

        // Inventario principal (0-35). Las mochilas se encolan.
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof BackpackItem) {
                backpacks.push(stack);
                continue;
            }

            if (certain || random.nextInt(100) < chance) {
                inventory.setItem(slot, EMPTY);
            }
        }

        // Armadura equipada.
        clearOrQueue(inventory.armor, backpacks, random, certain, chance);

        // Mano secundaria.
        clearOrQueue(inventory.offhand, backpacks, random, certain, chance);

        // Curios: solo mochilas equipadas. El resto queda intacto.
if (curiosLoaded()) {
    var curiosOptional = CuriosApi.getCuriosInventory(player);

    curiosOptional.ifPresent(curiosHandler -> {
        IItemHandlerModifiable curios = curiosHandler.getEquippedCurios();

        for (int slot = 0, size = curios.getSlots(); slot < size; slot++) {
            ItemStack stack = curios.getStackInSlot(slot);

            if (!stack.isEmpty()
                    && stack.getItem() instanceof BackpackItem) {
                backpacks.push(stack);
            }
        }
    });
}

        // Vaciar el contenido de todas las mochilas (anidadas incluidas).
        ItemStack backpack;
        while ((backpack = backpacks.poll()) != null) {
            processBackpack(backpack, backpacks, random, certain, chance);
        }
    }

    /**
     * Borra los slots indicados con la probabilidad configurada.
     * Las mochilas encontradas se conservan y se encolan.
     */
    private static void clearOrQueue(NonNullList<ItemStack> slots,
                                     Deque<ItemStack> backpacks,
                                     RandomSource random,
                                     boolean certain,
                                     int chance) {
        for (int slot = 0, size = slots.size(); slot < size; slot++) {
            ItemStack stack = slots.get(slot);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof BackpackItem) {
                backpacks.push(stack);
                continue;
            }

            if (certain || random.nextInt(100) < chance) {
                slots.set(slot, EMPTY);
            }
        }
    }

    private static void processBackpack(ItemStack backpack,
                                        Deque<ItemStack> backpacks,
                                        RandomSource random,
                                        boolean certain,
                                        int chance) {
        if (backpack.isEmpty()
                || !(backpack.getItem() instanceof BackpackItem)) {
            return;
        }

        var inventory = new BackpackWrapper(backpack).getInventoryHandler();

        for (int slot = 0, size = inventory.getSlots(); slot < size; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);

            if (stack.isEmpty()) {
                continue;
            }

            // Mochila anidada: se conserva y se revisa después.
            if (stack.getItem() instanceof BackpackItem) {
                backpacks.push(stack);
                continue;
            }

            if (certain || random.nextInt(100) < chance) {
                inventory.setStackInSlot(slot, EMPTY);
            }
        }
    }
}