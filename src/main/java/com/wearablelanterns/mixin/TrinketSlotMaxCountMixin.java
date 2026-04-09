package com.wearablelanterns.mixin;

import dev.emi.trinkets.api.TrinketInventory;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public class TrinketSlotMaxCountMixin {

    @Inject(method = "getMaxItemCount()I", at = @At("HEAD"), cancellable = true)
    private void wearablelanterns$limitTrinketSlot(CallbackInfoReturnable<Integer> cir) {
        Slot self = (Slot)(Object)this;
        if (self.inventory instanceof TrinketInventory) {
            cir.setReturnValue(1);
        }
    }
}
