package com.darkracers.combinedefforts.interfaces;

import com.darkracers.combinedefforts.multiblock.CoordTriplet;
import com.darkracers.combinedefforts.multiblock.MultiblockControllerBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.util.Set;

public abstract class IMultiblockPart extends TileEntity{
    public static final int INVALID_DISTANCE = Integer.MAX_VALUE;

    public abstract boolean isConnected();

    public abstract MultiblockControllerBase getMultiblockController();

    public abstract CoordTriplet getWorldLocation();

    public abstract void onAttached(MultiblockControllerBase newController);

    public abstract void onDetached(MultiblockControllerBase multiblockController);

    public abstract void onOrphaned(MultiblockControllerBase oldController, int oldControllerSize, int newControllerSize);

    public abstract MultiblockControllerBase createNewMultiblock();

    public abstract Class<? extends MultiblockControllerBase> getMultiblockControllerType();

    public abstract void onAssimilated(MultiblockControllerBase newController);

    public abstract void setVisited();

    public abstract void setUnvisited();

    public abstract boolean isVisited();

    public abstract void becomeMultiblockSaveDelegate();

    public abstract void forfeitMultibockSaveDelegate();

    public abstract boolean isMultiblockSaveDelegate();

    public abstract IMultiblockPart[] getNeighboringParts();

    public abstract void onMachineAssembled(MultiblockControllerBase multiblockControllerBase);

    public abstract void onMachineBroken();

    public abstract void onMachineActivated();

    public abstract void onMachineDeactivated();

    public abstract Set<MultiblockControllerBase> attachToNeighbors();

    public abstract void assertDetached();

    public abstract boolean hasMultiblockSaveData();

    public abstract NBTTagCompound getMultiblockSaveData();

    public abstract void onMultiblockDataAssimilated();


}
