package com.example.examplemod.storage.client;

import com.example.examplemod.client.WaypointIcon;
import com.example.examplemod.storage.DeathStorageBlockEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Solo el haz vertical: el badge lo dibuja WaypointOverlay. */
public class DeathStorageRenderer implements BlockEntityRenderer<DeathStorageBlockEntity> {

    private static final double MARKER_Y = 2.30D;

    private static final class Shards extends RenderStateShard {
        private Shards() {
            super("deathloot_beam", () -> {
            }, () -> {
            });
        }

        private static final DepthTestStateShard NO_DEPTH = NO_DEPTH_TEST;
        private static final TransparencyStateShard TRANSLUCENT = TRANSLUCENT_TRANSPARENCY;
        private static final CullStateShard NO_CULLING = NO_CULL;
        private static final WriteMaskStateShard COLOR_ONLY = COLOR_WRITE;
    }

    private static final RenderType SEE_THROUGH = RenderType.create(
            "deathloot_beam",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1024,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(Shards.TRANSLUCENT)
                    .setDepthTestState(Shards.NO_DEPTH)
                    .setCullState(Shards.NO_CULLING)
                    .setWriteMaskState(Shards.COLOR_ONLY)
                    .createCompositeState(false));

    public DeathStorageRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DeathStorageBlockEntity storage, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!storage.isCheckpoint() || !storage.isActive()) {
            return;
        }

        int color = storage.getSkullColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float bob = Mth.sin((Util.getMillis() % 3400L) / 3400.0F * Mth.TWO_PI) * 0.05F;

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(SEE_THROUGH);
        Matrix4f matrix = pose.last().pose();

        float w = 0.020F;
        WaypointIcon.quad(consumer, matrix, r, g, b, 0.28F, 0.5F - w, 1.0F, 0.5F + w, (float) (MARKER_Y + bob));

        buffers.endBatch(SEE_THROUGH);
    }

    @Override
    public boolean shouldRenderOffScreen(DeathStorageBlockEntity storage) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 8192;
    }
}