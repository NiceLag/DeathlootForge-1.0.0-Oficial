package com.example.examplemod.client;

import com.example.examplemod.storage.DeathRecord;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

import java.util.ArrayList;

public class ServerHistoryScreen extends Screen {

    private static final int W = 400;
    private static final int H = 250;

    private final Screen parent;
    private int left;
    private int top;
    private int scroll;

    public ServerHistoryScreen(Screen parent) {
        super(Component.translatable("deathlootforge.server.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        UiKit.panel(graphics, left, top, W, H);
        graphics.fill(left + 1, top + 1, left + W - 1, top + 22, 0x55000000);
        graphics.drawString(font, title, left + 8, top + 7, UiKit.TEXT, false);
        UiKit.close(graphics, left + W - 16, top + 7, UiKit.DIM);

        String hint = Component.translatable("deathlootforge.server.hint").getString();
        graphics.drawString(font, hint, left + 8, top + 28, UiKit.FAINT, false);

        int y = top + 42;

        for (int i = scroll; i < Math.min(ClientDeathHistory.lost().size(), scroll + 8); i++) {
            ClientDeathHistory.Lost lost = ClientDeathHistory.lost().get(i);
            boolean hover = mouseX >= left + 8 && mouseX < left + W - 8 && mouseY >= y && mouseY < y + 20;

            graphics.fill(left + 8, y, left + W - 8, y + 20, hover ? 0x33FFFFFF : 0x22000000);
            UiKit.outline(graphics, left + 8, y, W - 16, 20, UiKit.INNER);

            graphics.drawString(font, lost.ownerName, left + 12, y + 3, UiKit.TEXT, false);
            graphics.drawString(font, ClientDeathHistory.clock(lost.time) + "  " + UiKit.dimension(lost.dimension)
                    + "  " + UiKit.coords(lost.pos), left + 12, y + 11, UiKit.DIM, false);

            String count = lost.items.size() + " items";
            graphics.drawString(font, count, left + W - 14 - font.width(count), y + 6, UiKit.WARN, false);

            y += 22;
        }

        if (ClientDeathHistory.lost().isEmpty()) {
            String empty = Component.translatable("deathlootforge.server.empty").getString();
            graphics.drawString(font, empty, left + W / 2 - font.width(empty) / 2, top + 100, UiKit.FAINT, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= left + W - 20 && mouseX < left + W - 8 && mouseY >= top + 4 && mouseY < top + 16) {
            onClose();
            return true;
        }

        int y = top + 42;
        for (int i = scroll; i < Math.min(ClientDeathHistory.lost().size(), scroll + 8); i++) {
            if (mouseX >= left + 8 && mouseX < left + W - 8 && mouseY >= y && mouseY < y + 20) {
                minecraft.setScreen(new DeathDetailsScreen(toRecord(ClientDeathHistory.lost().get(i), i + 1), this));
                return true;
            }
            y += 22;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Convierte la entrada de historial en un registro revisable, con sus mochilas. */
private DeathRecord toRecord(ClientDeathHistory.Lost lost, int index) {
    DeathRecord record = new DeathRecord();
    record.ownerName = lost.ownerName;
    record.dimension = lost.dimension;
    record.pos = lost.pos;
    record.timestamp = lost.time;
    record.storageNameIndex = index;
    record.inventory = new ArrayList<>(lost.items);

    collectBackpacks(record);

    return record;
}

private void collectBackpacks(DeathRecord record) {
    if (!ModList.get().isLoaded("sophisticatedbackpacks")) {
        return;
    }

    for (int slot = 0; slot < record.inventory.size(); slot++) {
        ItemStack stack = record.inventory.get(slot);

        if (stack.isEmpty() || !(stack.getItem() instanceof BackpackItem)) {
            continue;
        }

        DeathRecord.Backpack backpack = new DeathRecord.Backpack();
        backpack.slot = slot;
        backpack.curiosSlot = false;
        backpack.icon = stack.copy();

        try {
            IItemHandler handler = new BackpackWrapper(stack).getInventoryHandler();
            for (int i = 0; i < handler.getSlots(); i++) {
                backpack.contents.add(handler.getStackInSlot(i).copy());
            }
        } catch (Throwable ignored) {
        }

        record.backpacks.add(backpack);
    }
}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(Math.max(0, ClientDeathHistory.lost().size() - 8),
                scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}