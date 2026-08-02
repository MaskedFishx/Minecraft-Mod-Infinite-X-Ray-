package com.maskedfish.infinitexray;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

public final class Keybinds {
    public static final String CATEGORY = "key.categories.infinitexray";

    /** Opens the block search screen. Default: X */
    public static final KeyMapping OPEN_SEARCH = new KeyMapping(
            "key.infinitexray.open_search",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            CATEGORY);

    /** Toggles the highlight on/off. Default: G */
    public static final KeyMapping TOGGLE_HIGHLIGHT = new KeyMapping(
            "key.infinitexray.toggle_highlight",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            CATEGORY);

    private Keybinds() {
    }
}
