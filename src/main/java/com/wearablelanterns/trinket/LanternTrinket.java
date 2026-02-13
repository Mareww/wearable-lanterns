package com.wearablelanterns.trinket;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketEnums.DropRule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public class LanternTrinket implements Trinket {

    @Override
    public void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
        // No special effects - just a wearable light source
    }

    @Override
    public DropRule getDropRule(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return DropRule.DEFAULT;
    }
}
