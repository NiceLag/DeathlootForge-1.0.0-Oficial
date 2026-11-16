package com.example.examplemod.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Rueda de tono/saturacion con deslizador de brillo. */
public class ColorPicker {

    public static final int SIZE = 132;
    private static final int RADIUS = 44;

    public int x;
    public int y;
    public boolean visible;
    private int rgb = 0x8FE3FF;
    private float hue = 0.55F;
    private float saturation = 0.45F;
    private float value = 1.0F;
    private int dragging = -1;
    private Runnable onChange;

    public void open(int x, int y, int rgb, Runnable onChange) {
        this.x = x;
        this.y = y;
        this.onChange = onChange;
        set(rgb);
        this.visible = true;
    }

    public void close() {
        this.visible = false;
        this.dragging = -1;
    }

    public void set(int rgb) {
        this.rgb = rgb & 0xFFFFFF;

        float r = ((rgb >> 16) & 255) / 255.0F;
        float g = ((rgb >> 8) & 255) / 255.0F;
        float b = (rgb & 255) / 255.0F;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        this.value = max;

        if (max <= 0.0F) {
            this.saturation = 0.0F;
            return;
        }

        this.saturation = delta / max;

        if (delta <= 0.0F) {
            return;
        }

        if (max == r) {
            this.hue = ((g - b) / delta % 6.0F + 6.0F) % 6.0F / 6.0F;
        } else if (max == g) {
            this.hue = ((b - r) / delta + 2.0F) / 6.0F;
        } else {
            this.hue = ((r - g) / delta + 4.0F) / 6.0F;
        }
    }

        public int fieldX() {
        return x + 34;
    }

    public int fieldY() {
        return y + SIZE - 22;
    }

    public int fieldWidth() {
        return SIZE - 42;
    }

    public int rgb() {
        return rgb;
    }

    private int centerX() {
        return x + SIZE / 2;
    }

    private int centerY() {
        return y + 56;
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }

        UiKit.panel(graphics, x, y, SIZE, SIZE);
        graphics.drawString(font, "RGB", x + 8, y + 6, UiKit.DIM, false);

        int cx = centerX();
        int cy = centerY();

        for (int py = -RADIUS; py <= RADIUS; py++) {
            for (int px = -RADIUS; px <= RADIUS; px++) {
                if (px * px + py * py > RADIUS * RADIUS) {
                    continue;
                }

                float distance = Mth.sqrt((float) (px * px + py * py));
                float h = ((float) (Math.toDegrees(Math.atan2(py, px)) + 360.0D) % 360.0F) / 360.0F;
                int color = Mth.hsvToRgb(h, Math.min(1.0F, distance / RADIUS), value);

                graphics.fill(cx + px, cy + py, cx + px + 1, cy + py + 1, 0xFF000000 | color);
            }
        }

        graphics.fill(cx - RADIUS, cy + RADIUS + 6, cx + RADIUS, cy + RADIUS + 14, 0x66000000);
        graphics.fill(cx - RADIUS, cy + RADIUS + 6,
                cx - RADIUS + (int) ((RADIUS * 2) * value), cy + RADIUS + 14, 0xAAFFFFFF);

        double angle = hue * Math.PI * 2.0D;
        int knobX = cx + (int) (Math.cos(angle) * saturation * RADIUS);
        int knobY = cy + (int) (Math.sin(angle) * saturation * RADIUS);
        graphics.fill(knobX - 2, knobY - 2, knobX + 3, knobY + 3, 0xFF000000);
        graphics.fill(knobX - 1, knobY - 1, knobX + 2, knobY + 2, 0xFFFFFFFF);

        graphics.fill(x + 8, y + SIZE - 22, x + 30, y + SIZE - 8, 0xFF000000 | rgb);
        UiKit.outline(graphics, x + 8, y + SIZE - 22, 22, 14, UiKit.INNER);
    }

    public boolean mouseDown(double mouseX, double mouseY) {
        if (!visible) {
            return false;
        }

        int cx = centerX();
        int cy = centerY();
        double dx = mouseX - cx;
        double dy = mouseY - cy;

        if (dx * dx + dy * dy <= RADIUS * RADIUS) {
            dragging = 0;
            drag(mouseX, mouseY);
            return true;
        }

        if (mouseY >= cy + RADIUS + 4 && mouseY < cy + RADIUS + 16
                && Math.abs(dx) <= RADIUS) {
            dragging = 1;
            drag(mouseX, mouseY);
            return true;
        }

        return mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
    }

    public void drag(double mouseX, double mouseY) {
        int cx = centerX();
        int cy = centerY();

        if (dragging == 0) {
            double dx = mouseX - cx;
            double dy = mouseY - cy;
            double distance = Math.sqrt(dx * dx + dy * dy);

            hue = (float) (((Math.toDegrees(Math.atan2(dy, dx)) + 360.0D) % 360.0D) / 360.0D);
            saturation = (float) Math.min(1.0D, distance / RADIUS);
        } else if (dragging == 1) {
            value = (float) Math.max(0.0D, Math.min(1.0D, (mouseX - (cx - RADIUS)) / (RADIUS * 2.0D)));
        } else {
            return;
        }

        rgb = Mth.hsvToRgb(hue, saturation, Math.max(0.08F, value));
        apply();
    }

    private void apply() {
        if (onChange != null) {
            onChange.run();
        }
    }

    public boolean dragging() {
        return dragging >= 0;
    }

    public void release() {
        if (dragging >= 0) {
            dragging = -1;
            apply();
        }
    }
}