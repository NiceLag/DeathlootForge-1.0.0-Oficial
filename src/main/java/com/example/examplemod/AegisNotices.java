package com.example.examplemod;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Avisos de dos lineas con divisor. Se dibujan al final del HUD y bien
 * arriba de la hotbar, con fondo suficientemente opaco: nada los tapa.
 */
public final class AegisNotices {

    private static final List<Notice> ACTIVE = new ArrayList<>(4);

    private static final long LIFETIME = 3400L;
    private static final long FADE_IN = 140L;
    private static final long FADE_OUT = 600L;
    private static final int PANEL_H = 36;
    private static final int ICON = 16;
    private static final int MAX_VISIBLE = 3;

    private AegisNotices() {
    }

    public static boolean hasNotices() {
        return !ACTIVE.isEmpty();
    }

    private static final class Notice {
        final Component line1;
        final Component line2;
        final int width;
        final int type;
        final long start;

        Notice(Component line1, Component line2, int width, int type, long start) {
            this.line1 = line1;
            this.line2 = line2;
            this.width = width;
            this.type = type;
            this.start = start;
        }
    }

    public static void show(int type, int value) {
        boolean isProtected = type == AegisNetwork.NOTICE_PROTECTED;

        Component line1 = Component.translatable(isProtected
                ? "deathlootforge.aegis.notice.protected.1"
                : "deathlootforge.aegis.notice.gained.1");
        Component line2 = Component.translatable(isProtected
                ? "deathlootforge.aegis.notice.protected.2"
                : "deathlootforge.aegis.notice.gained.2", value);

        Font font = Minecraft.getInstance().font;
        int width = Math.max(font.width(line1), font.width(line2));

        ACTIVE.add(new Notice(line1, line2, width, type, Util.getMillis()));

        while (ACTIVE.size() > MAX_VISIBLE) {
            ACTIVE.remove(0);
        }
    }

    public static void render(GuiGraphics graphics, Font font, int screenWidth, int screenHeight) {
        if (ACTIVE.isEmpty()) {
            return;
        }

        long now = Util.getMillis();

        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            if (now - ACTIVE.get(i).start > LIFETIME) {
                ACTIVE.remove(i);
            }
        }
        if (ACTIVE.isEmpty()) {
            return;
        }

        // Arriba, centrado. El mas nuevo queda arriba del todo.
        int y = 35;

        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Notice notice = ACTIVE.get(i);
            drawNotice(graphics, font, screenWidth, y, notice, alphaOf(now - notice.start));
            y += PANEL_H + 3;
        }
    }

    private static float alphaOf(long age) {
        if (age < FADE_IN) {
            return age / (float) FADE_IN;
        }
        long remaining = LIFETIME - age;
        if (remaining < FADE_OUT) {
            return Math.max(0.0F, remaining / (float) FADE_OUT);
        }
        return 1.0F;
    }

    private static void drawNotice(GuiGraphics graphics, Font font, int screenWidth, int y, Notice notice, float a) {
        boolean isProtected = notice.type == AegisNetwork.NOTICE_PROTECTED;
        int accent = isProtected ? AegisRender.AMBER : AegisRender.BLUE;

        int width = 6 + ICON + 7 + notice.width + 8;
        int x = (screenWidth - width) / 2;

        AegisRender.panel(graphics, x, y, width, PANEL_H,
                AegisRender.alpha(0x990E1218, a),
                AegisRender.alpha(0x2EFFFFFF, a));

        // Barra de acento
        graphics.fill(x + 1, y + 1, x + 3, y + PANEL_H - 1, AegisRender.alpha(accent, a * 0.9F));

        // Panel del icono
        int iconX = x + 6;
        int iconY = y + (PANEL_H - ICON) / 2;
        AegisRender.panel(graphics, iconX, iconY, ICON, ICON,
                AegisRender.alpha(0x1CFFFFFF, a),
                AegisRender.alpha(accent, a * 0.5F));

        if (isProtected) {
            AegisRender.warning(graphics, iconX + 3, iconY + 3,
                    AegisRender.alpha(accent, a), AegisRender.alpha(0xFF14181E, a));
        } else {
            AegisRender.plus(graphics, iconX + 3, iconY + 3, AegisRender.alpha(accent, a));
        }

        int textX = iconX + ICON + 7;

        graphics.drawString(font, notice.line1, textX, y + 7,
                AegisRender.alpha(AegisRender.TEXT, a), true);

        // Divisor
        graphics.fill(textX, y + 19, x + width - 7, y + 20, AegisRender.alpha(0x22FFFFFF, a));

        graphics.drawString(font, notice.line2, textX, y + 22,
                AegisRender.alpha(AegisRender.TEXT_DIM, a), true);
    }
}