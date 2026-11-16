package com.example.examplemod.storage.client;

import com.example.examplemod.storage.DeathStorageBlockEntity;
import com.example.examplemod.storage.DeathStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.example.examplemod.client.UiKit;

public class DeathStorageScreen extends AbstractContainerScreen<DeathStorageMenu> {

    private static final int W = 194;
    private static final int H = 240;
    private static final int EQUIP_X = 96;
    private static final int EQUIP_Y = 14;
    private static final int EQUIP_W = 82;
    private static final int EQUIP_H = 12;

    private static final int BG_TOP = 0xF00A0D12;
    private static final int BG_BOTTOM = 0xF0141A22;
    private static final int BORDER = 0x33FFFFFF;
    private static final int ACCENT = 0xFF4FA8FF;
    private static final int SLOT_BG = 0x66101419;
    private static final int SLOT_BORDER = 0x22FFFFFF;

    public DeathStorageScreen(DeathStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.inventoryLabelY = 999;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        vertical(graphics, x, y, W, H, BG_TOP, BG_BOTTOM);
        outline(graphics, x, y, W, H, BORDER);
        graphics.fill(x + 1, y + 1, x + W - 1, y + 26, 0x44000000);
        graphics.fill(x + 1, y + 26, x + W - 1, y + 27, ACCENT & 0x66FFFFFF | 0x40000000);

        slotGrid(graphics, x + DeathStorageMenu.GRID_X - 1, y + DeathStorageMenu.GRID_Y - 1, 9, 6);
        slotGrid(graphics, x + DeathStorageMenu.GRID_X - 1, y + 149, 9, 3);
        slotGrid(graphics, x + DeathStorageMenu.GRID_X - 1, y + 207, 9, 1);

        graphics.fill(x + 18, y + 143, x + W - 18, y + 144, 0x22FFFFFF);

        boolean hover = mouseX >= leftPos + EQUIP_X && mouseX < leftPos + EQUIP_X + EQUIP_W
        && mouseY >= topPos + EQUIP_Y && mouseY < topPos + EQUIP_Y + EQUIP_H;

graphics.fill(leftPos + EQUIP_X, topPos + EQUIP_Y,
        leftPos + EQUIP_X + EQUIP_W, topPos + EQUIP_Y + EQUIP_H,
        hover ? 0x554FA8FF : 0x33101820);
UiKit.outline(graphics, leftPos + EQUIP_X, topPos + EQUIP_Y, EQUIP_W, EQUIP_H,
        hover ? UiKit.ACCENT : UiKit.INNER);

String label = Component.translatable("deathlootforge.storage.equip").getString();
graphics.drawString(font, label,
        leftPos + EQUIP_X + (EQUIP_W - font.width(label)) / 2, topPos + EQUIP_Y + 2,
        hover ? 0xFFFFFFFF : UiKit.ACCENT, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        DeathStorageBlockEntity storage = menu.getStorage();
        int x = leftPos;
        int y = topPos;

        if (storage == null) {
            graphics.drawString(font, title, x + 10, y + 9, 0xFFFFFFFF, false);
            return;
        }

        graphics.drawString(font, storage.getStorageName(), x + 10, y + 8, 0xFFFFFFFF, false);

        long remaining = storage.getRemainingMillis();
        String timer = format(remaining);
        int color = remaining < 60_000L ? 0xFFFF6B6B : 0xFF8FE3FF;
        graphics.drawString(font, timer, x + W - 10 - font.width(timer), y + 8, color, false);

        String owner = storage.getOwnerName();
        if (!owner.isEmpty()) {
            graphics.drawString(font, owner, x + 10, y + 17, 0xFF7C93AC, false);
        }

        String status = storage.isOpened()
                ? Component.translatable("deathlootforge.storage.status.recovered").getString()
                : Component.translatable("deathlootforge.storage.status.pending").getString();

        graphics.drawString(font, status, x + W - 10 - font.width(status), y + 17,
                storage.isOpened() ? 0xFF6EDC8B : 0xFFFFC24B, false);

        graphics.drawString(font, Component.translatable("deathlootforge.storage.inventory"), x + 8, y + 137, 0xFF7C93AC, false);
        graphics.drawString(font, Component.translatable("deathlootforge.storage.yours"), x + 8, y + 195, 0xFF7C93AC, false);
    }

    private static String format(long millis) {
        long total = Math.max(0L, millis / 1000L);
        return String.format("%d:%02d", total / 60L, total % 60L);
    }

    private static void vertical(GuiGraphics graphics, int x, int y, int w, int h, int top, int bottom) {
        for (int row = 0; row < h; row++) {
            float t = row / (float) h;
            graphics.fill(x, y + row, x + w, y + row + 1, blend(top, bottom, t));
        }
    }

    private static void outline(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static void slotGrid(GuiGraphics graphics, int x, int y, int cols, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int sx = x + col * 18;
                int sy = y + row * 18;
                graphics.fill(sx, sy, sx + 18, sy + 18, SLOT_BG);
                outline(graphics, sx, sy, 18, 18, SLOT_BORDER);
            }
        }
    }

    private static int blend(int from, int to, float t) {
        int a = (int) (((from >>> 24) & 255) + ((((to >>> 24) & 255) - ((from >>> 24) & 255)) * t));
        int r = (int) (((from >> 16) & 255) + ((((to >> 16) & 255) - ((from >> 16) & 255)) * t));
        int g = (int) (((from >> 8) & 255) + ((((to >> 8) & 255) - ((from >> 8) & 255)) * t));
        int b = (int) ((from & 255) + (((to & 255) - (from & 255)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (mouseX >= leftPos + EQUIP_X && mouseX < leftPos + EQUIP_X + EQUIP_W
            && mouseY >= topPos + EQUIP_Y && mouseY < topPos + EQUIP_Y + EQUIP_H) {
        com.example.examplemod.network.DeathLootNetwork.toServer(
                new com.example.examplemod.network.packet.PacketEquipAll());
        return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
}
}