package com.example.examplemod.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class UiKit {

    public static final int PANEL_TOP = 0xF0090C11;
    public static final int PANEL_BOTTOM = 0xF0171E27;
    public static final int BORDER = 0x33FFFFFF;
    public static final int INNER = 0x1AFFFFFF;
    public static final int SLOT_BG = 0xFF141417;
    public static final int ACCENT = 0xFF4FA8FF;
    public static final int GOOD = 0xFF6EDC8B;
    public static final int WARN = 0xFFFFC24B;
    public static final int BAD = 0xFFFF6B6B;
    public static final int TEXT = 0xFFEAF2FF;
    public static final int DIM = 0xFF8A9AAB;
    public static final int FAINT = 0xFF566472;

    public static final int CARD = 0xFF0B0C0E;
    public static final int EDGE = 0xFF2A2C31;
    public static final int GREEN = 0xFF22C55E;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK_TEXT = 0xFF101014;

    private UiKit() {
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        vertical(graphics, x, y, width, height, PANEL_TOP, PANEL_BOTTOM);
        outline(graphics, x, y, width, height, BORDER);
    }

    public static void vertical(GuiGraphics graphics, int x, int y, int width, int height, int top, int bottom) {
        for (int row = 0; row < height; row++) {
            float t = height <= 1 ? 0.0F : row / (float) height;
            graphics.fill(x, y + row, x + width, y + row + 1, blend(top, bottom, t));
        }
    }

    public static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    /** Tarjeta con esquinas recortadas a 2px. */
    public static void round(GuiGraphics graphics, int x, int y, int width, int height, int bg, int edge) {
        graphics.fill(x + 2, y, x + width - 2, y + height, bg);
        graphics.fill(x, y + 2, x + width, y + height - 2, bg);

        if (edge == 0) {
            return;
        }

        graphics.fill(x + 2, y, x + width - 2, y + 1, edge);
        graphics.fill(x + 2, y + height - 1, x + width - 2, y + height, edge);
        graphics.fill(x, y + 2, x + 1, y + height - 2, edge);
        graphics.fill(x + width - 1, y + 2, x + width, y + height - 2, edge);
    }

    public static void slot(GuiGraphics graphics, int x, int y) {
        round(graphics, x, y, 18, 18, SLOT_BG, EDGE);
    }

    public static void pill(GuiGraphics graphics, Font font, int x, int y, String label, int color) {
        int width = font.width(label) + 10;
        round(graphics, x, y, width, 12, (color & 0xFFFFFF) | 0x26000000, (color & 0xFFFFFF) | 0x55000000);
        graphics.drawString(font, label, x + 5, y + 2, color, false);
    }

    public static void chip(GuiGraphics graphics, Font font, int x, int y, String label, int color, boolean filled) {
        int width = font.width(label) + 8;
        int bg = filled ? (color & 0xFFFFFF) | 0x33000000 : 0x44000000;
        graphics.fill(x, y, x + width, y + 12, bg);
        outline(graphics, x, y, width, 12, (color & 0xFFFFFF) | 0x66000000);
        graphics.drawString(font, label, x + 4, y + 2, color, false);
    }

    public static void star(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 3, y, x + 5, y + 9, color);
        graphics.fill(x, y + 3, x + 8, y + 5, color);
        graphics.fill(x + 1, y + 1, x + 3, y + 3, color);
        graphics.fill(x + 5, y + 1, x + 7, y + 3, color);
    }

    public static void trash(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 1, y, x + 8, y + 1, color);
        graphics.fill(x + 2, y + 1, x + 7, y + 9, color);
        graphics.fill(x + 3, y + 3, x + 6, y + 8, 0xFF0B0C0E);
        graphics.fill(x + 4, y - 2, x + 5, y, color);
    }

    public static void pencil(GuiGraphics graphics, int x, int y, int color) {
        for (int i = 0; i < 7; i++) {
            graphics.fill(x + 4 + i, y + 8 - i, x + 7 + i, y + 10 - i, color);
        }
        graphics.fill(x + 2, y + 9, x + 5, y + 12, color);
        graphics.fill(x, y + 12, x + 2, y + 14, color);
        graphics.fill(x + 11, y, x + 14, y + 3, color);
    }

    public static void skull(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 1, y, x + 6, y + 4, color);
        graphics.fill(x, y + 1, x + 7, y + 4, color);
        graphics.fill(x + 1, y + 4, x + 6, y + 6, color);
        graphics.fill(x + 2, y + 2, x + 3, y + 3, 0xFF10141A);
        graphics.fill(x + 4, y + 2, x + 5, y + 3, 0xFF10141A);
    }

    public static void close(GuiGraphics graphics, int x, int y, int color) {
        for (int i = 0; i < 7; i++) {
            graphics.fill(x + i, y + i, x + i + 1, y + i + 1, color);
            graphics.fill(x + 6 - i, y + i, x + 7 - i, y + i + 1, color);
        }
    }

    public static void clock(GuiGraphics graphics, int cx, int cy, int color) {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                int dx = col - 4;
                int dy = row - 4;
                int d = dx * dx + dy * dy;
                if (d >= 12 && d <= 20) {
                    graphics.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
                }
            }
        }
        graphics.fill(cx, cy - 3, cx + 1, cy + 1, color);
        graphics.fill(cx, cy, cx + 3, cy + 1, color);
    }

    public static void copy(GuiGraphics graphics, int x, int y, int color) {
        outline(graphics, x + 3, y + 3, 8, 9, color);
        graphics.fill(x + 3, y + 3, x + 11, y + 12, CARD);
        outline(graphics, x, y, 8, 9, color);
    }

    public static String timer(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format("%02d:%02d:%02d", seconds / 3600L, (seconds % 3600L) / 60L, seconds % 60L);
    }

    public static String coords(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    public static String coordsPretty(BlockPos pos) {
        return "X: " + pos.getX() + " \u00B7 Y: " + pos.getY() + " \u00B7 Z: " + pos.getZ();
    }

    public static String date(long epoch) {
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.ofEpochMilli(epoch));
    }

    public static String hour(long epoch) {
        return java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.ofEpochMilli(epoch));
    }

    public static int blend(int from, int to, float t) {
        int a = (int) (((from >>> 24) & 255) + ((((to >>> 24) & 255) - ((from >>> 24) & 255)) * t));
        int r = (int) (((from >> 16) & 255) + ((((to >> 16) & 255) - ((from >> 16) & 255)) * t));
        int g = (int) (((from >> 8) & 255) + ((((to >> 8) & 255) - ((from >> 8) & 255)) * t));
        int b = (int) ((from & 255) + (((to & 255) - (from & 255)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    
    /** Cuadro pequeño con la inicial del waypoint. */
    public static void badge(GuiGraphics graphics, Font font, int x, int y, String text, int color, boolean dim) {
        int width = 14;
        round(graphics, x, y, width, width, 0xFF0E1116, dim ? 0xFF3A3D44 : ((color & 0xFFFFFF) | 0xFF000000));

        String label = text == null ? "" : text;
        while (label.length() > 1 && font.width(label) > width - 3) {
            label = label.substring(0, label.length() - 1);
        }

        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + 3,
                dim ? 0xFF6B7280 : color, false);
    }

    public static int parseHex(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String clean = value.startsWith("#") ? value.substring(1) : value;
        if (clean.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(clean, 16) & 0xFFFFFF;
        } catch (NumberFormatException error) {
            return fallback;
        }
    }


    public static String dimension(ResourceKey<Level> key) {
        return dimension(key.location().toString());
    }

    public static String dimension(String id) {
        if (id == null) {
            return "?";
        }
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        return switch (path) {
            case "overworld" -> "Supervivencia";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> path;
        };
    }
}