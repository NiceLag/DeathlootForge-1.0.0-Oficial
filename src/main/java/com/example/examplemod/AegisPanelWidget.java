package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel de cargas de Aegis. Es un widget hijo del inventario, no un overlay:
 * vive en la lista de renderables de la pantalla, se dibuja en coordenadas
 * relativas a la GUI y se mueve con ella (incluido el libro de recetas).
 *
 * Zona elegida: debajo del slot de resultado y a la derecha de la cuadricula
 * de crafteo (que acaba en x=134), por encima del boton de Quark (y=79).
 */
public class AegisPanelWidget extends AbstractWidget {

    // Borde izquierdo a 2px de la cuadricula de crafteo.
    private static final int REL_X = 136;
    private static final int REL_Y = 49;

    private static final int PAD = 2;
    private static final int PANEL_H = 19;

    private final AbstractContainerScreen<?> screen;

    private float hover;
    private int cachedCharges = -1;
    private String count = "";
    private int countWidth;
    private List<Component> tooltip;

    public AegisPanelWidget(AbstractContainerScreen<?> screen) {
        super(0, 0, PAD * 2 + AegisRender.SHIELD_SMALL_W, PANEL_H, Component.empty());
        this.screen = screen;
        updatePosition();
    }

    /** Se re-ancla cada frame: sigue a la GUI aunque cambie de sitio. */
    private void updatePosition() {
        setX(screen.getGuiLeft() + REL_X);
        setY(screen.getGuiTop() + REL_Y);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        Font font = minecraft.font;
        int charges = ClientAegisData.getCharges();
        if (charges != cachedCharges) {
            refresh(charges, font);
        }
        updatePosition();

        boolean active = charges > 0;

        // Lerp con zona muerta: al llegar al destino deja de calcularse.
        float target = isHovered ? 1.0F : 0.0F;
        if (hover != target) {
            hover += (target - hover) * 0.25F;
            if (Math.abs(target - hover) < 0.01F) {
                hover = target;
            }
        }

        int x = getX();
        int y = getY();
        int width = getWidth();

        AegisRender.panel(graphics, x, y, width, PANEL_H,
                AegisRender.lerp(AegisRender.PANEL_BG, 0xA2121720, hover),
                AegisRender.PANEL_BORDER);

        AegisRender.shieldSmall(graphics, x + PAD, y + 3, active, 1.0F);

        if (countWidth > 0) {
            graphics.drawString(font, count, x + PAD + AegisRender.SHIELD_SMALL_W + PAD, y + 6,
                    AegisRender.lerp(0xFFD8E6F7, 0xFFFFFFFF, hover), true);
        }

        if (hover > 0.02F) {
            drawSweep(graphics, x, y, width);
        }

        if (isHovered) {
            if (tooltip == null) {
                tooltip = buildTooltip(charges, active);
            }
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    /** Solo se recalcula cuando cambia la cantidad: ni strings ni medidas por frame. */
    private void refresh(int charges, Font font) {
        cachedCharges = charges;
        count = charges > 0 ? Integer.toString(charges) : "";
        countWidth = count.isEmpty() ? 0 : font.width(count);
        setWidth(PAD * 2 + AegisRender.SHIELD_SMALL_W + (countWidth > 0 ? PAD + countWidth : 0));
        tooltip = null;
    }

    private static List<Component> buildTooltip(int charges, boolean active) {
        List<Component> list = new ArrayList<>(5);

        list.add(Component.translatable(active
                ? "deathlootforge.aegis.hud.on"
                : "deathlootforge.aegis.hud.off"
        ).withStyle(active ? ChatFormatting.AQUA : ChatFormatting.GRAY));

        if (active) {
            list.add(Component.translatable("deathlootforge.aegis.hud.amount", charges)
                    .withStyle(ChatFormatting.WHITE));
        }

        list.add(Component.translatable("deathlootforge.aegis.hud.desc1").withStyle(ChatFormatting.DARK_GRAY));
        list.add(Component.translatable("deathlootforge.aegis.hud.desc2").withStyle(ChatFormatting.DARK_GRAY));
        list.add(Component.translatable("deathlootforge.aegis.hud.desc3").withStyle(ChatFormatting.DARK_GRAY));

        return list;
    }

    /** Barrido de izquierda a derecha, en bucle. */
    private static void drawSweep(GuiGraphics graphics, int x, int y, int width) {
        int span = width + 32;
        int head = (int) ((Util.getMillis() / 7L) % span) - 16;

        for (int i = 0; i < 14; i++) {
            int sx = x + head - i;
            if (sx <= x + 1 || sx >= x + width - 2) {
                continue;
            }
            float al = i == 0 ? 1.0F : 0.7F * (1.0F - i / 14.0F);
            graphics.fill(sx, y + 1, sx + 1, y + 2,
                    AegisRender.alpha(i == 0 ? 0xFFFFFFFF : AegisRender.BLUE, al));
        }
    }

    /** Solo informativo: los clics pasan de largo hacia los slots. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}