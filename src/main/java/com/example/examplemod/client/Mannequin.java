package com.example.examplemod.client;

import com.example.examplemod.storage.DeathRecord;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Quaternionf;

import java.util.UUID;

public final class Mannequin {

    private static RemotePlayer fake;

    private Mannequin() {
    }

    private static RemotePlayer get() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }

        if (fake == null || fake.level() != minecraft.level) {
            fake = new RemotePlayer(minecraft.level,
                    new GameProfile(UUID.nameUUIDFromBytes("deathloot".getBytes()), "DeathLoot"));
            fake.setNoGravity(true);
            fake.setSilent(true);
        }

        return fake;
    }

    public static void render(GuiGraphics graphics, DeathRecord record, int x, int y, int scale,
                              int mouseX, int mouseY) {
        RemotePlayer entity = get();
        if (entity == null) {
            return;
        }

        entity.setItemSlot(EquipmentSlot.HEAD, record.armorPiece(3));
        entity.setItemSlot(EquipmentSlot.CHEST, record.armorPiece(2));
        entity.setItemSlot(EquipmentSlot.LEGS, record.armorPiece(1));
        entity.setItemSlot(EquipmentSlot.FEET, record.armorPiece(0));
        entity.setItemSlot(EquipmentSlot.MAINHAND, record.mainHand(record.selectedSlot));
        entity.setItemSlot(EquipmentSlot.OFFHAND, record.offhand);

        float offset = Mth.clamp((float) Math.toDegrees(Math.atan2(mouseX - x, 40.0D)), -70.0F, 70.0F);
        float pitch = Mth.clamp((float) Math.toDegrees(Math.atan2(mouseY - y, 40.0D)) * 0.6F, -25.0F, 25.0F);

        // La cabeza sigue la mira completa; el cuerpo solo la mitad y hacia el mismo lado.
        float yaw = 180.0F - offset;
        float body = 180.0F - offset * 0.45F;

        entity.yBodyRot = body;
        entity.yBodyRotO = body;
        entity.yHeadRot = yaw;
        entity.yHeadRotO = yaw;
        entity.setYRot(yaw);
        entity.yRotO = yaw;
        entity.setXRot(pitch);
        entity.xRotO = pitch;
        entity.tickCount++;

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.overrideCameraOrientation(new Quaternionf());
        dispatcher.setRenderShadow(false);

        RenderSystem.runAsFancy(() -> {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 50.0D);
            graphics.pose().scale(scale, scale, -scale);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
            dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F,
                    graphics.pose(), graphics.bufferSource(), 15728880);
            graphics.flush();
            graphics.pose().popPose();
        });

        dispatcher.setRenderShadow(true);
    }
}