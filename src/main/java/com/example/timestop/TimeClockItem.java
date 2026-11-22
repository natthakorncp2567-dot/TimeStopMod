package com.example.timestop;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class TimeClockItem extends Item {
    private final boolean isCopperVersion;
    private static int copperTimeRemaining = 0;

    private static final String[] MODES = {
            "Stop Time (หยุดเวลา)",
            "Slow Time (สโลว์โมชัน)",
            "Accelerate Time (เร่งเวลา)",
            "Matrix Mode (สโลว์ + วิ่งไว)"
    };

    public TimeClockItem(Properties properties, boolean isCopperVersion) {
        super(properties);
        this.isCopperVersion = isCopperVersion;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && level.getServer() != null) {
            MinecraftServer server = level.getServer();
            ServerTickRateManager tickManager = server.tickRateManager();

            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            int currentMode = tag.getInt("TimeMode").orElse(0);
            boolean isActive = tag.getBoolean("IsActive").orElse(false);

            if (player.isShiftKeyDown()) {
                resetTime(tickManager, player);
                copperTimeRemaining = 0;
                currentMode = (currentMode + 1) % MODES.length;
                tag.putInt("TimeMode", currentMode);
                tag.putBoolean("IsActive", false);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
                player.displayClientMessage(Component.literal("§eเปลี่ยนโหมดเป็น: §f" + MODES[currentMode]), true);
                return InteractionResult.SUCCESS;
            }

            isActive = !isActive;
            tag.putBoolean("IsActive", isActive);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            if (!isActive) {
                resetTime(tickManager, player);
                copperTimeRemaining = 0;
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
                player.displayClientMessage(Component.literal("§aเวลากลับมาเดินตามปกติ"), true);
            } else {
                if (isCopperVersion) {
                    EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    stack.hurtAndBreak(1, player, slot);
                    copperTimeRemaining = 400;
                }
                applyTimeMode(tickManager, level, player, currentMode);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static void tickCopperClockLimit(MinecraftServer server) {
        if (copperTimeRemaining > 0) {
            copperTimeRemaining--;
            if (copperTimeRemaining == 0) {
                ServerTickRateManager tickManager = server.tickRateManager();
                tickManager.setFrozen(false);
                tickManager.setTickRate(20.0F);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    player.removeEffect(MobEffects.MOVEMENT_SPEED);
                    player.displayClientMessage(Component.literal("§cหมดเวลาของ Copper Clock (20 วินาที) แล้ว!"), true);
                }
            }
        }
    }

    private void applyTimeMode(ServerTickRateManager tickManager, Level level, Player player, int mode) {
        switch (mode) {
            case 0 -> {
                tickManager.setTickRate(20.0F);
                tickManager.setFrozen(true);
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.5F);
                player.displayClientMessage(Component.literal("§b[Stop Time] หยุดเวลา!"), true);
            }
            case 1 -> {
                tickManager.setFrozen(false);
                tickManager.setTickRate(5.0F);
                level.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.7F);
                player.displayClientMessage(Component.literal("§3[Slow Time] สโลว์โมชัน"), true);
            }
            case 2 -> {
                tickManager.setFrozen(false);
                tickManager.setTickRate(100.0F);
                level.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.8F);
                player.displayClientMessage(Component.literal("§6[Accelerate Time] เร่งเวลา"), true);
            }
            case 3 -> {
                tickManager.setFrozen(false);
                tickManager.setTickRate(5.0F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 3, false, false));
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.displayClientMessage(Component.literal("§d[Matrix Mode] เข้าสู่โหมดเมทริกซ์!"), true);
            }
        }
    }

    private void resetTime(ServerTickRateManager tickManager, Player player) {
        tickManager.setFrozen(false);
        tickManager.setTickRate(20.0F);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
    }
}
