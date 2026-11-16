package com.example.examplemod;

import net.minecraft.world.entity.player.Player;

/** Cargas por jugador. Vive en el NBT persistente (sobrevive muerte, logout y reinicio). */
public final class AegisData {

    public static final String KEY = "aegis_charges";

    private AegisData() {
    }

    public static int getCharges(Player player) {
        return Math.max(0, player.getPersistentData().getInt(KEY));
    }

    public static void setCharges(Player player, int value) {
        player.getPersistentData().putInt(KEY, Math.max(0, value));
    }

    public static int addCharges(Player player, int amount) {
        int total = getCharges(player) + Math.max(0, amount);
        setCharges(player, total);
        return total;
    }

    public static int consumeOne(Player player) {
        int charges = getCharges(player);
        if (charges <= 0) {
            return 0;
        }
        int remaining = charges - 1;
        setCharges(player, remaining);
        return remaining;
    }

    public static boolean hasProtection(Player player) {
        return getCharges(player) > 0;
    }
}