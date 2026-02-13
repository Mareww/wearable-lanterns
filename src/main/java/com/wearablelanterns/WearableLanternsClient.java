package com.wearablelanterns;

import com.wearablelanterns.client.LanternTrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.item.Items;

public class WearableLanternsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register the hip lantern renderer for both lantern types
        LanternTrinketRenderer renderer = new LanternTrinketRenderer();
        TrinketRendererRegistry.registerRenderer(Items.LANTERN, renderer);
        TrinketRendererRegistry.registerRenderer(Items.SOUL_LANTERN, renderer);
    }
}
