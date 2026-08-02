package com.maskedfish.infinitexray;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Helpers for listing and searching all blocks in the game. */
public final class InfiniteXRayBlocks {
    /** Blocks shown first when the search box is empty. */
    private static final List<Block> COMMON = new ArrayList<>();

    private static final List<Block> ALL = new ArrayList<>();

    private static final String[] COMMON_IDS = {
            "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
            "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
            "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
            "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
            "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
            "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
            "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
            "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
            "minecraft:nether_quartz_ore", "minecraft:nether_gold_ore",
            "minecraft:ancient_debris",
            "minecraft:raw_iron_block", "minecraft:raw_gold_block", "minecraft:raw_copper_block",
            "minecraft:diamond_block", "minecraft:iron_block", "minecraft:gold_block",
            "minecraft:emerald_block", "minecraft:lapis_block", "minecraft:redstone_block",
            "minecraft:copper_block", "minecraft:netherite_block",
            "minecraft:amethyst_block", "minecraft:budding_amethyst",
            "minecraft:sculk", "minecraft:sculk_catalyst", "minecraft:sculk_sensor",
            "minecraft:spawner", "minecraft:trial_spawner"
    };

    static {
        for (String id : COMMON_IDS) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
            if (block != null && block != Blocks.AIR) {
                COMMON.add(block);
            }
        }
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (block != null && block != Blocks.AIR) {
                ALL.add(block);
            }
        }
        ALL.sort(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).getPath()));
    }

    private InfiniteXRayBlocks() {
    }

    /**
     * Returns blocks matching the query. Matches against the registry ID as
     * well as the localized display name, so both "diamond_ore" and "钻石矿"
     * work.
     */
    public static List<Block> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            List<Block> result = new ArrayList<>(COMMON);
            Set<Block> seen = new HashSet<>(COMMON);
            for (Block block : ALL) {
                if (seen.add(block)) {
                    result.add(block);
                }
            }
            return result;
        }
        List<Block> result = new ArrayList<>();
        for (Block block : ALL) {
            if (matches(block, q)) {
                result.add(block);
            }
        }
        return result;
    }

    private static boolean matches(Block block, String q) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id.getNamespace().contains(q) || id.getPath().contains(q)) {
            return true;
        }
        String name = block.getName().getString().toLowerCase(Locale.ROOT);
        return name.contains(q);
    }
}
