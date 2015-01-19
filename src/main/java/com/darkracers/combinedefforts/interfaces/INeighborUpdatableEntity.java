package com.darkracers.combinedefforts.interfaces;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public interface INeighborUpdatableEntity {

    public void onNeighborBlockChange(World world,int x, int y, int z, Block neighborBlock);

    public void onNeigborBlockChange(World world, int x, int y, int z, int neighborX, int neighborY, int neighBorZ);
}
