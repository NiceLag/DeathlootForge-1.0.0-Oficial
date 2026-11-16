package com.example.examplemod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AegisHandler {

    private static final String SNAPSHOT = "aegis_snapshot";
    private static final String PENDING = "aegis_pending";
    private static Boolean curiosLoaded;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        int charges = AegisData.getCharges(player);
        if (charges <= 0) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        if (data.contains(SNAPSHOT)) {
            return;
        }

        CompoundTag snapshot = new CompoundTag();
        snapshot.put("inventory", player.getInventory().save(new ListTag()));
        snapshot.putInt("xpLevel", player.experienceLevel);
        snapshot.putFloat("xpProgress", player.experienceProgress);
        snapshot.putDouble("x", player.getX());
        snapshot.putDouble("y", player.getY());
        snapshot.putDouble("z", player.getZ());
        if (curiosLoaded()) {
            snapshot.put("curios", snapshotCurios(player));
        }
        data.put(SNAPSHOT, snapshot);

        player.getInventory().clearContent();
        if (curiosLoaded()) {
            clearCurios(player);
        }

        // Sin orbes de experiencia: evita duplicarla.
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;

        int remaining = AegisData.consumeOne(player);

        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    32, 0.5D, 0.8D, 0.5D, 0.3D);
        }

        AegisNetwork.sendSync(player, remaining);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        newPlayer.getPersistentData().merge(oldPlayer.getPersistentData().copy());

        if (!(newPlayer instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!event.isWasDeath()) {
            AegisNetwork.sendSync(serverPlayer, AegisData.getCharges(serverPlayer));
            return;
        }

        CompoundTag data = serverPlayer.getPersistentData();
        if (!data.contains(SNAPSHOT)) {
            AegisNetwork.sendSync(serverPlayer, AegisData.getCharges(serverPlayer));
            return;
        }

        CompoundTag snapshot = data.getCompound(SNAPSHOT);

        serverPlayer.getInventory().load(snapshot.getList("inventory", 10));
        serverPlayer.experienceLevel = snapshot.getInt("xpLevel");
        serverPlayer.experienceProgress = snapshot.getFloat("xpProgress");

        data.putBoolean(PENDING, true);
        AegisNetwork.sendSync(serverPlayer, AegisData.getCharges(serverPlayer));
    }

    /** Reemplaza al tick handler: la restauración ocurre una sola vez, al reaparecer. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(PENDING)) {
            return;
        }

        CompoundTag snapshot = data.getCompound(SNAPSHOT);

        if (curiosLoaded() && snapshot.contains("curios")) {
            restoreCurios(player, snapshot.getList("curios", 10));
        }

        if (player.level() instanceof ServerLevel serverLevel && snapshot.contains("x")) {
            double x = snapshot.getDouble("x");
            double y = snapshot.getDouble("y");
            double z = snapshot.getDouble("z");
            AABB box = new AABB(x - 3.0D, y - 3.0D, z - 3.0D, x + 3.0D, y + 3.0D, z + 3.0D);
            for (ExperienceOrb orb : serverLevel.getEntitiesOfClass(ExperienceOrb.class, box)) {
                orb.discard();
            }
        }

        data.putBoolean(PENDING, false);
        data.remove(SNAPSHOT);

        // Aviso fuera del chat, al reaparecer.
        AegisNetwork.sendNotice(player, AegisNetwork.NOTICE_PROTECTED, AegisData.getCharges(player));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AegisNetwork.sendSync(serverPlayer, AegisData.getCharges(serverPlayer));
        }
    }

    private static boolean curiosLoaded() {
        if (curiosLoaded == null) {
            curiosLoaded = ModList.get().isLoaded("curios");
        }
        return curiosLoaded;
    }

    private static ListTag snapshotCurios(Player player) {
        ListTag list = new ListTag();

        ICuriosItemHandler handler = CuriosApi.getCuriosInventory(player).orElse(null);
        if (handler == null) {
            return list;
        }

        IItemHandlerModifiable equipped = handler.getEquippedCurios();

        for (int slot = 0, size = equipped.getSlots(); slot < size; slot++) {
            ItemStack stack = equipped.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) slot);
                stack.save(entry);
                list.add(entry);
            }
        }

        return list;
    }

    private static void clearCurios(Player player) {
        ICuriosItemHandler handler = CuriosApi.getCuriosInventory(player).orElse(null);
        if (handler == null) {
            return;
        }

        IItemHandlerModifiable equipped = handler.getEquippedCurios();

        for (int slot = 0, size = equipped.getSlots(); slot < size; slot++) {
            equipped.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private static void restoreCurios(Player player, ListTag list) {
        ICuriosItemHandler handler = CuriosApi.getCuriosInventory(player).orElse(null);
        if (handler == null) {
            return;
        }

        IItemHandlerModifiable equipped = handler.getEquippedCurios();

        for (int index = 0, size = list.size(); index < size; index++) {
            CompoundTag entry = list.getCompound(index);
            int slot = entry.getByte("Slot") & 255;
            if (slot < equipped.getSlots()) {
                equipped.setStackInSlot(slot, ItemStack.of(entry));
            }
        }
    }
}