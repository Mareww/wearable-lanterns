package com.wearablelanterns.client;

import com.wearablelanterns.LanternConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class LanternConfigScreen extends Screen {

    private final Screen parent;

    public LanternConfigScreen(Screen parent) {
        super(Text.literal("Wearable Lanterns Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        LanternConfig config = LanternConfig.get();
        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Legs variant: Left Hip / Right Hip
        addDrawableChild(CyclingButtonWidget.<String>builder(value -> Text.literal(formatName(value)))
                .values(List.of("left_hip", "right_hip"))
                .initially(config.legs_variant)
                .build(centerX - 100, startY, 200, 20, Text.literal("Legs Position"),
                        (button, value) -> config.legs_variant = value));

        // Chest variant: Right Shoulder / Left Shoulder
        addDrawableChild(CyclingButtonWidget.<String>builder(value -> Text.literal(formatName(value)))
                .values(List.of("right_shoulder", "left_shoulder"))
                .initially(config.chest_variant)
                .build(centerX - 100, startY + 30, 200, 20, Text.literal("Chest Position"),
                        (button, value) -> config.chest_variant = value));

        // Done button
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(centerX - 100, startY + 80, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Edit config/wearablelanterns.json for fine-tuning positions"),
                this.width / 2, this.height / 4 + 60, 0x888888);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        LanternConfig.save();
        this.client.setScreen(parent);
    }

    private static String formatName(String variant) {
        StringBuilder sb = new StringBuilder();
        for (String part : variant.split("_")) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1));
        }
        return sb.toString();
    }
}
