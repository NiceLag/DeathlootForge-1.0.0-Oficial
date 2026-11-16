package com.example.examplemod.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class FlatButton {

    public static final int OUTLINE = 0;
    public static final int FILLED = 1;
    public static final int SOLID = 2;
    public static final int DANGER = 3;

    public int x;
    public int y;
    public int width;
    public int height;
    public String label;
    public int accent = UiKit.ACCENT;
    public int style = OUTLINE;
    public boolean enabled = true;
    public boolean visible = true;
    public Runnable onClick;

    public FlatButton(String label, Runnable onClick) {
        this.label = label;
        this.onClick = onClick;
    }

    public FlatButton bounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public FlatButton style(int style) {
        this.style = style;
        return this;
    }

    public boolean hovered(double mouseX, double mouseY) {
        return visible && enabled && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void render(GuiGraphics graphics, Font font, double mouseX, double mouseY) {
        if (!visible) {
            return;
        }

        boolean hover = hovered(mouseX, mouseY);
        int bg;
        int border;
        int text;

        switch (style) {
            case FILLED -> {
                bg = enabled ? (hover ? 0xFF2EE06A : UiKit.GREEN) : 0xFF16241B;
                border = enabled ? 0xFF2EE06A : 0xFF20301F;
                text = enabled ? 0xFF06210F : 0xFF4F6B57;
            }
            case SOLID -> {
                bg = enabled ? (hover ? 0xFFF2F5F7 : 0xFFFFFFFF) : 0xFF3A3A3D;
                border = 0;
                text = enabled ? UiKit.BLACK_TEXT : 0xFF8A8A8E;
            }
            case DANGER -> {
                bg = enabled ? (hover ? 0x33FF5A5A : 0x22FF5A5A) : 0x22000000;
                border = 0x66FF5A5A;
                text = 0xFFFF7B7B;
            }
            default -> {
                bg = enabled ? (hover ? 0xFF17181B : 0xFF0E0F11) : 0xFF0B0C0E;
                border = enabled ? (hover ? 0xFF3C3E44 : UiKit.EDGE) : 0xFF1C1D20;
                text = enabled ? UiKit.WHITE : 0xFF5A5C62;
            }
        }

        graphics.fill(x, y, x + width, y + height, bg);
        if (border != 0) {
            UiKit.outline(graphics, x, y, width, height, border);
        }

        int textX = x + (width - font.width(label)) / 2;
        int textY = y + (height - 8) / 2 + 1;
        graphics.drawString(font, label, textX, textY, text, false);
    }

    public boolean click(double mouseX, double mouseY) {
        if (hovered(mouseX, mouseY)) {
            onClick.run();
            return true;
        }
        return false;
    }
}