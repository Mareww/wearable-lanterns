package com.wearablelanterns;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class LanternConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static LanternConfig instance;

    // --- Variant Selection ---
    // Valid options are listed in the _options fields
    public String _legs_options = "left_hip, right_hip";
    public String legs_variant = "left_hip";

    public String _chest_options = "right_shoulder, left_shoulder";
    public String chest_variant = "right_shoulder";

    // Head has no variants - lantern always sits on top

    // --- Render Positions ---
    // x: left/right (positive = left), y: up/down (positive = down from pivot), z: front/back (positive = front)
    // baseTilt: side lean in degrees, outwardAngle: Y rotation in degrees
    // dropDistance: how far the lantern hangs below the pivot point
    // enablePhysics: whether the lantern sways with movement

    public SlotPosition left_hip = new SlotPosition(0.25f, 0.62f, 0.12f, 0.32f, -10f, 25f, 0.18f, true);
    public SlotPosition right_hip = new SlotPosition(-0.25f, 0.62f, 0.12f, 0.32f, 10f, -25f, 0.18f, true);
    public SlotPosition right_shoulder = new SlotPosition(-0.5f, 0.05f, 0.0f, 0.30f, 8f, -20f, 0.12f, true);
    public SlotPosition left_shoulder = new SlotPosition(0.5f, 0.05f, 0.0f, 0.30f, -8f, 20f, 0.12f, true);
    public SlotPosition head = new SlotPosition(0.0f, -0.75f, 0.0f, 0.45f, 0f, 0f, 0f, false);

    public static class SlotPosition {
        public float x;
        public float y;
        public float z;
        public float scale;
        public float baseTilt;
        public float outwardAngle;
        public float dropDistance;
        public boolean enablePhysics;

        public SlotPosition() {}

        public SlotPosition(float x, float y, float z, float scale, float baseTilt, float outwardAngle, float dropDistance, boolean enablePhysics) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.scale = scale;
            this.baseTilt = baseTilt;
            this.outwardAngle = outwardAngle;
            this.dropDistance = dropDistance;
            this.enablePhysics = enablePhysics;
        }
    }

    public static LanternConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("wearablelanterns.json");

        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                instance = GSON.fromJson(reader, LanternConfig.class);
                if (instance == null) {
                    instance = new LanternConfig();
                }
            } catch (Exception e) {
                WearableLanterns.LOGGER.error("Failed to load config, using defaults", e);
                instance = new LanternConfig();
            }
        } else {
            instance = new LanternConfig();
        }

        // Fill in defaults for any fields missing from an older config file
        LanternConfig defaults = new LanternConfig();
        if (instance.legs_variant == null) instance.legs_variant = defaults.legs_variant;
        if (instance.chest_variant == null) instance.chest_variant = defaults.chest_variant;
        if (instance.left_hip == null) instance.left_hip = defaults.left_hip;
        if (instance.right_hip == null) instance.right_hip = defaults.right_hip;
        if (instance.right_shoulder == null) instance.right_shoulder = defaults.right_shoulder;
        if (instance.left_shoulder == null) instance.left_shoulder = defaults.left_shoulder;
        if (instance.head == null) instance.head = defaults.head;

        // Always save to create the file or update with any new fields
        save();
    }

    public static void save() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("wearablelanterns.json");
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(instance, writer);
            }
        } catch (Exception e) {
            WearableLanterns.LOGGER.error("Failed to save config", e);
        }
    }

    public SlotPosition getLegsPosition() {
        return "right_hip".equals(legs_variant) ? right_hip : left_hip;
    }

    public SlotPosition getChestPosition() {
        return "left_shoulder".equals(chest_variant) ? left_shoulder : right_shoulder;
    }

    public SlotPosition getHeadPosition() {
        return head;
    }

    public boolean isHipMirrored() {
        return "right_hip".equals(legs_variant);
    }

    public boolean isShoulderMirrored() {
        return "left_shoulder".equals(chest_variant);
    }
}
