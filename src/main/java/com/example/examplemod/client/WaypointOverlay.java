package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Waypoints en espacio de pantalla. La punta del pin cae sobre la cara
 * superior del almacen; el nombre va encima y la distancia debajo.
 * Si el almacen queda detras del jugador no se dibuja.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WaypointOverlay {

    private static final int PIN_W = 18;
    private static final int PIN_H = 26;
    private static final int MARGIN = 26;
    private static final double AIM = 30.0D;

    private static final int[][] OUTLINE = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1}};

    private static final class Label {
        private final ClientDeathHistory.Active active;
        private final int x;
        private final int y;
        private final double distance;
        private final boolean offscreen;
        private final boolean aimed;

        private Label(ClientDeathHistory.Active active, int x, int y, double distance,
                      boolean offscreen, boolean aimed) {
            this.active = active;
            this.x = x;
            this.y = y;
            this.distance = distance;
            this.offscreen = offscreen;
            this.aimed = aimed;
        }
    }

    private WaypointOverlay() {
    }

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        if (ClientDeathHistory.hideAllWaypoints()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;

        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        double centerX = width / 2.0D;
        double centerY = height / 2.0D;

        Camera camera = minecraft.gameRenderer.getMainCamera();
        double eyeX = camera.getPosition().x;
        double eyeY = camera.getPosition().y;
        double eyeZ = camera.getPosition().z;

        double yaw = camera.getYRot() * Mth.DEG_TO_RAD;
        double pitch = camera.getXRot() * Mth.DEG_TO_RAD;

        // Base de camara: adelante, derecha y arriba.
        double forwardX = -Math.sin(yaw) * Math.cos(pitch);
        double forwardY = -Math.sin(pitch);
        double forwardZ = Math.cos(yaw) * Math.cos(pitch);

        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);

        double upX = Math.sin(yaw) * forwardY;
        double upY = -Math.sin(yaw) * forwardX + Math.cos(yaw) * forwardZ;
        double upZ = -Math.cos(yaw) * forwardY;

        double fov = Math.toRadians(minecraft.options.fov().get());
        double tanY = Math.tan(fov / 2.0D);
        double tanX = tanY * (width / (double) Math.max(1, height));

        String dimension = minecraft.level.dimension().location().toString();

        List<Label> labels = new ArrayList<>();

        for (ClientDeathHistory.Active active : ClientDeathHistory.active()) {
            if (active.hidden || !dimension.equals(active.dimension)) {
                continue;
            }
            if (ClientDeathHistory.remainingFor(active.recordId) <= 0L) {
                continue;
            }

            double dx = active.pos.getX() + 0.5D - eyeX;
            double dy = active.pos.getY() + 1.0D - eyeY;
            double dz = active.pos.getZ() + 0.5D - eyeZ;

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 0.05D) {
                continue;
            }

            double depth = dx * forwardX + dy * forwardY + dz * forwardZ;

            // Detras de la camara: fuera.
            if (depth <= 0.08D) {
                continue;
            }

            double sideX = dx * rightX + dz * rightZ;
            double sideY = dx * upX + dy * upY + dz * upZ;

            double screenX = centerX + sideX / depth / tanX * (width / 2.0D);
            double screenY = centerY - sideY / depth / tanY * (height / 2.0D);

            double clampedX = Mth.clamp(screenX, MARGIN, width - MARGIN);
            double clampedY = Mth.clamp(screenY, MARGIN + PIN_H + 16, height - MARGIN);

            boolean offscreen = clampedX != screenX || clampedY != screenY;

            int x = (int) Math.round(clampedX);
            int y = (int) Math.round(clampedY);

            boolean aimed = !offscreen && Math.abs(x - centerX) < AIM && Math.abs(y - centerY) < AIM;

            labels.add(new Label(active, x, y, distance, offscreen, aimed));
        }

        if (labels.isEmpty()) {
            return;
        }

        labels.sort(Comparator.comparingDouble((Label label) -> label.distance).reversed());

        for (Label label : labels) {
            int color = label.active.color & 0xFFFFFF;
            String initial = label.active.initial == null ? "" : label.active.initial.trim();

            int badgeColor = initial.isEmpty()
                    ? 0xFFFFFFFF
                    : (luminance(color) > 0.55D ? 0xFF10141A : 0xFFFFFFFF);

            marker(graphics, font, label.x, label.y, color, badge(label.active), badgeColor);

            String distance = distance(label.distance);
            int distanceWidth = font.width(distance);
            outlined(graphics, font, distance, label.x - distanceWidth / 2, label.y + 3, 0xFFFFFFFF);

            if (label.aimed) {
                String text = name(label.active) + " \u00B7 " + distance;
                int textWidth = font.width(text);
                int plateX = label.x - textWidth / 2 - 5;
                int plateY = label.y - PIN_H - 18;

                graphics.fill(plateX, plateY, plateX + textWidth + 10, plateY + 14, 0xCC05070A);
                graphics.fill(plateX, plateY, plateX + textWidth + 10, plateY + 1, 0xFF000000 | color);
                graphics.fill(plateX, plateY + 13, plateX + textWidth + 10, plateY + 14, 0xFF000000 | color);
                graphics.fill(plateX, plateY, plateX + 1, plateY + 14, 0xFF000000 | color);
                graphics.fill(plateX + textWidth + 9, plateY, plateX + textWidth + 10, plateY + 14, 0xFF000000 | color);

                outlined(graphics, font, text, label.x - textWidth / 2, plateY + 3, 0xFFFFFFFF);
            }
        }
    }

    private static void marker(GuiGraphics graphics, Font font, int tipX, int tipY, int color,
                               String badge, int badgeColor) {
        int top = tipY - PIN_H;

        for (int[] offset : OUTLINE) {
            pin(graphics, tipX + offset[0], top + offset[1], 0xFF000000);
        }

        pin(graphics, tipX, top, 0xFF000000 | color);

        int textWidth = font.width(badge);
        outlined(graphics, font, badge, tipX - textWidth / 2, top + 5, badgeColor);
    }

    private static void pin(GuiGraphics graphics, int cx, int top, int color) {
        for (int row = 0; row < PIN_H; row++) {
            int half = half(row);
            if (half <= 0) {
                continue;
            }
            graphics.fill(cx - half, top + row, cx + half + 1, top + row + 1, color);
        }
    }

    private static int half(int row) {
        if (row <= PIN_W) {
            float radius = PIN_W / 2.0F;
            float dy = row - radius;
            float squared = radius * radius - dy * dy;
            return squared <= 0.0F ? 0 : (int) Math.sqrt(squared);
        }

        float progress = (row - PIN_W) / (float) (PIN_H - PIN_W);
        return (int) ((PIN_W / 2.0F) * (1.0F - progress));
    }

    private static void outlined(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        for (int[] offset : OUTLINE) {
            graphics.drawString(font, text, x + offset[0], y + offset[1], 0xFF000000, false);
        }
        graphics.drawString(font, text, x, y, color, false);
    }

    private static double luminance(int color) {
        double red = ((color >> 16) & 0xFF) / 255.0D;
        double green = ((color >> 8) & 0xFF) / 255.0D;
        double blue = (color & 0xFF) / 255.0D;
        return 0.299D * red + 0.587D * green + 0.114D * blue;
    }

    private static String badge(ClientDeathHistory.Active active) {
        String initial = active.initial == null ? "" : active.initial.trim();
        if (!initial.isEmpty()) {
            return initial.length() > 4 ? initial.substring(0, 4) : initial;
        }
        return Integer.toString(active.nameIndex);
    }

    private static String name(ClientDeathHistory.Active active) {
        String value = active.name == null ? "" : active.name.trim();
        return value.isEmpty() ? Integer.toString(active.nameIndex) : value;
    }

    private static String distance(double blocks) {
        if (blocks < 1000.0D) {
            return (int) Math.round(blocks) + " m";
        }
        return String.format("%.1f km", blocks / 1000.0D);
    }
}