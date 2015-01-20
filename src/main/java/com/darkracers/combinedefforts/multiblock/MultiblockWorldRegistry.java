package com.darkracers.combinedefforts.multiblock;

import com.darkracers.combinedefforts.interfaces.IMultiblockPart;
import net.minecraft.world.ChunkCoordIntPair;
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
                    for (MultiblockControllerBase controller: mergePool){
                        if (controller != newMaster){
                            newMaster.assimilate(controller);
                            addDeadController(controller);
                            addDirtyController(newMaster);
                        }
                    }
                }
            }
        }

        if (dirtyControllers.size() > 0) {
            Set<IMultiblockPart> newlyDetachedParts = null;
            for (MultiblockControllerBase controller : dirtyControllers){
                newlyDetachedParts = controller.checkForDisconnections();

                if (!controller.isEmpty()){
                    controller.recalculateMinMaxCoords();
                    controller.checkIfMachineIsWhole();
                }
                else {
                    addDeadController(controller);
                }

                if (newlyDetachedParts != null && newlyDetachedParts.size() > 0){
                    detachedParts.addAll(newlyDetachedParts);
                }
            }

            dirtyControllers.clear();
        }

        if (deadControllers.size() > 0){
            for (MultiblockControllerBase controller : deadControllers){
                if (!controller.isEmpty()){
                    System.out.println("Found a non-empty controller. Forcing it to shed its blocks and die. This should never happen!)");
                    detachedParts.addAll(controller.detachAllBlocks());
                }

                this.controllers.remove(controller);
            }

            deadControllers.clear();
        }

        for (IMultiblockPart part : detachedParts){
            part.assertDetached();
        }

        addAllOrphanedPartsThreadsafe(detachedParts);
        detachedParts.clear();
    }

    public void onPartAdded(IMultiblockPart part){
        CoordTriplet worldLocation = part.getWorldLocation();

        if (!worldObj.getChunkProvider().chunkExists(worldLocation.getChunkX(), worldLocation.getChunkZ())){
            Set<IMultiblockPart> partSet;
            long chunkHash = worldLocation.getChunkXZHash();
            synchronized (partsAwaitingChunkLoadMutex){
                if (!partsAwaitingChunkLoad.containsKey(chunkHash)){
                    partSet = new HashSet<IMultiblockPart>();
                    partsAwaitingChunkLoad.put(chunkHash, partSet);
                }
                else {
                    partSet = partsAwaitingChunkLoad.get(chunkHash);
                }

                partSet.add(part);
            }
        }
        else {
            addOrphanedPartsThreadsafe(part);
        }
    }

    public void onPartRemovedFromWorld(IMultiblockPart part){
        CoordTriplet coord = part.getWorldLocation();
        if (coord != null) {
            long hash = coord.getChunkXZHash();

            if (partsAwaitingChunkLoad.containsKey(hash)){
                synchronized (partsAwaitingChunkLoadMutex) {
                    if (partsAwaitingChunkLoad.containsKey(hash)){
                        partsAwaitingChunkLoad.get(hash).remove(part);
                        if (partsAwaitingChunkLoad.get(hash).size() <= 0){
                            partsAwaitingChunkLoad.remove(hash);
                        }
                    }
                }
            }
        }

        detachedParts.remove(part);
        if (orphanedParts.contains(part)){
            synchronized (orphanedPartsMutex) {
                orphanedParts.remove(part);
            }
        }

        part.assertDetached();
    }

    public void onWorldUnloaded() {
        controllers.clear();
        deadControllers.clear();
        dirtyControllers.clear();

        detachedParts.clear();

        synchronized (partsAwaitingChunkLoadMutex) {
            partsAwaitingChunkLoad.clear();
        }

        synchronized (orphanedPartsMutex) {
            orphanedParts.clear();
        }

        worldObj = null;
    }

    public void onChunkLoaded(int chunkX, int chunkZ) {
        long chunkHash = ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ);
        if (partsAwaitingChunkLoad.containsKey(chunkHash)){
            synchronized (partsAwaitingChunkLoadMutex) {
                if (partsAwaitingChunkLoad.containsKey(chunkHash)){
                    addAllOrphanedPartsThreadsafe(partsAwaitingChunkLoad.get(chunkHash));
                    partsAwaitingChunkLoad.remove(chunkHash);
                }
            }
        }
    }

    public void addDeadController(MultiblockControllerBase deadController){
        this.deadControllers.add(deadController);
    }

    public void addDirtyController(MultiblockControllerBase dirtyController) {
        this.dirtyControllers.add(dirtyController);
    }

    public Set<MultiblockControllerBase> getControllers(){
        return Collections.unmodifiableSet(controllers);
    }

    private void addOrphanedPartsThreadsafe(IMultiblockPart part){
        synchronized (orphanedPartsMutex) {
            orphanedParts.add(part);
        }
    }

    private void addAllOrphanedPartsThreadsafe(Collection<? extends IMultiblockPart> parts){
        synchronized (orphanedPartsMutex){
            orphanedParts.addAll(parts);
        }
    }

    private String clientOrServer() { return worldObj.isRemote ? "CLIEN" : "SERVER";}
}
