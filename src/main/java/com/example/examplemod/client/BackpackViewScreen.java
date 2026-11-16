package com.example.examplemod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Abre la interfaz ORIGINAL de Sophisticated Backpacks sobre la copia guardada
 * de la mochila.
 *
 * Se reutilizan BackpackScreen + BackpackContainer con un BackpackContext
 * propio que sirve el wrapper de la instantanea, asi se respetan tamano,
 * rejilla, objetos y mejoras. El contenido queda en solo lectura.
 */
public class BackpackViewScreen extends Screen {

    private final ItemStack backpack;
    private final Screen parent;

    private Screen inner;

    private BackpackViewScreen(ItemStack backpack, Screen parent) {
        super(backpack.getHoverName());
        this.backpack = backpack;
        this.parent = parent;
    }

    public static Screen open(ItemStack backpack, Screen parent) {
        return new BackpackViewScreen(backpack.copy(), parent);
    }

    @Override
    protected void init() {
        inner = build();

        if (inner != null) {
            inner.init(minecraft, width, height);
            lock();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (inner != null) {
            inner.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        renderFallback(graphics, mouseX, mouseY);
    }

    private Screen build() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || !ModList.get().isLoaded("sophisticatedbackpacks")) {
            return null;
        }

        try {
            IBackpackWrapper wrapper = backpack
                    .getCapability(CapabilityBackpackWrapper.getCapabilityInstance())
                    .orElseGet(() -> new BackpackWrapper(backpack));

            BackpackContext context = new SnapshotContext(wrapper);
            BackpackContainer container = new BackpackContainer(0, minecraft.player, context);

            return BackpackScreen.constructScreen(container, minecraft.player.getInventory(),
                    backpack.getHoverName());
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Contexto minimo que sirve el wrapper de la copia guardada. */
    private static final class SnapshotContext extends BackpackContext {

        private final IBackpackWrapper wrapper;

        private SnapshotContext(IBackpackWrapper wrapper) {
            this.wrapper = wrapper;
        }

        @Override
        public Optional<IStorageWrapper> getParentBackpackWrapper(Player player) {
            return Optional.empty();
        }

        @Override
        public boolean shouldLockBackpackSlot(Player player) {
            return false;
        }

        @Override
        public IBackpackWrapper getBackpackWrapper(Player player) {
            return wrapper;
        }

        @Override
        public int getBackpackSlotIndex() {
            return -1;
        }

        @Override
        public BackpackContext getSubBackpackContext(int subBackpackSlotIndex, boolean saveAfterOpen) {
            return this;
        }

        @Override
        public BackpackContext getParentBackpackContext() {
            return this;
        }

        @Override
        public ContextType getType() {
            return ContextType.ITEM_BACKPACK;
        }

        @Override
        public boolean canInteractWith(Player player) {
            return true;
        }

        @Override
        public void addToBuffer(FriendlyByteBuf buffer) {
        }

        @Override
        public void onUpgradeChanged(Player player) {
        }
    }

    /** Deja el contenido inmutable; las pestañas de mejoras siguen funcionando. */
    private void lock() {
        try {
            AbstractContainerMenu menu = ((AbstractContainerScreen<?>) inner).getMenu();
            List<Slot> slots = menu.slots;

            for (int i = 0; i < slots.size(); i++) {
                Slot slot = slots.get(i);

                if (slot.getClass().getName().contains("Upgrade")) {
                    continue;
                }

                slots.set(i, new ReadOnlySlot(slot));
            }
        } catch (Throwable ignored) {
        }
    }

    private static final class ReadOnlySlot extends Slot {

        private final Slot source;

        private ReadOnlySlot(Slot source) {
            super(source.container, source.getContainerSlot(), source.x, source.y);
            this.source = source;
        }

        @Override
        public ItemStack getItem() {
            return source.getItem();
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean isActive() {
            return source.isActive();
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return source.getNoItemIcon();
        }
    }

    // ---------------------------------------------------------------- respaldo

    private void renderFallback(GuiGraphics graphics, int mouseX, int mouseY) {
        renderBackground(graphics);

        List<ItemStack> contents = new ArrayList<>();
        List<ItemStack> upgrades = new ArrayList<>();

        try {
            BackpackWrapper wrapper = new BackpackWrapper(backpack);
            IItemHandler handler = wrapper.getInventoryHandler();
            for (int i = 0; i < handler.getSlots(); i++) {
                contents.add(handler.getStackInSlot(i));
            }
            IItemHandler upgradeHandler = wrapper.getUpgradeHandler();
            for (int i = 0; i < upgradeHandler.getSlots(); i++) {
                upgrades.add(upgradeHandler.getStackInSlot(i));
            }
        } catch (Throwable ignored) {
        }

        int rows = Math.max(1, (int) Math.ceil(contents.size() / 9.0D));
        int boxW = 9 * 18 + 16;
        int boxH = rows * 18 + 34 + (upgrades.isEmpty() ? 0 : 24);
        int left = (width - boxW) / 2;
        int top = (height - boxH) / 2;

        UiKit.round(graphics, left, top, boxW, boxH, UiKit.CARD, UiKit.EDGE);
        graphics.drawString(font, title, left + 8, top + 6, UiKit.WHITE, false);
        UiKit.close(graphics, left + boxW - 16, top + 6, UiKit.DIM);

        for (int i = 0; i < contents.size(); i++) {
            int x = left + 8 + (i % 9) * 18;
            int y = top + 22 + (i / 9) * 18;

            UiKit.slot(graphics, x, y);

            ItemStack stack = contents.get(i);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x + 1, y + 1);
                graphics.renderItemDecorations(font, stack, x + 1, y + 1);

                if (inside(mouseX, mouseY, x, y, 18, 18)) {
                    graphics.renderTooltip(font, stack, mouseX, mouseY);
                }
            }
        }

        if (!upgrades.isEmpty()) {
            int y = top + boxH - 20;

            for (int i = 0; i < upgrades.size() && i < 9; i++) {
                int x = left + 8 + i * 18;
                UiKit.slot(graphics, x, y);

                if (!upgrades.get(i).isEmpty()) {
                    graphics.renderItem(upgrades.get(i), x + 1, y + 1);

                    if (inside(mouseX, mouseY, x, y, 18, 18)) {
                        graphics.renderTooltip(font, upgrades.get(i), mouseX, mouseY);
                    }
                }
            }
        }
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return inner != null ? inner.mouseClicked(mouseX, mouseY, button) : super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return inner != null ? inner.mouseReleased(mouseX, mouseY, button) : super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return inner != null
                ? inner.mouseDragged(mouseX, mouseY, button, dragX, dragY)
                : super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return inner != null ? inner.mouseScrolled(mouseX, mouseY, delta) : super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return inner != null ? inner.keyPressed(keyCode, scanCode, modifiers) : super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return inner != null ? inner.charTyped(character, modifiers) : super.charTyped(character, modifiers);
    }

    @Override
    public void tick() {
        if (inner != null) {
            inner.tick();
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        if (inner != null) {
            inner.resize(minecraft, width, height);
        }
        super.resize(minecraft, width, height);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}