package com.maskedfish.infinitexray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Client-side singleton holding the current selection and the cached list of
 * matching block positions.
 */
public final class InfiniteXRayState {
    public enum RenderMode {
        FILLED,
        OUTLINE;

        public static RenderMode fromString(String name) {
            if (name == null) {
                return null;
            }
            try {
                return RenderMode.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** Default max distance in blocks from the player that is scanned and highlighted. */
    public static final int DEFAULT_SCAN_RADIUS = 80;

    public static final int MIN_SCAN_RADIUS = 16;
    public static final int MAX_SCAN_RADIUS = 160;

    /** How often (in ticks) the world is rescanned while the highlight is on. */
    private static final int SCAN_INTERVAL = 60;

    private static Block selected;
    private static boolean enabled;
    private static int nextScanTick = 0;
    private static int scanRadius = DEFAULT_SCAN_RADIUS;
    private static RenderMode renderMode = RenderMode.FILLED;
    private static int highlightColor = 0xFF8C0C; // RGB, default orange

    private static final List<BlockPos> positions = new ArrayList<>();

    static {
        InfiniteXRayConfig config = InfiniteXRayConfig.load();
        int loaded = config.scanRadius;
        if (loaded >= MIN_SCAN_RADIUS && loaded <= MAX_SCAN_RADIUS) {
            scanRadius = loaded;
        }
        RenderMode loadedMode = RenderMode.fromString(config.renderMode);
        if (loadedMode != null) {
            renderMode = loadedMode;
        }
        highlightColor = config.highlightColor & 0xFFFFFF;
    }

    private InfiniteXRayState() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static Block getSelected() {
        return selected;
    }

    public static List<BlockPos> getPositions() {
        return positions;
    }

    /** Whether the player has ever picked a block (even if currently off). */
    public static boolean hasSelection() {
        return selected != null;
    }

    public static int getScanRadius() {
        return scanRadius;
    }

    /**
     * Updates the scan/render distance. The actual rescan is debounced by a
     * few ticks so dragging a slider does not trigger scans every frame.
     */
    public static void setScanRadius(int radius) {
        scanRadius = Math.max(MIN_SCAN_RADIUS, Math.min(MAX_SCAN_RADIUS, radius));
        nextScanTick = 5;
    }

    public static RenderMode getRenderMode() {
        return renderMode;
    }

    public static void setRenderMode(RenderMode mode) {
        renderMode = mode;
    }

    /** Highlight color as 0xRRGGBB. */
    public static int getHighlightColor() {
        return highlightColor;
    }

    public static void setHighlightColor(int rgb) {
        highlightColor = rgb & 0xFFFFFF;
    }

    public static void select(Block block) {
        selected = block;
        enabled = true;
        nextScanTick = 0;
        rescan();
        InfiniteXRayMod.LOGGER.info("InfiniteXRay: highlighting {} ({} blocks found)",
                BuiltInRegistries.BLOCK.getKey(block), positions.size());
    }

    public static void disable() {
        enabled = false;
        positions.clear();
        // Keep `selected` so the toggle key can quickly turn it back on.
    }

    public static boolean toggle() {
        if (enabled) {
            disable();
        } else {
            enabled = true;
            nextScanTick = 0;
            rescan();
        }
        return enabled;
    }

    public static void tick() {
        if (!enabled || selected == null) {
            positions.clear();
            return;
        }
        if (--nextScanTick > 0) {
            return;
        }
        nextScanTick = SCAN_INTERVAL;
        rescan();
    }

    /**
     * Scans every loaded chunk section within the configured radius for blocks
     * matching the current selection. Runs on the client thread, so this is
     * throttled by {@link #SCAN_INTERVAL} to keep the game smooth.
     */
    public static void rescan() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || selected == null || !enabled) {
            positions.clear();
            return;
        }

        Block target = selected;
        BlockPos center = player.blockPosition();
        int chunkRadius = Math.max(1, (scanRadius + 15) / 16);
        int pcx = SectionPos.blockToSectionCoord(center.getX());
        int pcz = SectionPos.blockToSectionCoord(center.getZ());

        List<BlockPos> found = new ArrayList<>();
        for (int cx = pcx - chunkRadius; cx <= pcx + chunkRadius; cx++) {
            for (int cz = pcz - chunkRadius; cz <= pcz + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                LevelChunkSection[] sections = chunk.getSections();
                int minSectionY = SectionPos.blockToSectionCoord(chunk.getMinBuildHeight());
                int xBase = cx << 4;
                int zBase = cz << 4;
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    if (section == null || section.hasOnlyAir()) {
                        continue;
                    }
                    if (!section.maybeHas(state -> state.is(target))) {
                        continue;
                    }
                    int yBase = SectionPos.sectionToBlockCoord(minSectionY + i);
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                if (section.getBlockState(x, y, z).is(target)) {
                                    found.add(new BlockPos(xBase + x, yBase + y, zBase + z));
                                }
                            }
                        }
                    }
                }
            }
        }

        positions.clear();
        positions.addAll(found);
    }
}
