package com.example.examplemod.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

public final class WaypointIcon {

    public static final String[] KEYS = {
            "skull", "target", "flag", "triangle", "star",
            "heart", "crosshair", "bolt", "shield", "gear"
    };

    private static final String[][] MASKS = {
            {".#####.", "#######", "#.###.#", "#######", ".#####.", ".#.#.#."},
            {".#####.", "##...##", "#..#..#", "#.###.#", "#..#..#", "##...##", ".#####."},
            {"#......", "###....", "#####..", "###....", "#......", "#......", "#......"},
            {"...#...", "..###..", ".#####.", "#######"},
            {".##.##.", "#######", "#######", ".#####.", "..###..", "...#..."},
            {"...#...", ".#.#.#.", "..###..", "#######", "..###..", ".#.#.#.", "...#..."},
            {"....##.", "...##..", "..###..", ".#####.", "..##...", ".##....", "##....."},
            {"#######", "#######", "#######", ".#####.", ".#####.", "..###..", "...#..."},
            {"#######", "#######", "#######", ".#####.", ".#####.", "..###..", "...#..."},
            {"#.###.#", "#######", ".#####.", "#######", ".#####.", "#######", "#.###.#"}
    };

    private static final int GRID = 7;

    private WaypointIcon() {
    }

    public static String name(String key) {
        return Component.translatable("deathlootforge.icon." + key).getString();
    }

    public static boolean valid(String key) {
        return indexOf(key) >= 0;
    }

    public static void gui(GuiGraphics graphics, String key, int x, int y, int size, int color) {
        int index = indexOf(key);
        if (index < 0 || index >= MASKS.length) {
            return;
        }

        String[] mask = MASKS[index];
        int step = Math.max(1, size / GRID);
        int offsetX = x + (size - step * GRID) / 2;
        int offsetY = y + (size - step * mask.length) / 2;

        for (int row = 0; row < mask.length; row++) {
            String line = mask[row];
            for (int col = 0; col < GRID; col++) {
                if (line.charAt(col) == '1') {
                    int px = offsetX + col * step;
                    int py = offsetY + row * step;
                    graphics.fill(px, py, px + step, py + step, color);
                }
            }
        }
    }

    public static void guiCentered(GuiGraphics graphics, String key, int cx, int cy, int size, int color) {
        gui(graphics, key, cx - size / 2, cy - size / 2, size, color);
    }

    public static void world(VertexConsumer consumer, Matrix4f matrix, String key,
                             float cx, float cy, float size, float r, float g, float b, float a) {
        int index = indexOf(key);
        if (index < 0 || index >= MASKS.length) {
            return;
        }

        String[] mask = MASKS[index];
        float step = size / GRID;
        float offsetX = cx - step * GRID / 2.0F;
        float offsetY = cy - step * mask.length / 2.0F;

        for (int row = 0; row < mask.length; row++) {
            String line = mask[row];
            for (int col = 0; col < GRID; col++) {
                if (line.charAt(col) == '1') {
                    float px = offsetX + col * step;
                    float py = offsetY + row * step;
                    quad(consumer, matrix, r, g, b, a, px, py, px + step, py + step);
                }
            }
        }
    }

    public static void quad(VertexConsumer consumer, Matrix4f matrix, float r, float g, float b, float a,
                            float x1, float y1, float x2, float y2) {
        consumer.vertex(matrix, x1, y1, 0.0F).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, x1, y2, 0.0F).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, x2, y2, 0.0F).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, x2, y1, 0.0F).color(r, g, b, a).endVertex();
    }

    private static int indexOf(String key) {
        if (key == null || key.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }
}