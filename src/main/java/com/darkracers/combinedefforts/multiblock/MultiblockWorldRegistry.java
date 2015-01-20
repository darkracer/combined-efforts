package com.darkracers.combinedefforts.multiblock;

import com.darkracers.combinedefforts.interfaces.IMultiblockPart;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import java.util.*;

public class MultiblockWorldRegistry {

    private World worldObj;

    private Set<MultiblockControllerBase> controllers;
    private Set<MultiblockControllerBase> dirtyControllers;
    private Set<MultiblockControllerBase> deadControllers;

    private Set<IMultiblockPart> orphanedParts;

    private Set<IMultiblockPart> detachedParts;

    private HashMap<Long, Set<IMultiblockPart>> partsAwaitingChunkLoad;

    private Object partsAwaitingChunkLoadMutex;
    private Object orphanedPartsMutex;

    public MultiblockWorldRegistry(World world){
        worldObj = world;

        controllers = new HashSet<MultiblockControllerBase>();
        deadControllers = new HashSet<MultiblockControllerBase>();
        dirtyControllers = new HashSet<MultiblockControllerBase>();

        detachedParts = new HashSet<IMultiblockPart>();
        orphanedParts = new HashSet<IMultiblockPart>();

        partsAwaitingChunkLoad = new HashMap<Long, Set<IMultiblockPart>>();
        partsAwaitingChunkLoadMutex = new Object();
        orphanedPartsMutex = new Object();
    }

    public void tickStart() {
        if (controllers.size() > 0){
            for (MultiblockControllerBase controller: controllers){
                if (controller.worldObj == worldObj && controller.worldObj.isRemote == worldObj.isRemote){
                    if (controller.isEmpty()){
                        deadControllers.add(controller);
                    }else {
                        controller.updateMultiblockEntity();
                    }
                }
            }
        }
    }

    public void tickEnded() {
        IChunkProvider chunkProvider = worldObj.getChunkProvider();
        CoordTriplet coord;

        List<Set<MultiblockControllerBase>> mergePools = null;
        if (orphanedParts.size() > 0){
            Set<IMultiblockPart> orphansToProcess = null;

            synchronized (orphanedPartsMutex) {
                if (orphanedParts.size() > 0){
                    orphansToProcess = orphanedParts;
                    orphanedParts = new HashSet<IMultiblockPart>();
                }
            }

            if (orphansToProcess != null && orphansToProcess.size() > 0){
                Set<MultiblockControllerBase> compatibleControllers;

                for (IMultiblockPart orphan : orphansToProcess) {
                    coord = orphan.getWorldLocation();
                    if (!chunkProvider.chunkExists(coord.getChunkX(), coord.getChunkZ())){
                        continue;
                    }

                    if (orphan.isInvalid()) { continue; }

                    if (worldObj.getTileEntity(coord.x, coord.y, coord.z) != orphan){
                        continue;
                    }

                    compatibleControllers = orphan.attachToNeighbors();
                    if (compatibleControllers == null) {
                        MultiblockControllerBase newController = orphan.createNewMultiblock();
                        newController.attachBlock(orphan);
                        this.controllers.add(newController);
                    }
                    else if (compatibleControllers.size() > 1){
                        if (mergePools == null) {mergePools = new ArrayList<Set<MultiblockControllerBase>>();}

                        boolean hasAddedToPool = false;
                        List<Set<MultiblockControllerBase>> candidatePools = new ArrayList<Set<MultiblockControllerBase>>();
                        for (Set<MultiblockControllerBase> candidatePool : mergePools) {
                            if (!Collections.disjoint(candidatePool, compatibleControllers)){
                                candidatePools.add(candidatePool);
                            }
                        }

                        if (candidatePools.size() <= 0) {
                            mergePools.add(compatibleControllers);
                        }
                        else if (candidatePools.size() == 1){
                            candidatePools.get(0).addAll(compatibleControllers);
                        }
                        else{
                            Set<MultiblockControllerBase> masterPool = candidatePools.get(0);
                            Set<MultiblockControllerBase> consumedPool;
                            for (int i = 1; i < candidatePools.size(); i++){
                                consumedPool = candidatePools.get(i);
                                masterPool.addAll(consumedPool);
                                mergePools.remove(consumedPool);
                            }
                            masterPool.addAll(compatibleControllers);
                        }
                    }
                }
            }
        }

        if (mergePools != null && mergePools.size() > 0){
            for (Set<MultiblockControllerBase> mergePool: mergePools){
                MultiblockControllerBase newMaster = null;
                for (MultiblockControllerBase controller : mergePool){
                    if (newMaster == null || controller.shouldConsume(newMaster)){
                        newMaster = controller;
                    }
                }

                if (newMaster == null){
                    System.out.println(String.format("Multiblock system checked a merge pool of size %d, found no master candidates. This should never happen.", mergePool.size()));
                }
                else {
                    addDirtyController(newMaster);
                    for (*MultiblockControllerBase controller: mergePool){
                        if (controller != newMaster){
                            newMaster.assimilate(controller);
                            addDeadController(controller);
                            addDirtyController(newMaster);
                        }
                    }
                }

            }
        }
    }
}
