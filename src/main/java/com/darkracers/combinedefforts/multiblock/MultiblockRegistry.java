package com.darkracers.combinedefforts.multiblock;

import com.darkracers.combinedefforts.interfaces.IMultiblockPart;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Set;

public class MultiblockRegistry {

    private static HashMap<World, MultiblockWorldRegistry> registries = new HashMap<World, MultiblockWorldRegistry>();

    public static void tickStart(World world){
        if (registries.containsKey(world)){
            MultiblockWorldRegistry registry = registries.get(world);
            registry.tickStart();
        }
    }

    public static void tickEnd(World world){
        if (registries.containsKey(world)){
            MultiblockWorldRegistry registry = registries.get(world);
            registry.tickEnded();
        }
    }

    public static void onChunkLoaded(World world, int chunkX, int chunkZ){
        if (registries.containsKey(world)){
            registries.get(world).onChunkLoaded(chunkX, chunkX);
        }
    }

    public static void onPartAdded(World world, IMultiblockPart part){
        MultiblockWorldRegistry registry = getOrCreateRegistry(world);
        registry.onPartAdded(part);
    }

    public static void onPartRemovedFromWorld(World world, IMultiblockPart part){
        if (registries.containsKey(world)){
            registries.get(world).onPartRemovedFromWorld(part);
        }
    }

    public static void onWorldUnloaded(World world){
        if (registries.containsKey(world)){
            registries.get(world).onWorldUnloaded();
            registries.remove(world);
        }
    }

    public static void addDirtyController(World world, MultiblockControllerBase controller){
        if (registries.containsKey(world)){
            registries.get(world).addDirtyController(controller);
        }
        else {
            throw new IllegalArgumentException("Adding a dirty controller to a world that has no registered controllers!");
        }
    }

    public static void addDeadController(World world, MultiblockControllerBase controller){
        if (registries.containsKey(world)){
            registries.get(world).addDeadController(controller);
        }
        else {
            System.out.println(String.format("Controller %d in world %s marked as dead, but that world is not tracked! Controller is being ignored.", controller.hashCode(), world));
        }
    }

    public static Set<MultiblockControllerBase> getControllersFromWorld(World world){
        if (registries.containsKey(world)){
            return registries.get(world).getControllers();
        }
        return null;
    }

    private static MultiblockWorldRegistry getOrCreateRegistry(World world){
        if (registries.containsKey(world)){
            return registries.get(world);
        }
        else {
            MultiblockWorldRegistry newRegistry = new MultiblockWorldRegistry(world);
            registries.put(world, newRegistry);
            return newRegistry;
        }
    }
}
