package com.wearablelanterns.client;

import com.wearablelanterns.LanternConfig;
import com.wearablelanterns.LanternConfig.SlotPosition;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.HashMap;
import java.util.Map;

public class LanternTrinketRenderer implements TrinketRenderer {

    // Per-entity, per-slot physics state to avoid cross-contamination
    private static final Map<String, PhysicsState> physicsStates = new HashMap<>();

    private static class PhysicsState {
        float swingAngle = 0f;
        float swingVelocity = 0f;
        float forwardTilt = 0f;
        float forwardVelocity = 0f;
        double prevY = 0;
    }

    private static PhysicsState getPhysics(int entityId, String group) {
        String key = entityId + ":" + group;
        return physicsStates.computeIfAbsent(key, k -> new PhysicsState());
    }

    @Override
    public void render(
            ItemStack stack,
            SlotReference slotReference,
            EntityModel<? extends LivingEntity> contextModel,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            LivingEntity entity,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float animationProgress,
            float headYaw,
            float headPitch
    ) {
        if (!(contextModel instanceof PlayerEntityModel<?> playerModel)) {
            return;
        }
        if (!(entity instanceof AbstractClientPlayerEntity player)) {
            return;
        }

        String group = slotReference.inventory().getSlotType().getGroup();
        LanternConfig config = LanternConfig.get();

        switch (group) {
            case "legs" -> renderOnBody(
                    stack, playerModel, matrices, vertexConsumers, entity,
                    limbAngle, limbDistance, config.getLegsPosition(),
                    config.isHipMirrored(), true, playerModel.body, "legs"
            );
            case "chest" -> renderOnBody(
                    stack, playerModel, matrices, vertexConsumers, entity,
                    limbAngle, limbDistance, config.getChestPosition(),
                    config.isShoulderMirrored(), false, playerModel.body, "chest"
            );
            case "head" -> renderOnHead(
                    stack, playerModel, matrices, vertexConsumers, entity,
                    config.getHeadPosition()
            );
        }
    }

    private void renderOnBody(
            ItemStack stack,
            PlayerEntityModel<?> playerModel,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            LivingEntity entity,
            float limbAngle,
            float limbDistance,
            SlotPosition pos,
            boolean mirrored,
            boolean isHip,
            ModelPart attachPart,
            String group
    ) {
        PhysicsState physics = getPhysics(entity.getId(), group);

        // Vertical movement detection
        double currentY = entity.getY();
        float verticalSpeed = (float) (currentY - physics.prevY);
        physics.prevY = currentY;

        if (pos.enablePhysics) {
            // Leg swing angle for collision detection (hip only)
            float legSwing = MathHelper.sin(limbAngle * 0.6662f) * limbDistance * 1.4f;
            // Use the correct leg based on which side the lantern is on
            float legAngle = mirrored ? (legSwing * 40f) : (-legSwing * 40f);

            // Pendulum physics - reduced force for shoulder variants
            float moveFactor = MathHelper.clamp(limbDistance, 0f, 1f);
            float walkForce = MathHelper.sin(limbAngle * 0.5f) * moveFactor * (isHip ? 0.5f : 0.3f);

            physics.swingVelocity += walkForce;
            physics.swingVelocity -= physics.swingAngle * 0.06f;
            physics.swingVelocity *= 0.88f;
            physics.swingAngle += physics.swingVelocity;

            // Leg collision (hip variants only)
            if (isHip) {
                float collisionThreshold = 3f - legAngle * 0.3f;

                if (mirrored) {
                    // Right hip - collision from the opposite direction
                    if (physics.swingAngle < -collisionThreshold) {
                        physics.swingAngle = -collisionThreshold;
                        physics.swingVelocity = Math.abs(physics.swingVelocity) * 0.5f + Math.abs(legAngle) * 0.02f;
                    }
                } else {
                    // Left hip - original collision behavior
                    if (physics.swingAngle > collisionThreshold) {
                        physics.swingAngle = collisionThreshold;
                        physics.swingVelocity = -Math.abs(physics.swingVelocity) * 0.5f - Math.abs(legAngle) * 0.02f;
                    }
                }
            }

            physics.swingAngle = MathHelper.clamp(physics.swingAngle, -15f, 15f);

            // Forward/back tilt from vertical movement
            float verticalForce = -verticalSpeed * 15f;
            physics.forwardVelocity += verticalForce;
            physics.forwardVelocity -= physics.forwardTilt * 0.1f;
            physics.forwardVelocity *= 0.85f;
            physics.forwardTilt += physics.forwardVelocity;
            physics.forwardTilt = MathHelper.clamp(physics.forwardTilt, -25f, 25f);
        }

        matrices.push();

        // Attach to body
        attachPart.rotate(matrices);

        // Position from config
        matrices.translate(pos.x, pos.y, pos.z);

        // Side swing (base tilt + dynamic physics)
        float totalTilt = pos.baseTilt + (pos.enablePhysics ? physics.swingAngle : 0f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.toRadians(totalTilt)));

        // Forward/back tilt (slight base lean for hip, none for shoulder)
        float forwardBase = isHip ? 5f : 0f;
        float totalForward = forwardBase + (pos.enablePhysics ? physics.swingAngle * 0.3f + physics.forwardTilt : 0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotation((float) Math.toRadians(totalForward)));

        // Drop down from pivot point
        matrices.translate(0.0f, pos.dropDistance, 0.0f);

        // Flip upright (model space Y is downward)
        matrices.multiply(RotationAxis.POSITIVE_X.rotation((float) Math.toRadians(180.0)));

        // Angle outward
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) Math.toRadians(pos.outwardAngle)));

        // Scale
        matrices.scale(pos.scale, pos.scale, pos.scale);

        // Center the block model and render
        matrices.translate(-0.5f, -0.5f, -0.5f);
        renderBlock(stack, matrices, vertexConsumers);

        matrices.pop();
    }

    private void renderOnHead(
            ItemStack stack,
            PlayerEntityModel<?> playerModel,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            LivingEntity entity,
            SlotPosition pos
    ) {
        matrices.push();

        // Attach to head - lantern follows head rotation
        playerModel.head.rotate(matrices);

        // Position on top of head (negative Y = upward from pivot at neck)
        matrices.translate(pos.x, pos.y, pos.z);

        // Flip upright (model space Y is downward)
        matrices.multiply(RotationAxis.POSITIVE_X.rotation((float) Math.toRadians(180.0)));

        // Scale
        matrices.scale(pos.scale, pos.scale, pos.scale);

        // Center the block model and render
        matrices.translate(-0.5f, -0.5f, -0.5f);
        renderBlock(stack, matrices, vertexConsumers);

        matrices.pop();
    }

    private void renderBlock(ItemStack stack, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            BlockRenderManager blockRenderer = MinecraftClient.getInstance().getBlockRenderManager();
            blockRenderer.renderBlockAsEntity(
                    block.getDefaultState(),
                    matrices,
                    vertexConsumers,
                    15728880,
                    OverlayTexture.DEFAULT_UV
            );
        }
    }
}
