package com.maskedfish.infinitexray;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * A pure client-side block search &amp; x-ray highlight mod for NeoForge 1.21.
 * All real functionality lives in client-only classes, so this mod works on
 * any server without the server installing anything.
 */
@Mod(InfiniteXRayMod.MODID)
public class InfiniteXRayMod {
    public static final String MODID = "infinitexray";
    public static final Logger LOGGER = LogUtils.getLogger();

    public InfiniteXRayMod(IEventBus modEventBus) {
        // Everything is wired up through @EventBusSubscriber classes on the client.
    }
}
