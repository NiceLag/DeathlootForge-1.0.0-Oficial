package com.example.examplemod.client;

import com.example.examplemod.network.DeathLootNetwork;
import com.example.examplemod.network.packet.PacketAction;
import com.example.examplemod.network.packet.PacketDetailsRequest;
import com.example.examplemod.storage.DeathRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeathHistoryScreen extends Screen {

    private static final int W = 420;
    private static final int H = 296;

    private static final int LIST_X = 14;
    private static final int LIST_W = W - 28;
    private static final int LIST_Y = 46;
    private static final int ROW_H = 40;
    private static final int VISIBLE = 5;

    private static final int[] MINUTES = {5, 10, 30, 60, 300, 600};
    private static final int[] PRICES = {1, 2, 4, 6, 25, 40};

    private static final int[] COLORS = {
            0xEF4444, 0xF97316, 0xF59E0B, 0xEAB308, 0x84CC16, 0x22C55E,
            0x10B981, 0x14B8A6, 0x06B6D4, 0x0EA5E9, 0x3B82F6, 0x6366F1,
            0x8B5CF6, 0xA855F7, 0xD946EF, 0xEC4899, 0xF9A8D4, 0xFFFFFF,
            0xCBD5E1, 0x94A3B8, 0x64748B, 0x1F2937, 0x92400E, 0x000000};

    private static final int NONE = 0;
    private static final int DETAIL = 1;
    private static final int EDIT = 2;
    private static final int EXTEND = 3;
    private static final int TELEPORT = 4;
    private static final int CONFIRM = 5;

    private static final int MODAL_W = 330;
    private static final int EDIT_W = 380;
    private static final int EDIT_H = 320;
    private static final int PALETTE_Y = 132;
    private static final int PALETTE_STEP = 22;

    private int left;
    private int top;
    private int modalLeft;
    private int modalTop;
    private int modalHeight = 296;
    private int modalWidth = MODAL_W;

    private final List<FlatButton> buttons = new ArrayList<>();

    private EditBox nameField;
    private EditBox initialField;
    private EditBox hexField;

    private int state = NONE;
    private UUID selected;
    private int listScroll;
    private int selectedMinutes = 5;
    private boolean buying;
    private int editColor = 0x8FE3FF;

    private long noticeUntil;
    private boolean noticeSuccess;
    private String notice = "";

    public DeathHistoryScreen() {
        super(Component.translatable("deathlootforge.history.title"));
    }

    private String t(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;

        if (selected == null && !ClientDeathHistory.records().isEmpty()) {
            selected = ClientDeathHistory.records().get(0).recordId;
        }

        nameField = field(24, null);
        initialField = field(4, value -> value.matches("[A-Za-z0-9]*"));

        hexField = field(7, value -> value.matches("#?[0-9a-fA-F]{0,6}"));
        hexField.setResponder(value -> {
            if (value.length() >= 6) {
                editColor = UiKit.parseHex(value, editColor);
            }
        });

        buildButtons();
        DeathLootNetwork.requestHistory(false);
    }

    private EditBox field(int maxLength, java.util.function.Predicate<String> filter) {
        EditBox box = new EditBox(font, 0, 0, 1, 12, Component.empty());
        box.setBordered(false);
        box.setTextColor(0xFFFFFFFF);
        box.setMaxLength(maxLength);
        box.setVisible(false);
        if (filter != null) {
            box.setFilter(filter);
        }
        addRenderableWidget(box);
        return box;
    }

    private DeathRecord selectedRecord() {
        for (DeathRecord record : ClientDeathHistory.records()) {
            if (record.recordId.equals(selected)) {
                return record;
            }
        }
        return null;
    }

    private boolean isActive(DeathRecord record) {
        return ClientDeathHistory.activeFor(record.recordId) != null
                && ClientDeathHistory.remainingFor(record.recordId) > 0L;
    }

    private long price(int minutes) {
        for (int i = 0; i < MINUTES.length; i++) {
            if (MINUTES[i] == minutes) {
                return PRICES[i];
            }
        }
        return 1L;
    }

    private long extendCost() {
        DeathRecord record = selectedRecord();
        boolean free = (selectedMinutes == 5 || selectedMinutes == 10)
                && record != null && record.freeExtensions < 2;
        return free ? 0L : price(selectedMinutes);
    }

    private void showFields(boolean visible) {
        nameField.setVisible(visible);
        initialField.setVisible(visible);
        hexField.setVisible(visible);

        if (!visible) {
            setFocused(null);
        }
    }

    private void openModal(int next, int height) {
    state = next;
    modalWidth = next == EDIT ? EDIT_W : MODAL_W;
    modalHeight = height;
    buying = false;
    showFields(next == EDIT);

        if (next == EDIT) {
            DeathRecord record = selectedRecord();
            if (record != null) {
                nameField.setValue(record.label);
                initialField.setValue(record.initial);
                editColor = record.skullColor;
                hexField.setValue(String.format("#%06X", record.skullColor & 0xFFFFFF));
            }
        }

        buildButtons();

        if (next == EDIT) {
            setFocused(nameField);
        }
    }

    private void closeModal() {
        state = NONE;
        showFields(false);
        buildButtons();
    }

    private void saveEdit(DeathRecord record) {
        String name = nameField.getValue().trim();
        String initial = initialField.getValue().trim();

        if (!name.isEmpty()) {
            DeathLootNetwork.action(PacketAction.RENAME, record.recordId, name, 0);
            record.label = name;
        }

        if (!initial.equals(record.initial)) {
            DeathLootNetwork.action(PacketAction.INITIAL, record.recordId, initial, 0);
            record.initial = initial;
            ClientDeathHistory.Active active = ClientDeathHistory.activeFor(record.recordId);
            if (active != null) {
                active.initial = initial;
            }
        }

        if ((record.skullColor & 0xFFFFFF) != (editColor & 0xFFFFFF)) {
            DeathLootNetwork.action(PacketAction.COLOR, record.recordId, "", editColor);
            record.skullColor = editColor;
            ClientDeathHistory.Active active = ClientDeathHistory.activeFor(record.recordId);
            if (active != null) {
                active.color = editColor;
            }
        }

        openModal(DETAIL, 296);
    }

    private void toggleHidden(DeathRecord record) {
        DeathLootNetwork.action(PacketAction.HIDE, record.recordId, "", record.hidden ? 0 : 1);
        ClientDeathHistory.toggleHidden(record.recordId);
        notice(true, t(record.hidden ? "deathlootforge.result.hidden" : "deathlootforge.result.shown"));
    }

    private void share(DeathRecord record) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        minecraft.player.connection.sendChat(t("deathlootforge.edit.share.text",
                record.displayName(), record.pos.getX(), record.pos.getY(), record.pos.getZ()));
        notice(true, t("deathlootforge.edit.shared"));
    }

    private void buildButtons() {
        buttons.clear();

        if (state == NONE) {
            buttons.add(new FlatButton("X", this::onClose).bounds(left + W - 24, top + 8, 16, 16));

            buttons.add(new FlatButton(t(ClientDeathHistory.hideAllWaypoints()
                    ? "deathlootforge.history.showall" : "deathlootforge.history.hideall"),
                    ClientDeathHistory::toggleHideAll)
                    .style(FlatButton.OUTLINE)
                    .bounds(left + 14, top + H - 20, 118, 14));

            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2)) {
                buttons.add(new FlatButton(t("deathlootforge.history.server"), () -> {
                    DeathLootNetwork.requestHistory(true);
                    minecraft.setScreen(new ServerHistoryScreen(this));
                }).style(FlatButton.OUTLINE).bounds(left + W - 158, top + H - 20, 144, 14));
            }
            return;
        }

        modalLeft = Math.max(4, (width - modalWidth) / 2);
        modalTop = Math.max(4, (height - modalHeight) / 2);

        buttons.add(new FlatButton("X", this::closeModal).bounds(modalLeft + modalWidth - 20, modalTop + 8, 12, 12));

        DeathRecord record = selectedRecord();
        if (record == null) {
            state = NONE;
            return;
        }

        switch (state) {
            case DETAIL -> {
                buttons.add(new FlatButton(t("deathlootforge.detail.see"), () ->
                        DeathLootNetwork.toServer(new PacketDetailsRequest(record.recordId)))
                        .style(FlatButton.OUTLINE)
                        .bounds(modalLeft + 12, modalTop + 164, MODAL_W - 24, 18));

                buttons.add(new FlatButton(t("deathlootforge.detail.extend"), () -> {
                    selectedMinutes = 5;
                    openModal(EXTEND, 318);
                }).style(FlatButton.FILLED)
                        .bounds(modalLeft + 12, modalTop + 204, MODAL_W - 24, 20));

                buttons.add(new FlatButton(t("deathlootforge.detail.tp"), () -> openModal(TELEPORT, 318))
                        .style(FlatButton.OUTLINE)
                        .bounds(modalLeft + 12, modalTop + 230, MODAL_W - 24, 20));

                buttons.add(new FlatButton(t("deathlootforge.detail.edit"), () -> openModal(EDIT, EDIT_H))
                        .style(FlatButton.OUTLINE)
                        .bounds(modalLeft + MODAL_W - 132, modalTop + modalHeight - 22, 46, 16));

                buttons.add(new FlatButton(t(record.hidden
                        ? "deathlootforge.detail.show" : "deathlootforge.detail.hide"),
                        () -> toggleHidden(record))
                        .style(FlatButton.OUTLINE)
                        .bounds(modalLeft + MODAL_W - 80, modalTop + modalHeight - 22, 44, 16));

                buttons.add(new FlatButton(t("deathlootforge.detail.delete"), () -> openModal(CONFIRM, 116))
                        .style(FlatButton.DANGER)
                        .bounds(modalLeft + MODAL_W - 30, modalTop + modalHeight - 22, 20, 16));
            }
            
case EDIT -> {
    buttons.add(new FlatButton(t("deathlootforge.edit.random"), () -> {
        editColor = COLORS[(int) (Math.random() * COLORS.length)];
        hexField.setValue(String.format("#%06X", editColor));
    }).style(FlatButton.OUTLINE).bounds(modalLeft + 286, modalTop + 88, 70, 18));

    buttons.add(new FlatButton(t("deathlootforge.edit.share"), () -> share(record))
            .style(FlatButton.OUTLINE)
            .bounds(modalLeft + modalWidth - 130, modalTop + 200, 106, 16));

    buttons.add(new FlatButton(t("deathlootforge.edit.save"), () -> saveEdit(record))
            .style(FlatButton.SOLID)
            .bounds(modalLeft + 12, modalTop + modalHeight - 42, modalWidth - 24, 18));

    buttons.add(new FlatButton(t("deathlootforge.edit.cancel"), () -> openModal(DETAIL, 296))
            .style(FlatButton.OUTLINE)
            .bounds(modalLeft + 12, modalTop + modalHeight - 22, modalWidth - 24, 18));
}

            case EXTEND -> {
                if (!buying) {
                    FlatButton buy = new FlatButton(t("deathlootforge.extend.buy"), () -> {
                        buying = true;
                        buildButtons();
                    }).style(FlatButton.FILLED);
                    buy.bounds(modalLeft + 12, modalTop + modalHeight - 34, MODAL_W - 24, 20);
                    buy.enabled = isActive(record);
                    buttons.add(buy);
                } else {
                    long cost = extendCost();

                    FlatButton confirm = new FlatButton(t("deathlootforge.confirm.buy"), () -> {
                        DeathLootNetwork.action(PacketAction.EXTEND, record.recordId, "", selectedMinutes);
                        buying = false;
                        buildButtons();
                    }).style(FlatButton.FILLED);
                    confirm.bounds(modalLeft + 12, modalTop + modalHeight - 34, MODAL_W / 2 - 18, 20);
                    confirm.enabled = isActive(record) && ClientDeathHistory.credits() >= cost;
                    buttons.add(confirm);

                    buttons.add(new FlatButton(t("deathlootforge.confirm.cancel"), () -> {
                        buying = false;
                        buildButtons();
                    }).style(FlatButton.OUTLINE)
                            .bounds(modalLeft + MODAL_W / 2 + 6, modalTop + modalHeight - 34, MODAL_W / 2 - 18, 20));
                }
            }
            case TELEPORT -> {
                long cost = ClientDeathHistory.teleportPrice();

                FlatButton free = new FlatButton(
                        t("deathlootforge.tp.free", ClientDeathHistory.freeTeleports()),
                        () -> DeathLootNetwork.action(PacketAction.TELEPORT, record.recordId, "", 0))
                        .style(FlatButton.FILLED);
                free.bounds(modalLeft + 12, modalTop + modalHeight - 56, MODAL_W - 24, 20);
                free.enabled = ClientDeathHistory.freeTeleports() > 0 && isActive(record);
                buttons.add(free);

                FlatButton pay = new FlatButton(t("deathlootforge.tp.paybutton"), () ->
                        DeathLootNetwork.action(PacketAction.TELEPORT, record.recordId, "", 0))
                        .style(FlatButton.OUTLINE);
                pay.bounds(modalLeft + 12, modalTop + modalHeight - 32, MODAL_W - 24, 20);
                pay.enabled = ClientDeathHistory.freeTeleports() <= 0
                        && ClientDeathHistory.credits() >= cost && isActive(record);
                buttons.add(pay);
            }
            case CONFIRM -> {
                buttons.add(new FlatButton(t("deathlootforge.confirm.yes"), () -> {
                    DeathLootNetwork.action(PacketAction.DELETE, record.recordId, "", 0);
                    selected = null;
                    closeModal();
                }).style(FlatButton.DANGER)
                        .bounds(modalLeft + MODAL_W / 2 - 90, modalTop + modalHeight - 36, 84, 18));

                buttons.add(new FlatButton(t("deathlootforge.confirm.no"), () -> openModal(DETAIL, 296))
                        .style(FlatButton.OUTLINE)
                        .bounds(modalLeft + MODAL_W / 2 + 6, modalTop + modalHeight - 36, 84, 18));
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        UiKit.panel(graphics, left, top, W, H);
        graphics.fill(left + 1, top + 1, left + W - 1, top + 28, 0x55000000);
        graphics.fill(left + 1, top + 28, left + W - 1, top + 29, 0x33FFFFFF);
        graphics.drawString(font, title, left + 14, top + 9, UiKit.TEXT, false);

        String credits = ClientDeathHistory.creditsAvailable()
                ? t("deathlootforge.history.credits", ClientDeathHistory.creditsText())
                : t("deathlootforge.history.credits.none");
        graphics.drawString(font, credits, left + W - 36 - font.width(credits), top + 9, UiKit.WARN, false);

        renderList(graphics, mouseX, mouseY);

        if (state == NONE) {
            for (FlatButton button : buttons) {
                button.render(graphics, font, mouseX, mouseY);
            }
            graphics.drawString(font, t("deathlootforge.history.close"), left + 14, top + H + 6, UiKit.FAINT, false);
            renderNotice(graphics, left + W / 2, top + H + 18);
            return;
        }

        graphics.fill(left, top, left + W, top + H, 0xC0000000);
        renderModal(graphics, mouseX, mouseY);

        for (FlatButton button : buttons) {
            button.render(graphics, font, mouseX, mouseY);
        }

        if (state == EDIT) {
            nameField.render(graphics, mouseX, mouseY, partialTick);
            initialField.render(graphics, mouseX, mouseY, partialTick);
            hexField.render(graphics, mouseX, mouseY, partialTick);
        }

        renderNotice(graphics, modalLeft + MODAL_W / 2, modalTop + modalHeight - 78);
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        List<DeathRecord> records = ClientDeathHistory.records();

        graphics.drawString(font, t("deathlootforge.history.recent"), left + LIST_X, top + 36, UiKit.DIM, false);

        int y = top + LIST_Y;

        for (int i = listScroll; i < Math.min(records.size(), listScroll + VISIBLE); i++) {
            row(graphics, records.get(i), left + LIST_X, y, LIST_W, mouseX, mouseY);
            y += ROW_H;
        }

        if (records.isEmpty()) {
            graphics.drawString(font, t("deathlootforge.history.empty"), left + LIST_X + 4, top + LIST_Y + 8,
                    UiKit.FAINT, false);
        }

        if (records.size() > VISIBLE) {
            int height = VISIBLE * ROW_H - 6;
            int knob = (int) ((height - 16) * (listScroll / (float) Math.max(1, records.size() - VISIBLE)));
            graphics.fill(left + LIST_X + LIST_W + 4, top + LIST_Y, left + LIST_X + LIST_W + 6,
                    top + LIST_Y + height, 0x22FFFFFF);
            graphics.fill(left + LIST_X + LIST_W + 3, top + LIST_Y + knob, left + LIST_X + LIST_W + 7,
                    top + LIST_Y + knob + 16, 0x99FFFFFF);
        }
    }

    private void row(GuiGraphics graphics, DeathRecord record, int x, int y, int width, int mouseX, int mouseY) {
        boolean chosen = record.recordId.equals(selected);
        boolean hover = inside(mouseX, mouseY, x, y, width, ROW_H - 6);
        boolean active = isActive(record);
        boolean dim = record.hidden;

        UiKit.round(graphics, x, y, width, ROW_H - 6,
                chosen ? 0xFF14181D : hover ? 0xFF121316 : 0xFF0B0C0E,
                chosen ? UiKit.ACCENT : UiKit.EDGE);

        UiKit.badge(graphics, font, x + 8, y + 4, record.badgeText(), record.skullColor, dim);

        String name = record.displayName();
        graphics.drawString(font, font.plainSubstrByWidth(name, width - 118), x + 27, y + 5,
                dim ? UiKit.DIM : UiKit.WHITE, false);

        String pill = active ? t("deathlootforge.state.active") : t("deathlootforge.state.inactive");
        int pillX = x + width - font.width(pill) - 14;
        UiKit.pill(graphics, font, pillX, y + 5, pill, active ? UiKit.GREEN : UiKit.DIM);

        graphics.drawString(font, UiKit.date(record.timestamp) + "   " + UiKit.hour(record.timestamp),
                x + 27, y + 17, UiKit.DIM, false);

        String remaining = active ? UiKit.timer(ClientDeathHistory.remainingFor(record.recordId)) : "--:--:--";
        int timerX = x + width - font.width(remaining) - 14;
        graphics.drawString(font, remaining, timerX, y + 18, active ? UiKit.GREEN : UiKit.FAINT, false);

        String clock = t("deathlootforge.history.left");
        graphics.drawString(font, clock, x + width - font.width(clock) - 14, y + 28, UiKit.FAINT, false);
    }

    private void renderModal(GuiGraphics graphics, int mouseX, int mouseY) {
        DeathRecord record = selectedRecord();

        UiKit.round(graphics, modalLeft, modalTop, modalWidth, modalHeight, UiKit.CARD, UiKit.EDGE);
        graphics.fill(modalLeft + 1, modalTop + 26, modalLeft + modalWidth - 1, modalTop + 27, UiKit.EDGE);

        String header = switch (state) {
            case EDIT -> t("deathlootforge.edit.title");
            case EXTEND -> t("deathlootforge.extend.title.time", minutes(selectedMinutes));
            case TELEPORT -> record == null ? "" : t("deathlootforge.tp.title.dest", record.displayName());
            case CONFIRM -> t("deathlootforge.confirm.title");
            default -> record == null ? "" : t("deathlootforge.detail.title", record.storageNameIndex);
        };
        graphics.drawString(font, font.plainSubstrByWidth(header, modalWidth - 46),
        modalLeft + 12, modalTop + 9, UiKit.WHITE, false);
        if (record == null) {
            return;
        }

        switch (state) {
            case DETAIL -> renderDetail(graphics, record);
            case EDIT -> renderEdit(graphics, record, mouseX, mouseY);
            case EXTEND -> renderExtend(graphics, record, mouseX, mouseY);
            case TELEPORT -> renderTeleport(graphics, record);
            case CONFIRM -> renderConfirm(graphics, record);
            default -> {
            }
        }
    }

    private void renderDetail(GuiGraphics graphics, DeathRecord record) {
        boolean active = isActive(record);

        graphics.drawString(font, t("deathlootforge.detail.field"), modalLeft + 12, modalTop + 36, UiKit.DIM, false);

        String pill = active ? t("deathlootforge.state.active") : t("deathlootforge.state.inactive");
        UiKit.pill(graphics, font, modalLeft + MODAL_W - 12 - font.width(pill) - 10, modalTop + 34,
                pill, active ? UiKit.GREEN : UiKit.DIM);

        UiKit.badge(graphics, font, modalLeft + 12, modalTop + 50, record.badgeText(), record.skullColor,
                record.hidden);
        graphics.drawString(font, record.displayName(), modalLeft + 32, modalTop + 53, UiKit.WHITE, false);

        graphics.drawString(font, t("deathlootforge.detail.date"), modalLeft + 12, modalTop + 72, UiKit.DIM, false);
        graphics.drawString(font, UiKit.date(record.timestamp), modalLeft + 12, modalTop + 82, UiKit.WHITE, false);

        graphics.drawString(font, t("deathlootforge.detail.hour"), modalLeft + 148, modalTop + 72, UiKit.DIM, false);
        graphics.drawString(font, UiKit.hour(record.timestamp), modalLeft + 148, modalTop + 82, UiKit.WHITE, false);

        UiKit.round(graphics, modalLeft + 12, modalTop + 98, MODAL_W - 24, 54, 0xFF141518, 0);
        graphics.drawString(font, t("deathlootforge.detail.remaining"), modalLeft + 20, modalTop + 103, UiKit.DIM, false);

        String timer = UiKit.timer(ClientDeathHistory.remainingFor(record.recordId));
        graphics.pose().pushPose();
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.drawString(font, timer, (modalLeft + 20) / 2, (modalTop + 113) / 2,
                active ? UiKit.GREEN : UiKit.DIM, false);
        graphics.pose().popPose();

        graphics.drawString(font, t("deathlootforge.detail.available"), modalLeft + 20, modalTop + 136, UiKit.DIM, false);

        graphics.drawString(font, t("deathlootforge.detail.manage"), modalLeft + 12, modalTop + 190, UiKit.WHITE, false);
    }

    
private void renderEdit(GuiGraphics graphics, DeathRecord record, int mouseX, int mouseY) {
    graphics.drawString(font, t("deathlootforge.edit.name"), modalLeft + 12, modalTop + 34, UiKit.WHITE, false);
    UiKit.round(graphics, modalLeft + 12, modalTop + 46, modalWidth - 24, 18, 0xFF0E0F11, UiKit.EDGE);
    place(nameField, modalLeft + 17, modalTop + 51, modalWidth - 34);

    graphics.drawString(font, t("deathlootforge.edit.initial"), modalLeft + 12, modalTop + 76, UiKit.WHITE, false);
    graphics.drawString(font, t("deathlootforge.edit.hex"), modalLeft + 160, modalTop + 76, UiKit.WHITE, false);

    UiKit.round(graphics, modalLeft + 12, modalTop + 88, 64, 18, 0xFF0E0F11, UiKit.EDGE);
    place(initialField, modalLeft + 17, modalTop + 93, 54);

    UiKit.round(graphics, modalLeft + 160, modalTop + 88, 96, 18, 0xFF0E0F11, UiKit.EDGE);
    place(hexField, modalLeft + 165, modalTop + 93, 86);

    graphics.fill(modalLeft + 262, modalTop + 88, modalLeft + 280, modalTop + 106, 0xFF000000 | editColor);
    UiKit.outline(graphics, modalLeft + 262, modalTop + 88, 18, 18, UiKit.EDGE);

    String hint = font.plainSubstrByWidth(t("deathlootforge.edit.initial.hint"), modalWidth - 24);
    graphics.drawString(font, hint, modalLeft + 12, modalTop + 112, UiKit.DIM, false);

    for (int i = 0; i < COLORS.length; i++) {
        int cx = modalLeft + 20 + (i % 12) * 28;
        int cy = modalTop + PALETTE_Y + (i / 12) * 26;
        boolean chosen = (COLORS[i] & 0xFFFFFF) == (editColor & 0xFFFFFF);
        boolean hover = inside(mouseX, mouseY, cx - 9, cy - 9, 18, 18);

        graphics.fill(cx - 8, cy - 8, cx + 8, cy + 8, 0xFF000000 | COLORS[i]);
        if (chosen || hover) {
            UiKit.outline(graphics, cx - 10, cy - 10, 20, 20, chosen ? UiKit.GREEN : UiKit.WHITE);
        }
    }

    graphics.drawString(font, t("deathlootforge.edit.preview"), modalLeft + 12, modalTop + 176, UiKit.WHITE, false);
    UiKit.round(graphics, modalLeft + 12, modalTop + 188, modalWidth - 24, 46, 0xFF16181B, UiKit.EDGE);

    graphics.fill(modalLeft + 22, modalTop + 200, modalLeft + 40, modalTop + 218, 0xFF000000 | editColor);
    String previewBadge = initialField.getValue().isEmpty() ? record.badgeText() : initialField.getValue();
    graphics.drawString(font, previewBadge, modalLeft + 31 - font.width(previewBadge) / 2, modalTop + 204,
            0xFF101014, false);

    String preview = nameField.getValue().isEmpty() ? record.displayName() : nameField.getValue();
    graphics.drawString(font, font.plainSubstrByWidth(preview, 210), modalLeft + 48, modalTop + 204,
            UiKit.WHITE, false);

    String sub = t("deathlootforge.edit.preview.hint");
    graphics.drawString(font, font.plainSubstrByWidth(sub, 210), modalLeft + 48, modalTop + 216,
            UiKit.DIM, false);
}


    private void place(EditBox box, int x, int y, int width) {
        box.setX(x);
        box.setY(y);
        box.setWidth(width);
    }

    private void renderExtend(GuiGraphics graphics, DeathRecord record, int mouseX, int mouseY) {
        int lineY = modalTop + 32;

        for (var line : font.split(Component.translatable("deathlootforge.extend.subtitle"), MODAL_W - 24)) {
            graphics.drawString(font, line, modalLeft + 12, lineY, UiKit.DIM, false);
            lineY += 10;
        }

        UiKit.round(graphics, modalLeft + 12, modalTop + 54, MODAL_W - 24, 44, 0xFF141518, 0);
        graphics.drawString(font, t("deathlootforge.extend.current"), modalLeft + 20, modalTop + 59, UiKit.DIM, false);

        String timer = UiKit.timer(ClientDeathHistory.remainingFor(record.recordId));
        graphics.pose().pushPose();
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.drawString(font, timer, (modalLeft + 20) / 2, (modalTop + 69) / 2,
                isActive(record) ? UiKit.GREEN : UiKit.DIM, false);
        graphics.pose().popPose();

        graphics.drawString(font, t("deathlootforge.extend.pick"), modalLeft + 12, modalTop + 106, UiKit.WHITE, false);

        for (int i = 0; i < MINUTES.length; i++) {
            int x = modalLeft + 12 + (i % 2) * 160;
            int y = modalTop + 120 + (i / 2) * 36;
            boolean chosen = MINUTES[i] == selectedMinutes;
            boolean hover = inside(mouseX, mouseY, x, y, 152, 30);

            UiKit.round(graphics, x, y, 152, 30, chosen ? 0xFF10251A : 0xFF0E0F11,
                    chosen ? UiKit.GREEN : hover ? 0xFF3C3E44 : UiKit.EDGE);

            UiKit.clock(graphics, x + 76, y + 10, chosen ? UiKit.GREEN : UiKit.DIM);

            String label = minutes(MINUTES[i]);
            graphics.drawString(font, label, x + 76 - font.width(label) / 2, y + 19, UiKit.WHITE, false);
        }

        UiKit.round(graphics, modalLeft + 12, modalTop + 230, MODAL_W - 24, 40, 0xFF141518, 0);

        graphics.drawString(font, t("deathlootforge.extend.picked"), modalLeft + 20, modalTop + 235, UiKit.DIM, false);
        String picked = minutes(selectedMinutes);
        graphics.drawString(font, picked, modalLeft + MODAL_W - 20 - font.width(picked), modalTop + 235, UiKit.WHITE, false);

        graphics.drawString(font, t("deathlootforge.extend.cost"), modalLeft + 20, modalTop + 251, UiKit.DIM, false);
        long cost = extendCost();
        String costLabel = cost <= 0L ? t("deathlootforge.extend.free") : t("deathlootforge.history.cost", cost);
        graphics.drawString(font, costLabel, modalLeft + MODAL_W - 20 - font.width(costLabel), modalTop + 251,
                cost <= 0L ? UiKit.GREEN : UiKit.WARN, false);

        String wallet = t("deathlootforge.extend.wallet", ClientDeathHistory.creditsText());
        graphics.drawString(font, wallet, modalLeft + 20, modalTop + 262, UiKit.FAINT, false);

        String hint = t(buying ? "deathlootforge.extend.hint.confirm" : "deathlootforge.extend.hint");
        graphics.drawString(font, hint, modalLeft + MODAL_W / 2 - font.width(hint) / 2, modalTop + modalHeight - 10,
                UiKit.FAINT, false);
    }

    private void renderTeleport(GuiGraphics graphics, DeathRecord record) {
        graphics.drawString(font, t("deathlootforge.tp.subtitle"), modalLeft + 12, modalTop + 32, UiKit.DIM, false);
        graphics.drawString(font, t("deathlootforge.tp.destination"), modalLeft + 12, modalTop + 46, UiKit.WHITE, false);

        UiKit.round(graphics, modalLeft + 12, modalTop + 58, MODAL_W - 24, 62, 0xFF141518, UiKit.EDGE);

        UiKit.badge(graphics, font, modalLeft + 20, modalTop + 65, record.badgeText(), record.skullColor, false);
        graphics.drawString(font, record.displayName(), modalLeft + 40, modalTop + 67, UiKit.WHITE, false);

        String state = isActive(record) ? t("deathlootforge.tp.active") : t("deathlootforge.state.inactive");
        graphics.drawString(font, state, modalLeft + 40, modalTop + 79, isActive(record) ? UiKit.GREEN : UiKit.DIM, false);

        graphics.drawString(font, t("deathlootforge.tp.coords"), modalLeft + 20, modalTop + 92, UiKit.DIM, false);
        graphics.drawString(font, UiKit.coordsPretty(record.pos), modalLeft + 20, modalTop + 104, UiKit.WHITE, false);
        UiKit.copy(graphics, modalLeft + MODAL_W - 34, modalTop + 100, UiKit.FAINT);

        graphics.drawString(font, t("deathlootforge.tp.info"), modalLeft + 12, modalTop + 132, UiKit.WHITE, false);

        UiKit.round(graphics, modalLeft + 12, modalTop + 144, MODAL_W - 24, 70, 0xFF0E0F11, UiKit.EDGE);

        graphics.drawString(font, t("deathlootforge.tp.remaining"), modalLeft + 20, modalTop + 151, UiKit.DIM, false);
        String timer = UiKit.timer(ClientDeathHistory.remainingFor(record.recordId));
        graphics.drawString(font, timer, modalLeft + MODAL_W - 20 - font.width(timer), modalTop + 151, UiKit.GREEN, false);

        graphics.drawString(font, t("deathlootforge.tp.freeleft"), modalLeft + 20, modalTop + 166, UiKit.DIM, false);
        String left = t("deathlootforge.tp.freecount", ClientDeathHistory.freeTeleports());
        UiKit.pill(graphics, font, modalLeft + MODAL_W - 20 - font.width(left) - 10, modalTop + 164, left,
                ClientDeathHistory.freeTeleports() > 0 ? UiKit.GREEN : UiKit.DIM);

        graphics.drawString(font, t("deathlootforge.tp.cost"), modalLeft + 20, modalTop + 181, UiKit.DIM, false);
        long cost = ClientDeathHistory.teleportPrice();
        String costLabel = cost <= 0L ? t("deathlootforge.tp.freecost") : t("deathlootforge.history.cost", cost);
        graphics.drawString(font, costLabel, modalLeft + MODAL_W - 20 - font.width(costLabel), modalTop + 181,
                cost <= 0L ? UiKit.GREEN : UiKit.WARN, false);

        graphics.drawString(font, t("deathlootforge.tp.wallet"), modalLeft + 20, modalTop + 196, UiKit.DIM, false);
        String wallet = ClientDeathHistory.creditsAvailable()
                ? ClientDeathHistory.creditsText() : t("deathlootforge.tp.wallet.none");
        graphics.drawString(font, wallet, modalLeft + MODAL_W - 20 - font.width(wallet), modalTop + 196,
                UiKit.WARN, false);

        String hint = t("deathlootforge.tp.hint");
        graphics.drawString(font, hint, modalLeft + MODAL_W / 2 - font.width(hint) / 2, modalTop + modalHeight - 10,
                UiKit.FAINT, false);
    }

    private void renderConfirm(GuiGraphics graphics, DeathRecord record) {
        int y = modalTop + 36;

        for (var line : font.split(Component.translatable("deathlootforge.confirm.body", record.displayName()),
                MODAL_W - 28)) {
            graphics.drawString(font, line, modalLeft + 14, y, UiKit.DIM, false);
            y += 10;
        }
    }

    private void renderNotice(GuiGraphics graphics, int centerX, int y) {
        if (System.currentTimeMillis() > noticeUntil || notice.isEmpty()) {
            return;
        }

        int width = font.width(notice) + 20;
        int x = centerX - width / 2;

        UiKit.round(graphics, x, y, width, 18, noticeSuccess ? 0xF010251A : 0xF0251010,
                noticeSuccess ? UiKit.GREEN : UiKit.BAD);
        graphics.drawString(font, notice, x + 10, y + 5, noticeSuccess ? UiKit.GREEN : 0xFFFF8A8A, false);
    }

    private String minutes(int value) {
        return value >= 60 ? t("deathlootforge.extend.hours", value / 60) : t("deathlootforge.extend.minutes", value);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        for (FlatButton widget : new ArrayList<>(buttons)) {
            if (widget.click(mouseX, mouseY)) {
                buildButtons();
                return true;
            }
        }

        if (state == NONE) {
            if (clickList(mouseX, mouseY)) {
                buildButtons();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (clickModal(mouseX, mouseY)) {
            buildButtons();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickList(double mouseX, double mouseY) {
        List<DeathRecord> records = ClientDeathHistory.records();
        int y = top + LIST_Y;

        for (int i = listScroll; i < Math.min(records.size(), listScroll + VISIBLE); i++) {
            if (inside(mouseX, mouseY, left + LIST_X, y, LIST_W, ROW_H - 6)) {
                selected = records.get(i).recordId;
                openModal(DETAIL, 296);
                return true;
            }
            y += ROW_H;
        }

        return false;
    }

    private boolean clickModal(double mouseX, double mouseY) {
        DeathRecord record = selectedRecord();
        if (record == null) {
            return false;
        }

        switch (state) {
            
case EDIT -> {
    if (inside(mouseX, mouseY, modalLeft + 12, modalTop + 46, modalWidth - 24, 18)) {
        setFocused(nameField);
        return true;
    }
    if (inside(mouseX, mouseY, modalLeft + 12, modalTop + 88, 64, 18)) {
        setFocused(initialField);
        return true;
    }
    if (inside(mouseX, mouseY, modalLeft + 160, modalTop + 88, 96, 18)) {
        setFocused(hexField);
        return true;
    }

    for (int i = 0; i < COLORS.length; i++) {
        int cx = modalLeft + 20 + (i % 12) * 28;
        int cy = modalTop + PALETTE_Y + (i / 12) * 26;
        if (inside(mouseX, mouseY, cx - 10, cy - 10, 20, 20)) {
            editColor = COLORS[i];
            hexField.setValue(String.format("#%06X", editColor));
            return true;
        }
    }

    setFocused(null);
}

            case EXTEND -> {
                for (int i = 0; i < MINUTES.length; i++) {
                    int x = modalLeft + 12 + (i % 2) * 160;
                    int y = modalTop + 120 + (i / 2) * 36;
                    if (inside(mouseX, mouseY, x, y, 152, 30)) {
                        selectedMinutes = MINUTES[i];
                        buying = false;
                        return true;
                    }
                }
            }
            case TELEPORT -> {
                if (inside(mouseX, mouseY, modalLeft + MODAL_W - 38, modalTop + 96, 16, 16)) {
                    minecraft.keyboardHandler.setClipboard(
                            record.pos.getX() + " " + record.pos.getY() + " " + record.pos.getZ());
                    notice(true, t("deathlootforge.detail.copied"));
                    return true;
                }
            }
            default -> {
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (state == NONE) {
            listScroll = Math.max(0, Math.min(
                    Math.max(0, ClientDeathHistory.records().size() - VISIBLE),
                    listScroll - (int) Math.signum(delta)));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        GuiEventListener focused = getFocused();
        if (focused instanceof EditBox box && box.isVisible() && box.canConsumeInput()
                && box.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == 256) {
    if (state == NONE) {
        onClose();
    } else if (state == DETAIL) {
        closeModal();
    } else {
        openModal(DETAIL, 296);
    }
    return true;
}

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        GuiEventListener focused = getFocused();
        if (focused instanceof EditBox box && box.isVisible() && box.canConsumeInput()) {
            return box.charTyped(character, modifiers);
        }
        return super.charTyped(character, modifiers);
    }

    public void notice(boolean success, String message) {
        noticeSuccess = success;
        notice = message;
        noticeUntil = System.currentTimeMillis() + 3500L;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}