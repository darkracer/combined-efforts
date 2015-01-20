package com.darkracers.combinedefforts.tileentity;

import com.darkracers.combinedefforts.interfaces.IMultiblockPart;
import com.darkracers.combinedefforts.multiblock.CoordTriplet;
import com.darkracers.combinedefforts.multiblock.MultiblockControllerBase;
import com.darkracers.combinedefforts.multiblock.MultiblockRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class MultiblockTileEntityBase extends IMultiblockPart {
    private MultiblockControllerBase controller;
    private boolean visited;

    private boolean saveMultiblockData;
    private NBTTagCompound cashedMultiblockData;
    private boolean paused;

    public MultiblockTileEntityBase(){
        super();
        controller = null;
        visited = false;
        saveMultiblockData = false;
        paused = false;
        cashedMultiblockData = null;
    }

    @Override
    public Set<MultiblockControllerBase> attachToNeighbors() {
        Set<MultiblockControllerBase> controllers = null;
        MultiblockControllerBase bestController = null;

        IMultiblockPart[] partsToCheck = getNeighboringParts();
        for (IMultiblockPart neighborPart : partsToCheck){
            if (neighborPart.isConnected()){
                MultiblockControllerBase candidate = neighborPart.getMultiblockController();
                if (!candidate.getClass().equals(this.getMultiblockControllerType())){
                    continue;
                }

                if (controllers == null){
                    controllers = new HashSet<MultiblockControllerBase>();
                    bestController = candidate;
                }
                else if (!controllers.contains(candidate) && candidate.shouldConsume(bestController)){
                    bestController = candidate;
                }

                controllers.add(candidate);
            }
        }

        if (bestController != null){
            this.controller = bestController;
            bestController.attachBlock(this);
        }
        return controllers;
    }

    @Override
    public void assertDetached(){
        if (this.controller != null){
            System.out.println(String.format("[assert] Part @ (%d, %d, %d) should be detached already, but detected that it was not. This is not a fatal error, and will be repaired, but is unusual.", xCoord, yCoord, zCoord));
            this.controller = null;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound data){
        super.readFromNBT(data);

        if (data.hasKey("multiblockData")){
            this.cashedMultiblockData = data.getCompoundTag("multiblockData");
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound data){
        super.writeToNBT(data);

        if (isMultiblockSaveDelegate() && isConnected()){
            NBTTagCompound multiblockData = new NBTTagCompound();
            this.controller.writeToNBT(multiblockData);
            data.setTag("multiblockData", multiblockData);
        }
    }

    @Override
    public boolean canUpdate() {return false;}

    @Override
    public void invalidate(){
        super.invalidate();
        detachSelf(false);
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        detachSelf(true);
    }

    @Override
    public void validate() {
        super.validate();
        MultiblockRegistry.onPartAdded(this.worldObj, this);
    }

    @Override
    public S35PacketUpdateTileEntity getDescriptionPacket() {
        NBTTagCompound packetData = new NBTTagCompound();
        encodeDescriptionPacket(packetData);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, packetData);
    }

    @Override
    public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet){
        decodeDescriptionPacket(packet.func_148857_g());
    }

    protected void encodeDescriptionPacket(NBTTagCompound packetData){
        if (this.isMultiblockSaveDelegate() && isConnected()){
            NBTTagCompound tag = new NBTTagCompound();
            getMultiblockController().formatDescriptionPacket(tag);
            packetData.setTag("multiblockData", tag);
        }
    }

    public void decodeDescriptionPacket(NBTTagCompound packetData){
        if (packetData.hasKey("multiblockData")){
            NBTTagCompound tag = packetData.getCompoundTag("multiblockData");
            if (isConnected()){
                getMultiblockController().decodeDescriptionPacket(tag);
            }
            else {
                this.cashedMultiblockData = tag;
            }
        }
    }

    @Override
    public boolean hasMultiblockSaveData() {
        return this.cashedMultiblockData != null;
    }

    @Override
    public NBTTagCompound getMultiblockSaveData() {
        return this.cashedMultiblockData;
    }

    @Override
    public void onMultiblockDataAssimilated() {
        this.cashedMultiblockData = null;
    }

    @Override
    public abstract  void onMachineAssembled(MultiblockControllerBase multiblockControllerBase);

    @Override
    public abstract void onMachineBroken();

    @Override
    public abstract void onMachineActivated();

    @Override
    public abstract void onMachineDeactivated();

    ///// Miscellaneous multiblock-assembly callbacks and support methods (IMultiblockPart)

    @Override
    public boolean isConnected() {
        return (controller != null);
    }

    @Override
    public MultiblockControllerBase getMultiblockController() {
        return controller;
    }

    @Override
    public CoordTriplet getWorldLocation() {
        return new CoordTriplet(this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public void becomeMultiblockSaveDelegate() {
        this.saveMultiblockData = true;
    }

    @Override
    public void forfeitMultiblockSaveDelegate() {
        this.saveMultiblockData = false;
    }

    @Override
    public boolean isMultiblockSaveDelegate() { return this.saveMultiblockData; }

    @Override
    public void setUnvisited() {
        this.visited = false;
    }

    @Override
    public void setVisited() {
        this.visited = true;
    }

    @Override
    public boolean isVisited() {
        return this.visited;
    }

    @Override
    public void onAssimilated(MultiblockControllerBase newController) {
        assert(this.controller != newController);
        this.controller = newController;
    }

    @Override
    public void onAttached(MultiblockControllerBase newController) {
        this.controller = newController;
    }

    @Override
    public void onDetached(MultiblockControllerBase oldController) {
        this.controller = null;
    }

    @Override
    public abstract MultiblockControllerBase createNewMultiblock();

    @Override
    public World getWorldObj() {
        return super.getWorldObj();
    }

    @Override
    public IMultiblockPart[] getNeighboringParts() {
        CoordTriplet[] neighbors = new CoordTriplet[] {
                new CoordTriplet(this.xCoord-1, this.yCoord, this.zCoord),
                new CoordTriplet(this.xCoord, this.yCoord-1, this.zCoord),
                new CoordTriplet(this.xCoord, this.yCoord, this.zCoord-1),
                new CoordTriplet(this.xCoord, this.yCoord, this.zCoord+1),
                new CoordTriplet(this.xCoord, this.yCoord+1, this.zCoord),
                new CoordTriplet(this.xCoord+1, this.yCoord, this.zCoord)
        };

        TileEntity te;
        List<IMultiblockPart> neighborParts = new ArrayList<IMultiblockPart>();
        IChunkProvider chunkProvider = worldObj.getChunkProvider();
        for(CoordTriplet neighbor : neighbors) {
            if(!chunkProvider.chunkExists(neighbor.getChunkX(), neighbor.getChunkZ())) {
                // Chunk not loaded, skip it.
                continue;
            }

            te = this.worldObj.getTileEntity(neighbor.x, neighbor.y, neighbor.z);
            if(te instanceof IMultiblockPart) {
                neighborParts.add((IMultiblockPart)te);
            }
        }
        IMultiblockPart[] tmp = new IMultiblockPart[neighborParts.size()];
        return neighborParts.toArray(tmp);
    }

    @Override
    public void onOrphaned(MultiblockControllerBase controller, int oldSize, int newSize) {
        this.markDirty();
        worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this);
    }

    //// Helper functions for notifying neighboring blocks
    protected void notifyNeighborsOfBlockChange() {
        worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType());
    }

    protected void notifyNeighborsOfTileChange() {
        worldObj.func_147453_f(xCoord, yCoord, zCoord, getBlockType());
    }

    protected void detachSelf(boolean chunkUnloading){
        if (this.controller != null){
            this.controller.detachBlock(this, chunkUnloading);

            this.controller = null;
        }

        MultiblockRegistry.onPartRemovedFromWorld(worldObj, this);
    }
}
