package com.darkracers.combinedefforts.tileentity;

import com.darkracers.combinedefforts.multiblock.CoordTriplet;
import com.darkracers.combinedefforts.multiblock.MultiblockControllerBase;
import com.darkracers.combinedefforts.multiblock.PartPosition;
import net.minecraftforge.common.util.ForgeDirection;

public class RectangularMultiblockTileEntityBase extends MultiblockTileEntityBase{
    PartPosition position;
    ForgeDirection outwards;

    public RectangularMultiblockTileEntityBase() {
        super();
        position = PartPosition.Unknown;
        outwards = ForgeDirection.UNKNOWN;
    }

    public ForgeDirection getOutwardsDir() {
        return outwards;
    }

    public PartPosition getPartPosition() {
        return position;
    }

    @Override
    public void onAttached(MultiblockControllerBase newController){
        super.onAttached(newController);
        recalculateOutwardsDirection(newController.getMinimumCoord(), newController.getMaximumCoord());
    }

    @Override
    public void onMachineAssembled(MultiblockControllerBase controller){
        CoordTriplet maxCoord = controller.getMaximumCoord();
        CoordTriplet minCoord = controller.getMinimumCoord();

        recalculateOutwardsDirection(minCoord, maxCoord);
    }

    @Override
    public void onMachineBroken(){
        position = PartPosition.Unknown;
        outwards = ForgeDirection.UNKNOWN;
    }

    @Override
    public void recalculateOutwardsDirection(CoordTriplet minCoord, CoordTriplet maxCoord){
        outwards = ForgeDirection.UNKNOWN;
        position = PartPosition.Unknown;

        int facesMatching = 0;
        if (maxCoord.x == this.xCoord || minCoord.x == this.xCoord) { facesMatching++;}
        if (maxCoord.y == this.yCoord || minCoord.y == this.yCoord) { facesMatching++;}
        if (maxCoord.z == this.zCoord || minCoord.z == this.zCoord) { facesMatching++;}

        if (facesMatching <= 0) {position = PartPosition.Interior;
    }
}
