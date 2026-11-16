package com.example.examplemod.currency;

import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/** Puente por reflexion a com.secretshop.currency.CreditManager. Sin el mod de tienda todo es gratis. */
public final class Credits {

    private static boolean resolved;
    private static boolean present;
    private static Method getMethod;
    private static Method takeMethod;

    private Credits() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;

        try {
            Class<?> manager = Class.forName("com.secretshop.currency.CreditManager");
            getMethod = manager.getMethod("get", Player.class);
            takeMethod = manager.getMethod("take", Player.class, long.class);
            present = true;
        } catch (Throwable ignored) {
            present = false;
        }
    }

    public static boolean available() {
        resolve();
        return present;
    }

    public static long get(Player player) {
        resolve();
        if (!present) {
            return Long.MAX_VALUE;
        }
        try {
            return (long) getMethod.invoke(null, player);
        } catch (Throwable ignored) {
            return Long.MAX_VALUE;
        }
    }

    public static boolean take(Player player, long amount) {
        resolve();
        if (amount <= 0L || !present) {
            return true;
        }
        try {
            return (boolean) takeMethod.invoke(null, player, amount);
        } catch (Throwable ignored) {
            return true;
        }
    }
}