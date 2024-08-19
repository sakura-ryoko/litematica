package fi.dy.masa.litematica.util;

import net.minecraft.block.Block;
import net.minecraft.registry.tag.TagKey;

import java.util.ArrayList;
import java.util.List;
public class SubstituteBlockRegestry {
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

    public SubstituteBlockRegestry(List<Block> blocks, List<TagKey<Block>> blockTags) {
        this.blocks = blocks;
        this.blockTags = blockTags;
    }

    public SubstituteBlockRegestry() {
        this.blocks = new ArrayList<>();
        this.blockTags = new ArrayList<>();
    }
}