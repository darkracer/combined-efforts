package com.darkracers.combinedefforts.tileentity;

import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyReceiver;
import com.darkracers.combinedefforts.interfaces.INeighborUpdatableEntity;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityPowerInlet extends TileEntityServerPart implements IEnergyHandler, INeighborUpdatableEntity {

    IEnergyReceiver rgNetwork;

    public TileEntityPowerInlet(){
        super();

        rgNetwork = null;
    }

    @Override
    public int receiveEnergy(ForgeDirection forgeDirection, int i, boolean b) {
        return 0;
    }

    @Override
    public int extractEnergy(ForgeDirection forgeDirection, int i, boolean b) {
        return 0;
    }

    @Override
    public int getEnergyStored(ForgeDirection forgeDirection) {
        return 0;
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection forgeDirection) {
        return 0;
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection forgeDirection) {
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighborBlock) {

    }

    @Override
    public void onNeigborBlockChange(World world, int x, int y, int z, int neighborX, int neighborY, int neighBorZ) {

    }
}
