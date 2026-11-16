package com.example.examplemod.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Busca un hueco seguro en espiral: sin lava, fuego, liquido ni void.
 * Si muere por debajo del mundo (End), busca suelo desde arriba.
 */
public final class SafePlacement {

    private static final int MAX_RADIUS = 12;
    private static final int[] VERTICAL = {0, 1, -1, 2, -2, 3, -3};

    private SafePlacement() {
    }

    @Nullable
    public static BlockPos find(ServerLevel level, BlockPos origin) {
        int minY = level.getMinBuildHeight() + 1;

        if (origin.getY() < minY) {
            BlockPos ground = findVoidGround(level, origin);
            if (ground != null) {
                return ground;
            }
            return find(level, new BlockPos(origin.getX(), Math.max(minY + 16, 64), origin.getZ()));
        }

        for (int radius = 0; radius <= MAX_RADIUS; radius++) {
            for (BlockPos ring : ring(origin, radius)) {
                for (int dy : VERTICAL) {
                    BlockPos candidate = ring.offset(0, dy, 0);

                    if (candidate.getY() < minY || candidate.getY() >= level.getMaxBuildHeight() - 2) {
                        continue;
                    }

                    if (isSafe(level, candidate)) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private static BlockPos findVoidGround(ServerLevel level, BlockPos origin) {
        int minY = level.getMinBuildHeight() + 1;

        for (int radius = 0; radius <= MAX_RADIUS; radius++) {
            for (BlockPos ring : ring(origin, radius)) {
                for (int y = Math.min(level.getMaxBuildHeight() - 2, 120); y > minY; y--) {
                    BlockPos candidate = new BlockPos(ring.getX(), y, ring.getZ());
                    if (isSafe(level, candidate)) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!state.isAir() && !state.canBeReplaced()) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.COBWEB) || state.is(BlockTags.CAMPFIRES)) {
            return false;
        }

        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);

        if (!below.getFluidState().isEmpty()) {
            return false;
        }
        if (below.is(Blocks.MAGMA_BLOCK) || below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE)) {
            return false;
        }

        return below.isFaceSturdy(level, belowPos, Direction.UP);
    }

    private static Iterable<BlockPos> ring(BlockPos center, int radius) {
        if (radius == 0) {
            return java.util.List.of(center);
        }

        java.util.List<BlockPos> out = new java.util.ArrayList<>(radius * 8);

        for (int dx = -radius; dx <= radius; dx++) {
            out.add(center.offset(dx, 0, -radius));
            out.add(center.offset(dx, 0, radius));
        }
        for (int dz = -radius + 1; dz <= radius - 1; dz++) {
            out.add(center.offset(-radius, 0, dz));
            out.add(center.offset(radius, 0, dz));
        }

        return out;
    }

    
    /**
     * Prepara el destino de un teletransporte: si el suelo no es firme o hay lava,
     * genera una plataforma de 3x3 de obsidiana y despeja los dos bloques de arriba.
     * Devuelve la posicion donde debe aparecer el jugador.
     */
    public static BlockPos prepareLanding(ServerLevel level, BlockPos storage) {
        BlockPos floor = storage.below();
        BlockState below = level.getBlockState(floor);

        boolean firm = below.isFaceSturdy(level, floor, Direction.UP)
                && below.getFluidState().isEmpty()
                && !below.is(Blocks.MAGMA_BLOCK);

        if (!firm) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlockAndUpdate(
                            floor.offset(dx, 0, dz),
                            Blocks.OBSIDIAN.defaultBlockState()
                    );
                }
            }
        }

        clear(level, storage.above());
        clear(level, storage.above(2));

        return storage.above();
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!state.getFluidState().isEmpty()
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.COBWEB)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

}