package com.example.examplemod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Objeto comestible. Se come con la animacion nativa y al terminar
 * anade exactamente una carga de proteccion (validado en el servidor).
 */
public class AegisItem extends Item {

    public AegisItem(Properties properties) {
        super(properties);
    }

    /** 1 segundo de animacion de comer (mas agil que el 1.6s por defecto). */
    @Override
    public int getUseDuration(ItemStack stack) {
        return 20;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            int total = AegisData.addCharges(player, 1);

            AegisNetwork.sendSync(player, total);
            AegisNetwork.sendNotice(player, AegisNetwork.NOTICE_GAINED, total);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.85F, 1.6F);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        18, 0.45D, 0.6D, 0.45D, 0.04D);
            }
        }

        // Delega en vanilla: animacion, sonido de masticar, particulas y descuento del stack.
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.deathlootforge.aegis.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.deathlootforge.aegis.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.deathlootforge.aegis.desc3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.deathlootforge.aegis.stackable").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}