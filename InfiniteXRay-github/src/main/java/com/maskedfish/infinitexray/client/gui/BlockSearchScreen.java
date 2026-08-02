package com.maskedfish.infinitexray.client.gui;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.maskedfish.infinitexray.InfiniteXRayBlocks;
import com.maskedfish.infinitexray.InfiniteXRayState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * Search screen: type a block name, click an entry, and all matching blocks
 * in the world get highlighted through walls.
 */
public class BlockSearchScreen extends Screen {

    private EditBox searchBox;
    private BlockList list;
    private String query = "";

    public BlockSearchScreen() {
        super(Component.translatable("screen.infinitexray.title"));
    }

    @Override
    protected void init() {
        this.searchBox = new EditBox(this.font, this.width / 2 - 140, 26, 280, 20,
                Component.translatable("screen.infinitexray.search_hint"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);
        this.searchBox.setFocused(true);

        this.list = new BlockList(this.minecraft, this.width, this.height - 120, 56, 22);
        this.addRenderableWidget(this.list);

        // Restore the search text when this screen is re-initialized (e.g.
        // when returning from the settings screen).
        if (!this.query.isEmpty()) {
            this.searchBox.setValue(this.query);
        }

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.infinitexray.settings"),
                button -> this.minecraft.setScreen(new ConfigScreen(this)))
                .bounds(this.width / 2 - 60, this.height - 46, 120, 20)
                .build());

        refreshList();
    }

    private void onSearchChanged(String text) {
        this.query = text;
        refreshList();
    }

    private void refreshList() {
        this.list.setBlocks(InfiniteXRayBlocks.search(this.query), !this.query.trim().isEmpty());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("screen.infinitexray.hint"),
                this.width / 2, this.height - 24, 0xFFA0A0A0);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            BlockEntry entry = this.list.getSelected();
            if (entry != null) {
                selectEntry(entry);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void selectEntry(BlockEntry entry) {
        if (entry.block == null) {
            InfiniteXRayState.disable();
        } else {
            InfiniteXRayState.select(entry.block);
            if (this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.translatable("message.infinitexray.enabled", entry.block.getName()), true);
            }
        }
        this.onClose();
    }

    private class BlockList extends ObjectSelectionList<BlockEntry> {
        public BlockList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        public void setBlocks(List<Block> blocks, boolean autoSelectFirst) {
            this.clearEntries();
            this.setSelected(null);
            this.addEntry(new BlockEntry(null));
            boolean first = true;
            for (Block block : blocks) {
                this.addEntry(new BlockEntry(block));
                if (autoSelectFirst && first && this.getSelected() == null) {
                    this.setSelected(this.children().get(this.children().size() - 1));
                    first = false;
                }
            }
        }

        @Override
        public int getRowWidth() {
            return Math.min(440, this.width - 60);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowRight() + 6;
        }
    }

    private class BlockEntry extends ObjectSelectionList.Entry<BlockEntry> {
        private final Block block; // null = "disable highlight" entry
        private final Component displayName;
        private final String idText;

        public BlockEntry(Block block) {
            this.block = block;
            if (block == null) {
                this.displayName = Component.translatable("screen.infinitexray.disable");
                this.idText = "";
            } else {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                this.displayName = block.getName();
                this.idText = id.toString();
            }
        }

        private boolean isCurrentSelection() {
            if (this.block == null) {
                return !InfiniteXRayState.isEnabled();
            }
            return InfiniteXRayState.isEnabled() && InfiniteXRayState.getSelected() == this.block;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovered, float partialTick) {
            boolean active = isCurrentSelection();
            if (active) {
                graphics.fill(left + 1, top, left + width - 1, top + height, 0x5533AA33);
            } else if (hovered) {
                graphics.fill(left + 1, top, left + width - 1, top + height, 0x33FFFFFF);
            }
            int textColor = active ? 0xFFFFD24D : 0xFFFFFFFF;
            graphics.drawString(BlockSearchScreen.this.font, this.displayName,
                    left + 6, top + (height - 8) / 2, textColor, false);
            if (!this.idText.isEmpty()) {
                graphics.drawString(BlockSearchScreen.this.font, this.idText,
                        left + width - 6 - BlockSearchScreen.this.font.width(this.idText),
                        top + (height - 8) / 2, 0x99A0A0A0, false);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                selectEntry(this);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return this.displayName;
        }
    }
}
