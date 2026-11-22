package com.example.timestop;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class DecayItem extends Item {
    public DecayItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            BlockPos center = player.blockPosition();
            int radius = 5;

            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 3, radius))) {
                BlockState state = level.getBlockState(pos);
                Block block = state.getBlock();

                if (block == Blocks.GRASS_BLOCK) {
                    level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                } else if (block == Blocks.DIRT) {
                    level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
                } else if (block == Blocks.STONE) {
                    level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
                } else if (block == Blocks.COBBLESTONE) {
                    level.setBlockAndUpdate(pos, Blocks.GRAVEL.defaultBlockState());
                } else if (block == Blocks.GRAVEL) {
                    level.setBlockAndUpdate(pos, Blocks.SAND.defaultBlockState());
                } else if (block instanceof LeavesBlock) {
                    level.removeBlock(pos, false);
                }
            }

            AABB area = new AABB(center).inflate(radius);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player);
            for (LivingEntity target : entities) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2));
                target.hurt(level.damageSources().wither(), 6.0F);
            }

            level.playSound(null, center, SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8F, 0.6F);
            player.displayClientMessage(Component.literal("§8[Decay] เร่งความเสื่อมสลายรอบตัว!"), true);
            player.getCooldowns().addCooldown(this.getDefaultInstance(), 20);
        }
        return InteractionResult.SUCCESS;
    }
}
