package com.example.timestop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class TimeReverserItem extends Item {
    private static final int MAX_HISTORY_TICKS = 100;
    private static final Map<UUID, Deque<EntitySnapshot>> historyMap = new HashMap<>();

    private record EntitySnapshot(Vec3 pos, float yRot, float xRot, float health) {}

    public TimeReverserItem(Properties properties) {
        super(properties);
    }

    public static void recordServerTick(MinecraftServer server) {
        if (server.tickRateManager().isFrozen()) return;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    Deque<EntitySnapshot> history = historyMap.computeIfAbsent(living.getUUID(), k -> new ArrayDeque<>());
                    if (history.size() >= MAX_HISTORY_TICKS) {
                        history.removeFirst();
                    }
                    history.addLast(new EntitySnapshot(living.position(), living.getYRot(), living.getXRot(), living.getHealth()));
                }
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            int rewoundCount = 0;

            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof LivingEntity living && historyMap.containsKey(living.getUUID())) {
                    Deque<EntitySnapshot> history = historyMap.get(living.getUUID());
                    if (!history.isEmpty()) {
                        EntitySnapshot oldest = history.peekFirst();
                        living.teleportTo(oldest.pos.x, oldest.pos.y, oldest.pos.z);
                        living.setYRot(oldest.yRot);
                        living.setXRot(oldest.xRot);
                        living.setHealth(oldest.health);
                        history.clear();
                        rewoundCount++;
                    }
                }
            }

            level.playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.7F, 1.8F);
            player.displayClientMessage(Component.literal("§d[Time Reverser] ย้อนเวลากลับไป 5 วินาที! (ย้อนกลับ " + rewoundCount + " เป้าหมาย)"), true);
            player.getCooldowns().addCooldown(this.getDefaultInstance(), 40);
        }
        return InteractionResult.SUCCESS;
    }
}
