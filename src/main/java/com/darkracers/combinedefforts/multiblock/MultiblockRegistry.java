package com.darkracers.combinedefforts.multiblock;

import net.minecraft.world.World;

import java.util.HashMap;

public class MultiblockRegistry {

    private static HashMap<World, MultiblockWorldRegistry> registries = new HashMap<World, MultiblockWorldRegistry>();

    public static void tickStart(World world){
        if (registries.containsKey(world)){
            MultiblockWorldRegistry registry = registries.get(world);
            registry.tickStart();
        }
    }
}
