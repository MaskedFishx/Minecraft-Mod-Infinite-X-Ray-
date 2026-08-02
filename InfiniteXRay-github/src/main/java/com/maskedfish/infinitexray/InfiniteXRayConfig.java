package com.maskedfish.infinitexray;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.client.Minecraft;

/**
 * Simple JSON config stored in {@code <gamedir>/config/infinitexray.json}.
 * Only used on the client.
 */
public final class InfiniteXRayConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "infinitexray.json";

    public int scanRadius = InfiniteXRayState.DEFAULT_SCAN_RADIUS;
    public String renderMode = "filled";
    public int highlightColor = 0xFF8C0C;

    private InfiniteXRayConfig() {
    }

    public static InfiniteXRayConfig load() {
        try {
            File file = getFile();
            if (file.isFile()) {
                InfiniteXRayConfig config = GSON.fromJson(Files.readString(file.toPath()), InfiniteXRayConfig.class);
                if (config != null) {
                    return config;
                }
            }
        } catch (Exception e) {
            InfiniteXRayMod.LOGGER.warn("Failed to load InfiniteXRay config", e);
        }
        return new InfiniteXRayConfig();
    }

    public static void save(int scanRadius, InfiniteXRayState.RenderMode renderMode, int highlightColor) {
        try {
            File file = getFile();
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            InfiniteXRayConfig config = new InfiniteXRayConfig();
            config.scanRadius = scanRadius;
            config.renderMode = renderMode.name().toLowerCase(Locale.ROOT);
            config.highlightColor = highlightColor & 0xFFFFFF;
            Files.writeString(file.toPath(), GSON.toJson(config));
        } catch (IOException e) {
            InfiniteXRayMod.LOGGER.warn("Failed to save InfiniteXRay config", e);
        }
    }

    private static File getFile() {
        return new File(Minecraft.getInstance().gameDirectory, "config" + File.separator + FILE_NAME);
    }
}
