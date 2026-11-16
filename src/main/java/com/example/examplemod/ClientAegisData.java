package com.example.examplemod;

/** Cache del cliente. Solo informativo para el HUD. */
public final class ClientAegisData {

    private static int charges = 0;

    private ClientAegisData() {
    }

    public static int getCharges() {
        return charges;
    }

    public static void setCharges(int value) {
        charges = Math.max(0, value);
    }

    public static void reset() {
        charges = 0;
    }
}