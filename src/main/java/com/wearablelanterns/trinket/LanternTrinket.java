package com.wearablelanterns.trinket;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketEnums.DropRule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class LanternTrinket implements Trinket {

    @Override
    public boolean canEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return id.getPath().toLowerCase().contains("lantern");
    }

    @Override
    public void tick(ItemStack stack, SlotReference slot, LivingEntity entity) {
    }

    @Override
    public DropRule getDropRule(ItemStack stack, SlotReference slot, LivingEntity entity) {
        return DropRule.DEFAULT;
    }
}
