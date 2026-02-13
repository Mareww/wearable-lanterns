package com.wearablelanterns;

import com.wearablelanterns.trinket.LanternTrinket;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WearableLanterns implements ModInitializer {

    public static final String MOD_ID = "wearablelanterns";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LanternConfig.load();

        // Register all items containing "lantern" in their ID
        registerAllLanterns();

        DynamicLightHandler.register();

        LOGGER.info("Wearable Lanterns loaded!");
    }

    private void registerAllLanterns() {
        LanternTrinket trinket = new LanternTrinket();
        int count = 0;

        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            String path = id.getPath().toLowerCase();

            // Match any item with "lantern" in the name
            if (path.contains("lantern")) {
                TrinketsApi.registerTrinket(item, trinket);
                LOGGER.debug("Registered lantern trinket: {}", id);
                count++;
            }
        }

        LOGGER.info("Registered {} lantern(s) as wearable trinkets", count);
    }
}
