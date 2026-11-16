package com.example.examplemod;

import net.minecraft.client.gui.GuiGraphics;

/** Primitivas de dibujo. Solo fill(): sin texturas ni asignaciones. */
public final class AegisRender {

    public static final int BLUE = 0xFF3D8BFD;
    public static final int AMBER = 0xFFFFC24B;
    public static final int TEXT = 0xFFEAF2FF;
    public static final int TEXT_DIM = 0xFF9CC7FF;

    public static final int PANEL_BG = 0x8A0E1218;
    public static final int PANEL_BORDER = 0x22FFFFFF;

    /** Silueta del escudo de la referencia: pico arriba, lados rectos, punta abajo. */
    private static final int[] SHIELD_ROWS = {6, 11, 16, 16, 16, 16, 16, 16, 16, 16, 16, 15, 14, 12, 10, 8, 5, 2};

    public static final int SHIELD_W = 16;
    public static final int SHIELD_H = SHIELD_ROWS.length;

    /** Misma silueta, reducida a 11x13 para el panel del inventario. */
    private static final int[] SHIELD_SMALL_ROWS = {4, 8, 11, 11, 11, 11, 11, 11, 10, 9, 7, 5, 2};

    public static final int SHIELD_SMALL_W = 11;
    public static final int SHIELD_SMALL_H = SHIELD_SMALL_ROWS.length;

    // Desplazamiento izquierdo de cada fila, precalculado: cero divisiones en runtime.
    private static final int[] SHIELD_LEFT = lefts(SHIELD_W, SHIELD_ROWS);
    private static final int[] SHIELD_SMALL_LEFT = lefts(SHIELD_SMALL_W, SHIELD_SMALL_ROWS);

    // Halo: mismas filas ensanchadas en 2px, también precalculado.
    private static final int[] SHIELD_GLOW_W = widen(SHIELD_ROWS, 2);
    private static final int[] SHIELD_GLOW_LEFT = lefts(SHIELD_W, SHIELD_GLOW_W);
    private static final int[] SHIELD_SMALL_GLOW_W = widen(SHIELD_SMALL_ROWS, 2);
    private static final int[] SHIELD_SMALL_GLOW_LEFT = lefts(SHIELD_SMALL_W, SHIELD_SMALL_GLOW_W);

    private static final int[] WARNING_WIDTHS = {2, 2, 4, 4, 6, 6, 8, 9};
    private static final int[] WARNING_LEFT = lefts(9, WARNING_WIDTHS);

    private AegisRender() {
    }

    private static int[] lefts(int width, int[] rows) {
        int[] out = new int[rows.length];
        for (int i = 0; i < rows.length; i++) {
            out[i] = (width - rows[i]) / 2;
        }
        return out;
    }

    private static int[] widen(int[] rows, int amount) {
        int[] out = new int[rows.length];
        for (int i = 0; i < rows.length; i++) {
            out[i] = rows[i] + amount;
        }
        return out;
    }

    public static int alpha(int argb, float a) {
        if (a >= 1.0F) {
            return argb;
        }
        if (a <= 0.0F) {
            return argb & 0xFFFFFF;
        }
        return (((int) ((argb >>> 24) * a)) << 24) | (argb & 0xFFFFFF);
    }

    public static int lerp(int from, int to, float t) {
        if (t <= 0.0F) {
            return from;
        }
        if (t >= 1.0F) {
            return to;
        }
        int a = (from >>> 24) & 255;
        int r = (from >> 16) & 255;
        int g = (from >> 8) & 255;
        int b = from & 255;
        int a2 = (to >>> 24) & 255;
        int r2 = (to >> 16) & 255;
        int g2 = (to >> 8) & 255;
        int b2 = to & 255;
        return (((int) (a + (a2 - a) * t)) << 24)
                | (((int) (r + (r2 - r) * t)) << 16)
                | (((int) (g + (g2 - g) * t)) << 8)
                | ((int) (b + (b2 - b) * t));
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int bg, int border) {
        graphics.fill(x, y, x + width, y + height, bg);
        graphics.fill(x + 1, y, x + width - 1, y + 1, border);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, border);
        graphics.fill(x, y + 1, x + 1, y + height - 1, border);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, border);
    }

    /** Escudo grande, para el HUD y los avisos. */
    public static void shield(GuiGraphics graphics, int x, int y, boolean active, float a) {
        shape(graphics, x, y, SHIELD_ROWS, SHIELD_LEFT, SHIELD_GLOW_W, SHIELD_GLOW_LEFT, SHIELD_W, active, a);
    }

    /** Escudo compacto para el panel del inventario: misma silueta, 11x13. */
    public static void shieldSmall(GuiGraphics graphics, int x, int y, boolean active, float a) {
        shape(graphics, x, y, SHIELD_SMALL_ROWS, SHIELD_SMALL_LEFT, SHIELD_SMALL_GLOW_W, SHIELD_SMALL_GLOW_LEFT, SHIELD_SMALL_W, active, a);
    }

    /**
     * Silueta negra con mitad derecha negra y mitad izquierda en rojo (sin cargas)
     * o verde (con cargas).
     */
    private static void shape(GuiGraphics graphics, int x, int y, int[] rows, int[] lefts,
                              int[] glowW, int[] glowLeft, int width, boolean active, float a) {
        int glow = alpha(0x33FFFFFF, a);
        int black = alpha(0xFF0B0E12, a);
        int color = alpha(active ? 0xFF3FBF5F : 0xFFD8443F, a);
        int center = x + (width >> 1);

        // Halo tenue para separarlo del panel
        for (int row = 0; row < rows.length; row++) {
            int left = x + glowLeft[row];
            graphics.fill(left, y + row - 1, left + glowW[row], y + row + 2, glow);
        }

        // Silueta negra completa
        for (int row = 0; row < rows.length; row++) {
            int left = x + lefts[row];
            graphics.fill(left, y + row, left + rows[row], y + row + 1, black);
        }

        // Mitad izquierda coloreada, con 1px de margen negro
        for (int row = 1; row < rows.length - 1; row++) {
            int left = x + lefts[row];
            int end = Math.min(center, left + rows[row] - 1);
            if (end > left + 1) {
                graphics.fill(left + 1, y + row, end, y + row + 1, color);
            }
        }
    }

    public static void plus(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 4, y + 1, x + 5, y + 8, color);
        graphics.fill(x + 1, y + 4, x + 8, y + 5, color);
    }

    public static void warning(GuiGraphics graphics, int x, int y, int color, int mark) {
        for (int row = 0; row < WARNING_WIDTHS.length; row++) {
            int left = x + WARNING_LEFT[row];
            graphics.fill(left, y + row, left + WARNING_WIDTHS[row], y + row + 1, color);
        }
        graphics.fill(x + 4, y + 2, x + 5, y + 5, mark);
        graphics.fill(x + 4, y + 6, x + 5, y + 7, mark);
    }
}