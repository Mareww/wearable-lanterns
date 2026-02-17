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

    private static final Map<String, PhysicsState> physicsStates = new HashMap<>();

    private static class PhysicsState {
        float swingAngle = 0f;
        float swingVelocity = 0f;
        float forwardTilt = 0f;
        float forwardVelocity = 0f;
        double prevY = 0;
        // Shoulder-specific: track arm pitch for angular velocity
        float prevArmPitch = 0f;
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
                    config.isShoulderMirrored(), false,
                    config.isShoulderMirrored() ? playerModel.leftArm : playerModel.rightArm, "chest"
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

        // Capture arm pitch before physics (for shoulder pendulum)
        float armPitch = isHip ? 0f : attachPart.pitch;

        // Vertical movement detection
        double currentY = entity.getY();
        float verticalSpeed = (float) (currentY - physics.prevY);
        physics.prevY = currentY;

        if (pos.enablePhysics) {
            if (isHip) {
                // === HIP PENDULUM: driven by walk animation, swings side-to-side ===
                float legSwing = MathHelper.sin(limbAngle * 0.6662f) * limbDistance * 1.4f;
                float legAngle = mirrored ? (legSwing * 40f) : (-legSwing * 40f);

                float moveFactor = MathHelper.clamp(limbDistance, 0f, 1f);
                float walkForce = MathHelper.sin(limbAngle * 0.5f) * moveFactor * 0.5f;

                physics.swingVelocity += walkForce;
                physics.swingVelocity -= physics.swingAngle * 0.06f;
                physics.swingVelocity *= 0.88f;
                physics.swingAngle += physics.swingVelocity;

                // Leg collision
                float collisionThreshold = 3f - legAngle * 0.3f;
                if (mirrored) {
                    if (physics.swingAngle < -collisionThreshold) {
                        physics.swingAngle = -collisionThreshold;
                        physics.swingVelocity = Math.abs(physics.swingVelocity) * 0.5f + Math.abs(legAngle) * 0.02f;
                    }
                } else {
                    if (physics.swingAngle > collisionThreshold) {
                        physics.swingAngle = collisionThreshold;
                        physics.swingVelocity = -Math.abs(physics.swingVelocity) * 0.5f - Math.abs(legAngle) * 0.02f;
                    }
                }
                physics.swingAngle = MathHelper.clamp(physics.swingAngle, -15f, 15f);
            } else {
                // === SHOULDER PENDULUM: driven by arm angular velocity ===
                // The lantern hangs from the shoulder as a pendulum fulcrum.
                // Arm pitch changes create inertial forces - the lantern resists
                // sudden direction changes and lags behind the arm movement.
                float armAngularVelocity = armPitch - physics.prevArmPitch;
                physics.prevArmPitch = armPitch;

                // Inertia: arm acceleration drives the pendulum in the opposite direction
                physics.swingVelocity += -armAngularVelocity * 5f;
                // Gravity restoring force pulls lantern back to vertical
                physics.swingVelocity -= physics.swingAngle * 0.10f;
                // Damping from air resistance and flexible connection
                physics.swingVelocity *= 0.88f;
                physics.swingAngle += physics.swingVelocity;
                physics.swingAngle = MathHelper.clamp(physics.swingAngle, -20f, 20f);
            }

            // Forward/back tilt from vertical movement (jumping/falling) - both slots
            float verticalForce = -verticalSpeed * 15f;
            physics.forwardVelocity += verticalForce;
            physics.forwardVelocity -= physics.forwardTilt * 0.1f;
            physics.forwardVelocity *= 0.85f;
            physics.forwardTilt += physics.forwardVelocity;
            physics.forwardTilt = MathHelper.clamp(physics.forwardTilt, -25f, 25f);
        }

        matrices.push();

        // Attach to body/arm pivot
        attachPart.rotate(matrices);

        // Shoulder: counter-rotate the arm's pitch so the lantern hangs vertically
        // instead of rotating rigidly with the arm. The pendulum physics will add
        // its own swing angle driven by the arm's movement.
        if (!isHip) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(-armPitch));
        }

        if (isHip) {
            // Position from config
            matrices.translate(pos.x, pos.y, pos.z);

            // Hip: swing on Z axis (side-to-side tilt)
            float totalTilt = pos.baseTilt + (pos.enablePhysics ? physics.swingAngle : 0f);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.toRadians(totalTilt)));

            // Hip: forward lean + physics
            float totalForward = 5f + (pos.enablePhysics ? physics.swingAngle * 0.3f + physics.forwardTilt : 0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotation((float) Math.toRadians(totalForward)));
        } else {
            // Shoulder: tilt first at the pivot, then offset position
            matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.toRadians(pos.baseTilt)));

            // Shoulder: pendulum swing on X axis (forward/backward, same plane as arm)
            float totalSwing = pos.enablePhysics ? physics.swingAngle + physics.forwardTilt : 0f;
            matrices.multiply(RotationAxis.POSITIVE_X.rotation((float) Math.toRadians(totalSwing)));

            // Position from config (after tilt so it hangs from the tilted pivot)
            matrices.translate(pos.x, pos.y, pos.z);
        }

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
