package com.maskedfish.infinitexray;

import com.maskedfish.infinitexray.client.gui.BlockSearchScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = InfiniteXRayMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        while (Keybinds.OPEN_SEARCH.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new BlockSearchScreen());
            }
        }

        while (Keybinds.TOGGLE_HIGHLIGHT.consumeClick()) {
            // No block picked yet: open the search screen so there is
            // something to enable.
            if (!InfiniteXRayState.hasSelection()) {
                if (mc.screen == null) {
                    mc.setScreen(new BlockSearchScreen());
                }
                continue;
            }
            boolean enabled = InfiniteXRayState.toggle();
            LocalPlayer player = mc.player;
            if (player != null) {
                player.displayClientMessage(
                        enabled
                                ? Component.translatable("message.infinitexray.enabled",
                                        InfiniteXRayState.getSelected().getName())
                                : Component.translatable("message.infinitexray.disabled"),
                        true);
            }
        }

        InfiniteXRayState.tick();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        HighlightRenderer.render(event);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        InfiniteXRayState.disable();
    }
}
