package com.maskedfish.infinitexray.client.gui;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

import com.maskedfish.infinitexray.InfiniteXRayConfig;
import com.maskedfish.infinitexray.InfiniteXRayState;
import com.maskedfish.infinitexray.InfiniteXRayState.RenderMode;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Configuration screen for the x-ray highlight: render distance, render mode
 * (filled block or outline only) and highlight color.
 */
public class ConfigScreen extends Screen {

    private static final int[] PRESET_COLORS = {
            0xFF8C0C, 0xFF2222, 0xFFC822, 0x33E04D,
            0x2AD9E8, 0x3B6BFF, 0xA64DFF, 0xFF4D9E,
            0xFFFFFF, 0x8A8A8A, 0xFF7A3D, 0x66FFAA
    };

    private static final int DEFAULT_COLOR = 0xFF8C0C;

    private final Screen parent;
    private IntSlider radiusSlider;
    private IntSlider redSlider;
    private IntSlider greenSlider;
    private IntSlider blueSlider;
    private Button modeButton;

    public ConfigScreen() {
        this(null);
    }

    public ConfigScreen(Screen parent) {
        super(Component.translatable("screen.infinitexray.config_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int w = 240;

        this.radiusSlider = new IntSlider(cx - w / 2, 48, w, 20,
                InfiniteXRayState.getScanRadius(),
                InfiniteXRayState.MIN_SCAN_RADIUS, InfiniteXRayState.MAX_SCAN_RADIUS,
                value -> Component.translatable("screen.infinitexray.radius_label", value),
                InfiniteXRayState::setScanRadius);
        this.addRenderableWidget(this.radiusSlider);

        this.modeButton = Button.builder(Component.empty(), button -> {
            InfiniteXRayState.setRenderMode(
                    InfiniteXRayState.getRenderMode() == RenderMode.FILLED ? RenderMode.OUTLINE : RenderMode.FILLED);
            button.setMessage(modeLabel());
        }).bounds(cx - w / 2, 76, w, 20).build();
        this.modeButton.setMessage(modeLabel());
        this.addRenderableWidget(this.modeButton);

        // Preset color swatches (two rows of six).
        int swatchW = 34;
        int swatchH = 20;
        int gap = 4;
        int rowWidth = 6 * swatchW + 5 * gap;
        int startX = cx - rowWidth / 2;
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int row = i / 6;
            int col = i % 6;
            this.addRenderableWidget(new ColorSwatch(
                    startX + col * (swatchW + gap), 104 + row * (swatchH + gap),
                    swatchW, swatchH, PRESET_COLORS[i], this::applyColor));
        }

        // Fine-tuning RGB sliders.
        this.redSlider = new IntSlider(cx - w / 2, 156, w, 20,
                (InfiniteXRayState.getHighlightColor() >> 16) & 0xFF, 0, 255,
                value -> Component.translatable("screen.infinitexray.red", value),
                ignored -> this.applyRgb());
        this.greenSlider = new IntSlider(cx - w / 2, 182, w, 20,
                (InfiniteXRayState.getHighlightColor() >> 8) & 0xFF, 0, 255,
                value -> Component.translatable("screen.infinitexray.green", value),
                ignored -> this.applyRgb());
        this.blueSlider = new IntSlider(cx - w / 2, 208, w, 20,
                InfiniteXRayState.getHighlightColor() & 0xFF, 0, 255,
                value -> Component.translatable("screen.infinitexray.blue", value),
                ignored -> this.applyRgb());
        this.addRenderableWidget(this.redSlider);
        this.addRenderableWidget(this.greenSlider);
        this.addRenderableWidget(this.blueSlider);

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.infinitexray.reset"),
                button -> {
                    InfiniteXRayState.setScanRadius(InfiniteXRayState.DEFAULT_SCAN_RADIUS);
                    InfiniteXRayState.setRenderMode(RenderMode.FILLED);
                    InfiniteXRayState.setHighlightColor(DEFAULT_COLOR);
                    this.radiusSlider.setIntValue(InfiniteXRayState.DEFAULT_SCAN_RADIUS);
                    this.modeButton.setMessage(modeLabel());
                    this.redSlider.setIntValue((DEFAULT_COLOR >> 16) & 0xFF);
                    this.greenSlider.setIntValue((DEFAULT_COLOR >> 8) & 0xFF);
                    this.blueSlider.setIntValue(DEFAULT_COLOR & 0xFF);
                })
                .bounds(cx - w / 2, 238, w / 2 - 4, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.infinitexray.done"),
                button -> this.onClose())
                .bounds(cx + 4, 238, w / 2 - 4, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        InfiniteXRayConfig.save(
                InfiniteXRayState.getScanRadius(),
                InfiniteXRayState.getRenderMode(),
                InfiniteXRayState.getHighlightColor());
        if (this.parent != null) {
            this.minecraft.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }

    private void applyColor(int rgb) {
        InfiniteXRayState.setHighlightColor(rgb);
        this.redSlider.setIntValue((rgb >> 16) & 0xFF);
        this.greenSlider.setIntValue((rgb >> 8) & 0xFF);
        this.blueSlider.setIntValue(rgb & 0xFF);
    }

    private void applyRgb() {
        int rgb = (this.redSlider.intValue() << 16)
                | (this.greenSlider.intValue() << 8)
                | this.blueSlider.intValue();
        InfiniteXRayState.setHighlightColor(rgb);
    }

    private static Component modeLabel() {
        return Component.translatable(
                InfiniteXRayState.getRenderMode() == RenderMode.FILLED
                        ? "screen.infinitexray.mode_filled"
                        : "screen.infinitexray.mode_outline");
    }

    private static class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntFunction<Component> label;
        private final IntConsumer onChange;

        public IntSlider(int x, int y, int width, int height, int value, int min, int max,
                IntFunction<Component> label, IntConsumer onChange) {
            super(x, y, width, height, Component.empty(),
                    (double) (value - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.onChange = onChange;
            this.updateMessage();
        }

        public void setIntValue(int value) {
            this.value = (double) (value - min) / (double) (max - min);
            this.updateMessage();
        }

        public int intValue() {
            return this.min + (int) Math.round(this.value * (this.max - this.min));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(this.label.apply(this.intValue()));
        }

        @Override
        protected void applyValue() {
            this.onChange.accept(this.intValue());
        }
    }

    private static class ColorSwatch extends AbstractWidget {
        private final int rgb;
        private final IntConsumer onSelect;

        public ColorSwatch(int x, int y, int width, int height, int rgb, IntConsumer onSelect) {
            super(x, y, width, height, Component.empty());
            this.rgb = rgb;
            this.onSelect = onSelect;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean selected = InfiniteXRayState.getHighlightColor() == this.rgb;
            int border = selected
                    ? 0xFFFFFFFF
                    : (this.isHoveredOrFocused() ? 0xFFCCCCCC : 0xFF606060);
            graphics.fill(this.getX() - 1, this.getY() - 1,
                    this.getX() + this.width + 1, this.getY() + this.height + 1, border);
            graphics.fill(this.getX(), this.getY(),
                    this.getX() + this.width, this.getY() + this.height, 0xFF000000 | this.rgb);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.active && this.visible && button == 0 && this.isMouseOver(mouseX, mouseY)) {
                this.onSelect.accept(this.rgb);
                return true;
            }
            return false;
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            this.defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
