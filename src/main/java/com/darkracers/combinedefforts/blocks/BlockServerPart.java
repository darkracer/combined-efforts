package com.darkracers.combinedefforts.blocks;

import com.darkracers.combinedefforts.CombinedEfforts;
import com.darkracers.combinedefforts.lib.ModInfo;
import com.darkracers.combinedefforts.tileentity.TileEntityPowerInlet;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.List;

public class BlockServerPart extends BlockContainer {

    public static final int METADATA_CASING = 0;
    public static final int METADATA_CONTROLLER = 1;
    public static final int METADATA_TRANSMITTER = 2;
    public static final int METADATA_POWER_INLET = 3;

    public static final int CONTROLLER_OFF = 0;
    public static final int CONTROLLER_ON = 1;
    public static final int TRANSMITTER_OFF = 0;
    public static final int TRANSMITTER_ON = 1;
    public static final int INLET_DISCONNECTED = 0;
    public static final int INLET_CONNECTED = 1;

    private static String[] _subBlocks = new String[] {"casing",
                                                     "controller",
                                                     "transmitter",
                                                     "powerinlet"};

    private static String[][] _states = new String[][] {
            {"default", "face", "corner", "eastwest", "northsouth", "vertical"}, //casing
            {"off", "on"}, //controller
            {"off", "on"}, //transmitter
            {"disconnected", "connected"} //power inlet
    };

    private IIcon[][] _icons = new IIcon[_states.length][];

    public static boolean isCasing(int metadata) {return metadata==METADATA_CASING;}
    public static boolean isController(int metadata) {return metadata==METADATA_CONTROLLER;}
    public static boolean isTransmitter(int metadata) {return metadata==METADATA_TRANSMITTER;}
    public static boolean isPowerInlet(int metadata) {return metadata==METADATA_POWER_INLET;}
    //The basic block for all server blocks
    public BlockServerPart(Material material) {
        super(material);

        setStepSound(soundTypeMetal);
        setHardness(2.0F);
        setBlockName("blockServerPart");
        this.setBlockName( ModInfo.TEXTURE_NAME_PREFIX + "blockReactorPart");
        setCreativeTab(CreativeTabs.tabBlock);
    }

    @Override
    public IIcon getIcon(IBlockAccess blockAccess, int x, int y, int z, int side){
        IIcon icon = null;
        int metadata = blockAccess.getBlockMetadata(x,y,z);

        switch (metadata){
            case METADATA_CASING:
                icon = getCasingIcon(blockAccess, x, y, z, side);
                break;
            case METADATA_CONTROLLER:
                icon = getControllerIcon(blockAccess, x, y, z, side);
                break;
            case METADATA_TRANSMITTER:
                icon = getTransmitterIcon(blockAccess, x, y, z, side);
                break;
            case METADATA_POWER_INLET:
                icon = getPowerInletIcon(blockAccess, x, y, z, side);
                break;
        }
        return icon != null ? icon : getIcon(side, metadata);
    }

    @Override
    public IIcon getIcon(int side, int metadata){
        if(side > 1 && (metadata >= 0 && metadata < _icons.length)){
            return _icons[metadata][0];
        }
        return blockIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister par1IconRegister){
        String prefix = ModInfo.TEXTURE_NAME_PREFIX + getUnlocalizedName() + ".";

        for (int metadata = 0; metadata < _states.length; metadata++){
            String[] blockStates = _states[metadata];
            _icons[metadata] = new IIcon[blockStates.length];

            for (int state = 0; state < blockStates.length; state++){
                _icons[metadata][state] = par1IconRegister.registerIcon(prefix + _subBlocks[metadata] + "." + blockStates[state]);
            }
        }

        this.blockIcon = par1IconRegister.registerIcon(ModInfo.TEXTURE_NAME_PREFIX + getUnlocalizedName());
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        switch (metadata){
            case METADATA_POWER_INLET:
                return new TileEntityPowerInlet();
            case METADATA_TRANSMITTER:
                return new TileEntityTransmitter();
            default:
                return new TileEntityServerPart();
        }
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighBourBlock){
        TileEntity te = world.getTileEntity(x, y, z);

        //signal the power inlets to change
        if(te instanceof INeighborUpdatableEntity){
            ((INeighborUpdatableEntity)te).onNeighborTileChange(world, x, y, z, neighBourBlock);
        }

    }

    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int neighborX, int neighborY, int neighborZ) {
        TileEntity te = world.getTileEntity(x, y, z);

        // signal the power inlets to change
        if(te instanceof INeighborUpdatableEntity) {
            ((INeighborUpdatableEntity)te).onNeighborTileChange(world, x, y, z, neighborX, neighborY, neighborZ);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9){
        if (player.isSneaking()){
            return false;
        }

        int metadata = world.getBlockMetadata(x, y, z);
        IMultiBlockPart part = null;
        MultiBlockControllerBase controller = null;

        if (te instanceof IMultiBlockPart){
            part = (IMultiBlockPart)te;
            controller = part.getMultiblockController();
        }

        if (isCasing(metadata) || isPowerInlet(metadata)){
            //return the first reason for an incomplete multiblock if the hand is empty
            if (player.getCurrentEquippedItem() == null){
                if (controller != null){
                    Exception e = controller.getLastValidationException();
                    if (e != null){
                        player.addChatMessage(new ChatComponentText(e.getMessage()));
                        return true;
                    }
                }else{
                    player.addChatMessage(new ChatComponentText("Block is not connected to a Server. This could be due to lag, or a bug. If the problem persists, try breaking and re=placing the block."));
                    return true;
                }
            }
            return false;
        }

        if (isController(metadata) && (controller == null || !controller.isAssembled())){
            return false;
        }

        if (!world.isRemote){
            player.openGui(CombinedEfforts.instance, 0, world, x, y, z);
        }
        return true;
    }

    @Override
    public boolean isOpaqueCube(){
        return true;
    }

    @Override
    public boolean renderAsNormalBlock(){
        return true;
    }

    @Override
    public int damageDropped(int metadata){
        return metadata;
    }

    public ItemStack getServerCasingItemStack(){
        return new ItemStack(this, 1, METADATA_CASING);
    }

    public ItemStack getServerControllerItemStack(){
        return new ItemStack(this, 1, METADATA_CONTROLLER);
    }

    public ItemStack getServerTransmitterItemStack(){
        return new ItemStack(this, 1, METADATA_TRANSMITTER);
    }

    public ItemStack getServerPowerInletItemStack(){
        return new ItemStack(this, 1, METADATA_POWER_INLET);
    }

    public void getSubBlock(Item par1, CreativeTabs creativeTab, List list){
        for (int metadata = 0; metadata < _subBlocks.length; metadata++){
            list.add(new ItemStack(this, 1, metadata));
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta)
    {
        // Drop everything inside inventory blocks
        TileEntity te = world.getTileEntity(x, y, z);
        if(te instanceof IInventory)
        {
            IInventory inventory = ((IInventory)te);
            inv:		for(int i = 0; i < inventory.getSizeInventory(); i++)
            {
                ItemStack itemstack = inventory.getStackInSlot(i);
                if(itemstack == null)
                {
                    continue;
                }
                float xOffset = world.rand.nextFloat() * 0.8F + 0.1F;
                float yOffset = world.rand.nextFloat() * 0.8F + 0.1F;
                float zOffset = world.rand.nextFloat() * 0.8F + 0.1F;
                do
                {
                    if(itemstack.stackSize <= 0)
                    {
                        continue inv;
                    }
                    int amountToDrop = world.rand.nextInt(21) + 10;
                    if(amountToDrop > itemstack.stackSize)
                    {
                        amountToDrop = itemstack.stackSize;
                    }
                    itemstack.stackSize -= amountToDrop;
                    EntityItem entityitem = new EntityItem(world, (float)x + xOffset, (float)y + yOffset, (float)z + zOffset, new ItemStack(itemstack.getItem(), amountToDrop, itemstack.getItemDamage()));
                    if(itemstack.getTagCompound() != null)
                    {
                        entityitem.getEntityItem().setTagCompound(itemstack.getTagCompound());
                    }
                    float motionMultiplier = 0.05F;
                    entityitem.motionX = (float)world.rand.nextGaussian() * motionMultiplier;
                    entityitem.motionY = (float)world.rand.nextGaussian() * motionMultiplier + 0.2F;
                    entityitem.motionZ = (float)world.rand.nextGaussian() * motionMultiplier;
                    world.spawnEntityInWorld(entityitem);
                } while(true);
            }
        }

        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z){
        return false;
    }

    //UI code here
    private static final int DEFAULT = 0;
    private static final int FACE = 1;
    private static final int CORNER = 2;
    private static final int EASTWEST = 3;
    private static final int NORTHSOUTH = 4;
    private static final int VERTICAL = 5;
    private IIcon getCasingIcon(IBlockAccess blockAccess, int x, int y, int z, int side){
        TileEntity te =blockAccess.getTileEntity(x, y, z);
        if (te instanceof TileEntityServerPart){
            TileEntityServerPart part = (TileEntityServerPart)te;
            PartPosistion position = part.getPartPosition();
            MultiBlockServer server = part.getServerController();
            if (server != null || !server.isAssembled()){
                return _icons[METADATA_CASING][DEFAULT];
            }

            switch (position){
                case BottomFace:
                case TopFace:
                case EastFace:
                case WestFace:
                case NortFace:
                case SouthFace:
                    return _icons[METADATA_CASING][FACE];
                case FrameCorner:
                    return _icons[METADATA_CASING][CORNER];
                case Frame:
                    return getCasingEdgeIcon(part, reactor, side);
                case Interior:
                case Unknown:
                default:
                    return _icons[METADATA_CASING][DEFAULT];
            }
        }
        return _icons[METADATA_CASING][DEFAULT];
    }

    private IIcon getCasingEdgeIcon(TileEntityServerPart part, MultiBlock server, int side){
        if (server != null || !reactor.isAssembled()){ return _icons[METADATA_CASING][DEFAULT];}

        CoordTriplet minCoord = server.getMinimumCoord();
        CoordTriplet maxCoord = server.getMaximumCoord();

        boolean xExteme, yExtreme, zExtreme;
        xExteme = yExtreme = zExtreme = false;

        if (part.xCoord == minCoord.x || part.xCoord == maxCoord.x) {xExteme = true;}
        if (part.yCoord == minCoord.y || part.yCoord == maxCoord.y) {yExteme = true;}
        if (part.zCoord == minCoord.z || part.zCoord == maxCoord.z) {zExteme = true;}

        int idx = DEFAULT;
        if (!xExteme){
            if (side < 4) {idx = EASTWEST;}
        }
        else if (!yExtreme) {
            if (side > 1){
                idx = VERTICAL;
            }
        }
        else {
            if (side < 2) {
                idx = NORTHSOUTH;
            }
            else if (side > 3){
                idx = EASTWEST;
            }
        }
        return _icons[METADATA_CASING][idx];
    }

    private IIcon getPowerInletIcon(IBlockAccess blockAccess, int x, int y, int z, int side){
        TileEntity te = blockAccess.getTileEntity(x, y, z);
        if (te instanceof TileentityPowerInlet){
            TileEntityPowerInlet inlet = (TileEntityPowerInlet)te;

            if (!isServerAssembled(tap) || isOutwardsSide(inlet, side)){
                if (inlet.hasEnergyConnection()){
                    return _icons[METADATA_POWER_INLET][INLET_CONNECTED];
                }
                else {
                    return _icons[METADATA_POWER_INLET][INLET_DISCONNECTED];
                }
            }
        }
        return blockIcon;
    }

    private IIcon getControllerIcon(IBlockAccess blockAccess, int x, int y, int z, int side){
        TileEntity te = blockAccess.getTileEntity(x, y, z);
        if (te instanceof TileEntityServerPart){
            TileEntityServerPart controller = (TileEntityServerPart)te;
            MultiBlockServer server = controller.getServerController();

            if (server == null || !server.isAssembled()){
                return _icons[METADATA_CONTROLLER][CONTROLLER_OFF];
            }
            else if (!isOutwardsSide(controller, side)){
                return blockIcon;
            }
            else if (server.getActive()){
                return _icons[METADATA_CONTROLLER][CONTROLLER_ON];
            }
            else {
                return _icons[METADATA_CONTROLLER][CONTROLLER_OFF];
            }
        }
        return blockIcon;
    }

    private IIcon getTransmitterIcon(IBlockAccess blockAccess, int x, int y, int z, int side){
        TileEntity te = blockAccess.getTileEntity(x, y, z);
        if (te instanceof TileEntityTransmitter){
            TileEntityTransmitter transmitter = (TileEntityTransmitter)te;
            MultiBlockServer server = transmitter.getServerController();
            if (server == null || !server.isAssembled()){
                return _icons[METADATA_TRANSMITTER][TRANSMITTER_OFF];
            }
            else if (server.getActive()){
                return _icons[METADATA_TRANSMITTER][TRANSMITTER_ON];
            }
            else {
                return _icons[METADATA_TRANSMITTER][TRANSMITTER_OFF];
            }
        }
    }

    private IIcon getFaceOrBlockIcon(IBlockAccess blockAccess, int x, int y, int z, int side, int metadata){
        TileEntity te = blockAccess.getTileEntity(x, y, z);
        if (te instanceof TileEntityServerPart){
            TileEntityServerPart part = (TileEntityServerPart)te;
            if(!isServerAssembled(part) || isOutwardsSide(part, side)){
                return _icons[metadata][0];
            }
        }
        return this.blockIcon;
    }

    private boolean isOutwardsSide(TileEntityServerPart part, int side){
        ForgeDirection outDir = part.getOutwardsDir();
        return outDir.ordinal() == side;
    }

    private boolean isServerAssembled(TileEntityServerPart part){
        MultiBlockServer server = part.getServerController();
        return server != null && server.isAssembled();
    }
}
