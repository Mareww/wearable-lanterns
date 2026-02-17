package com.wearablelanterns;

import com.wearablelanterns.client.LanternTrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class WearableLanternsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LanternTrinketRenderer renderer = new LanternTrinketRenderer();
        int count = 0;

        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            if (id.getPath().toLowerCase().contains("lantern")) {
                TrinketRendererRegistry.registerRenderer(item, renderer);
                count++;
            }
        }

        WearableLanterns.LOGGER.info("Registered lantern renderer for {} item(s)", count);
    }
}
