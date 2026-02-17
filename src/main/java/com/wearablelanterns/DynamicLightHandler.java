package com.wearablelanterns;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DynamicLightHandler {

    private static final Map<UUID, BlockPos> lastLightPositions = new HashMap<>();
    // When onUnequip fires, suppress light placement for several ticks
    // This prevents the tick handler from re-placing light before the trinket state fully updates
    private static final Map<UUID, Integer> suppressionTicks = new HashMap<>();
    private static final int SUPPRESS_DURATION = 5;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                handlePlayer(player);
            }
        });
    }

    private static void handlePlayer(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        UUID uuid = player.getUuid();
        BlockPos currentPos = player.getBlockPos();

        // Check if light is suppressed (recently unequipped)
        Integer suppress = suppressionTicks.get(uuid);
        if (suppress != null) {
            // Actively scrub any light blocks near the player
            cleanAllNearby(world, currentPos);
            BlockPos lastPos = lastLightPositions.remove(uuid);
            if (lastPos != null) {
                removeLightIfOurs(world, lastPos);
            }
            if (suppress <= 1) {
                suppressionTicks.remove(uuid);
            } else {
                suppressionTicks.put(uuid, suppress - 1);
            }
            return;
        }

        boolean hasLantern = isWearingLantern(player);
        BlockPos lastPos = lastLightPositions.get(uuid);

        if (hasLantern) {
            // Remove old light if player moved
            if (lastPos != null && !lastPos.equals(currentPos)) {
                removeLightIfOurs(world, lastPos);
            }

            // Place light at current position if it's air
            BlockState state = world.getBlockState(currentPos);
            if (state.isAir()) {
                world.setBlockState(currentPos,
                        Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15),
                        Block.NOTIFY_ALL);
                lastLightPositions.put(uuid, currentPos);
            } else if (lastPos != null && lastPos.equals(currentPos)) {
                // Still at same position, keep the light
            } else {
                // Can't place here, try one block above (eye level)
                BlockPos above = currentPos.up();
                BlockState aboveState = world.getBlockState(above);
                if (aboveState.isAir()) {
                    world.setBlockState(above,
                            Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 15),
                            Block.NOTIFY_ALL);
                    lastLightPositions.put(uuid, above);
                }
            }
        } else {
            // No lantern equipped - clean up
            if (lastPos != null) {
                removeLightIfOurs(world, lastPos);
                lastLightPositions.remove(uuid);
            }
        }
    }

    private static void cleanAllNearby(ServerWorld world, BlockPos center) {
        // Scrub light blocks at and around the player position
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    removeLightIfOurs(world, center.add(dx, dy, dz));
                }
            }
        }
    }

    private static boolean isWearingLantern(ServerPlayerEntity player) {
        Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) {
            return false;
        }

        List<Pair<dev.emi.trinkets.api.SlotReference, ItemStack>> equipped = component.get().getAllEquipped();
        for (Pair<dev.emi.trinkets.api.SlotReference, ItemStack> pair : equipped) {
            ItemStack stack = pair.getRight();
            if (!stack.isEmpty()) {
                Identifier id = Registries.ITEM.getId(stack.getItem());
                if (id.getPath().toLowerCase().contains("lantern")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void removeLightIfOurs(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isOf(Blocks.LIGHT)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    public static void removePlayerLight(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        // Suppress light placement for several ticks
        suppressionTicks.put(uuid, SUPPRESS_DURATION);
        // Immediately clean up
        BlockPos lastPos = lastLightPositions.remove(uuid);
        ServerWorld world = player.getServerWorld();
        if (lastPos != null) {
            removeLightIfOurs(world, lastPos);
        }
        cleanAllNearby(world, player.getBlockPos());
    }
}
