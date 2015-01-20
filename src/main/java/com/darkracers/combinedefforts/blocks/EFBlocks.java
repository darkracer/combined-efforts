package com.darkracers.combinedefforts.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class EFBlocks {
    public static Block frame;

    public static void init(){
        frame = new BlockServerPart(Material.iron);

    }
}
