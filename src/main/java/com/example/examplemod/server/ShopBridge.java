package com.example.examplemod.server;

import com.example.examplemod.currency.Credits;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Cargos que quedan en el historial de la tienda si esta instalada.
 * Todo por reflexion: la tienda no es dependencia de compilacion.
 */
public final class ShopBridge {

    private static Boolean loaded;
    private static Method apply;
    private static Object entryCompra;

    private ShopBridge() {
    }

    private static boolean present() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("secretshop");
            if (loaded) {
                resolve();
            }
        }
        return loaded && apply != null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void resolve() {
        try {
            Class<?> service = Class.forName("com.secretshop.shop.ShopService");
            Class<?> type = Class.forName("com.secretshop.shop.EntryType");

            entryCompra = Enum.valueOf((Class<? extends Enum>) type, "COMPRA");
            apply = service.getMethod("apply", ServerPlayer.class, type,
                    String.class, long.class, long.class, long.class);
        } catch (Throwable throwable) {
            apply = null;
        }
    }

    /** Cobra `milli` mili-creditos y lo registra en el historial de la tienda. */
    public static void charge(ServerPlayer player, String description, long milli) {
        if (milli <= 0L) {
            return;
        }

        if (present()) {
            try {
                apply.invoke(null, player, entryCompra, description, 0L, milli, 0L);
                return;
            } catch (Throwable throwable) {
                // Si la firma cambia, se cae al cobro directo.
            }
        }

        Credits.take(player, milli);
    }
}