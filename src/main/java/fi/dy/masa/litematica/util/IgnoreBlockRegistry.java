package fi.dy.masa.litematica.util;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import net.minecraft.block.Block;
import net.minecraft.registry.tag.TagKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IgnoreBlockRegistry {
    private final List<Block> blocks;
    private final List<TagKey<Block>> blockTags;

    public boolean hasBlock(Block block) {
        if (this.blocks.contains(block)) {
            return true;
        } else {
            for (TagKey<Block> tag:
                    this.blockTags){
                if (block.getDefaultState().isIn(tag)) return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return this.blocks.isEmpty() && this.blockTags.isEmpty();
    }

    public IgnoreBlockRegistry(ConfigBoolean ignoreBlocks, ConfigStringList ignorableBlocks) {
        this.blocks = new ArrayList<>();
        this.blockTags = new ArrayList<>();

        if (ignoreBlocks.getBooleanValue()) {
            for (String value : ignorableBlocks.getStrings()) {
                String trimmed = value.trim();
                if (trimmed.startsWith("#")) {
                    Optional<TagKey<Block>> tag = BlockUtils.getBlockTagFromString(trimmed);
                    tag.ifPresent(this.blockTags::add);
                } else {
                    Optional<Block> block = BlockUtils.getBlockFromString(trimmed);
                    block.ifPresent(this.blocks::add);
                }
            }
        }
    }
}
