package mca.blocks;

import mca.core.minecraft.ModItems;
import mca.tile.TileMemorial;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import radixcore.util.BlockHelper;

public class BlockMemorial extends BlockContainer
{
	protected static final AxisAlignedBB SIGN_AABB = new AxisAlignedBB(0.1F, 0.0F, 0.1F, 0.9F, 0.75F, 0.9F);
    
	public BlockMemorial()
	{
		super(Material.cloth);
	}

	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
	{
		return SIGN_AABB;
	}

	public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos)
	{
		return NULL_AABB;
	}

	public boolean isFullCube(IBlockState state)
	{
		return false;
	}

	public boolean isPassable(IBlockAccess worldIn, BlockPos pos)
	{
		return true;
	}

	/**
	 * Used to determine ambient occlusion and culling when rebuilding chunks for render
	 */
	public boolean isOpaqueCube(IBlockState state)
	{
		return false;
	}

	/**
	 * Return true if an entity can be spawned inside the block (used to get the player's bed spawn location)
	 */
	public boolean canSpawnInBlock()
	{
		return true;
	}
	
	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) 
	{
		return null;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState meta) 
	{
		if (!world.isRemote)
		{
			TileMemorial memorial = (TileMemorial) BlockHelper.getTileEntity(world, pos.getX(), pos.getY(), pos.getZ());
			Item memorialItem = null;
			ItemStack memorialStack = null;

			switch (memorial.getType())
			{
			case BROKEN_RING: memorialItem = ModItems.brokenRing; break;
			case DOLL: memorialItem = ModItems.childsDoll; break;
			case TRAIN: memorialItem = ModItems.toyTrain; break;
			}

			if (memorial.getRevivalTicks() == 0) //Will be 1 when removed from a villager revival.
			{
				memorialStack = new ItemStack(memorialItem);
				memorialStack.setTagCompound(new NBTTagCompound());
				memorialStack.getTagCompound().setInteger("relation", memorial.getRelation().getId());
				memorialStack.getTagCompound().setString("ownerName", memorial.getOwnerName());
				memorial.getVillagerSaveData().writeDataToNBT(memorialStack.getTagCompound());
				
				EntityItem drop = new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), memorialStack);
				world.spawnEntityInWorld(drop);
			}
			
			super.breakBlock(world, pos, meta);
		}
	}

	@Override
	public boolean hasTileEntity(IBlockState state)
	{
		return true;
	}

    @Override
    public boolean onBlockEventReceived(World worldIn, BlockPos pos, IBlockState state, int eventID, int eventParam) 
    {
        super.onBlockEventReceived(worldIn, pos, state, eventID, eventParam);
        TileEntity tileentity = worldIn.getTileEntity(pos);
        return tileentity == null ? false : tileentity.receiveClientEvent(eventID, eventParam);
    }

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) 
	{
		return new TileMemorial();
	}
}