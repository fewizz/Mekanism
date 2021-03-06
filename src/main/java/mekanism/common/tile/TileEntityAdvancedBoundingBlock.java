package mekanism.common.tile;

import ic2.api.energy.tile.IEnergySink;
import mekanism.api.Coord4D;
import mekanism.api.IFilterAccess;
import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.common.IAdvancedBoundingBlock;
import mekanism.common.util.InventoryUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.InterfaceList;
import cpw.mods.fml.common.Optional.Method;

import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerHandler;
import buildcraft.api.power.PowerHandler.PowerReceiver;
import cofh.api.energy.IEnergyHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

@InterfaceList({
		@Interface(iface = "buildcraft.api.power.IPowerReceptor", modid = "BuildCraftAPI|power"),
		@Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2API", striprefs = true),
		@Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHAPI|energy"),
		@Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft")
})
public class TileEntityAdvancedBoundingBlock extends TileEntityBoundingBlock implements ISidedInventory, IEnergySink, IStrictEnergyAcceptor, IPowerReceptor, IStrictEnergyStorage, IEnergyHandler, IPeripheral, IFilterAccess
{
	@Override
	public int getSizeInventory()
	{
		if(getInv() == null)
		{
			return 0;
		}

		return getInv().getSizeInventory();
	}

	@Override
	public ItemStack getStackInSlot(int i)
	{
		if(getInv() == null)
		{
			return null;
		}

		return getInv().getStackInSlot(i);
	}

	@Override
	public ItemStack decrStackSize(int i, int j)
	{
		if(getInv() == null)
		{
			return null;
		}

		return getInv().decrStackSize(i, j);
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int i)
	{
		if(getInv() == null)
		{
			return null;
		}

		return getInv().getStackInSlotOnClosing(i);
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack)
	{
		if(getInv() == null)
		{
			return;
		}

		getInv().setInventorySlotContents(i, itemstack);
	}

	@Override
	public String getInventoryName()
	{
		if(getInv() == null)
		{
			return null;
		}

		return getInv().getInventoryName();
	}

	@Override
	public boolean hasCustomInventoryName()
	{
		if(getInv() == null)
		{
			return false;
		}

		return getInv().hasCustomInventoryName();
	}

	@Override
	public int getInventoryStackLimit()
	{
		if(getInv() == null)
		{
			return 0;
		}

		return getInv().getInventoryStackLimit();
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer)
	{
		if(getInv() == null)
		{
			return false;
		}

		return getInv().isUseableByPlayer(entityplayer);
	}

	@Override
	public void openInventory()
	{
		if(getInv() == null)
		{
			return;
		}

		getInv().openInventory();
	}

	@Override
	public void closeInventory()
	{
		if(getInv() == null)
		{
			return;
		}

		getInv().closeInventory();
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack)
	{
		if(getInv() == null)
		{
			return false;
		}

		return getInv().canBoundInsert(Coord4D.get(this), i, itemstack);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int slotID)
	{
		if(getInv() == null)
		{
			return InventoryUtils.EMPTY;
		}

		return getInv().getBoundSlots(Coord4D.get(this), slotID);
	}

	@Override
	public boolean canInsertItem(int i, ItemStack itemstack, int j)
	{
		return isItemValidForSlot(i, itemstack);
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemstack, int j)
	{
		if(getInv() == null)
		{
			return false;
		}

		return getInv().canBoundExtract(Coord4D.get(this), i, itemstack, j);
	}

	@Override
	@Method(modid = "IC2API")
	public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction)
	{
		if(getInv() == null)
		{
			return false;
		}

		return getInv().acceptsEnergyFrom(emitter, direction);
	}

	@Override
	@Method(modid = "CoFHAPI|energy")
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
	{
		if(getInv() == null)
		{
			return 0;
		}

		return getInv().receiveEnergy(from, maxReceive, simulate);
	}

	@Override
	@Method(modid = "CoFHAPI|energy")
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate)
	{
		if(getInv() == null)
		{
			return 0;
		}

		return getInv().extractEnergy(from, maxExtract, simulate);
	}

	@Override
	@Method(modid = "CoFHAPI|energy")
	public boolean canConnectEnergy(ForgeDirection from)
	{
		if(getInv() == null)
		{
			return false;
		}

		return getInv().canConnectEnergy(from);
	}

	@Override
	@Method(modid = "CoFHAPI|energy")
	public int getEnergyStored(ForgeDirection from)
	{
		if(getInv() == null)
		{
			return 0;
		}

		return getInv().getEnergyStored(from);
	}

	@Override
	@Method(modid = "CoFHAPI|energy")
	public int getMaxEnergyStored(ForgeDirection from)
	{
		if(getInv() == null)
		{
			return 0;
		}

		return getInv().getMaxEnergyStored(from);
	}

	@Override
	public double getEnergy()
	{
		if(getInv() == null)
		{
			return 0;
		}

		return getInv().getEnergy();
	}

	@Override
	public void setEnergy(double energy)
	{
		if(getInv() == null)
		{
			return;
		}

		getInv().setEnergy(energy);
	}

	@Override
	public double getMaxEnergy()
	{
		if(getInv() == null)
		{
			return 0;
		}

		return getInv().getMaxEnergy();
	}

	@Override
	@Method(modid = "BuildCraftAPI|power")
	public PowerReceiver getPowerReceiver(ForgeDirection side)
	{
		if(getInv() == null)
		{
			return null;
		}

		return getInv().getPowerReceiver(side);
	}

	@Override
	@Method(modid = "BuildCraftAPI|power")
	public void doWork(PowerHandler workProvider)
	{
		if(getInv() == null)
		{
			return;
		}

		getInv().doWork(workProvider);
	}

	@Override
	@Method(modid = "BuildCraftAPI|power")
	public World getWorld()
	{
		if(getInv() == null)
		{
			return null;
		}

		return getInv().getWorld();
	}

	@Override
	public double transferEnergyToAcceptor(ForgeDirection side, double amount)
	{
		if(getInv() == null)
		{
			return 0;
		}

		return getInv().transferEnergyToAcceptor(side, amount);
	}

	@Override
	public boolean canReceiveEnergy(ForgeDirection side)
	{
		if(getInv() == null)
		{
			return false;
		}

		return getInv().canReceiveEnergy(side);
	}

	@Override
	@Method(modid = "IC2API")
	public double getDemandedEnergy()
	{
		if(getInv() == null)
		{
			return 0;
		}

		return getInv().getDemandedEnergy();
	}

	@Override
	@Method(modid = "IC2API")
	public double injectEnergy(ForgeDirection directionFrom, double amount, double voltage)
	{
		if(getInv() == null)
		{
			return amount;
		}

		return getInv().injectEnergy(directionFrom, amount, voltage);
	}

	@Override
	@Method(modid = "IC2API")
	public int getSinkTier()
	{
		if(getInv() == null)
		{
			return 0;
		}

		return getInv().getSinkTier();
	}

	public IAdvancedBoundingBlock getInv()
	{
		TileEntity tile = new Coord4D(mainX, mainY, mainZ, worldObj.provider.dimensionId).getTileEntity(worldObj);

		if(!(tile instanceof IAdvancedBoundingBlock))
		{
			worldObj.setBlockToAir(xCoord, yCoord, zCoord);
			return null;
		}

		return (IAdvancedBoundingBlock)new Coord4D(mainX, mainY, mainZ, worldObj.provider.dimensionId).getTileEntity(worldObj);
	}

	@Override
	public void onPower()
	{
		super.onPower();

		if(getInv() != null)
		{
			getInv().onPower();
		}
	}

	@Override
	public void onNoPower()
	{
		super.onNoPower();

		if(getInv() != null)
		{
			getInv().onNoPower();
		}
	}

	@Override
	@Method(modid = "ComputerCraft")
	public String getType()
	{
		if(getInv() == null)
		{
			return null;
		}

		return getInv().getType();
	}

	@Override
	@Method(modid = "ComputerCraft")
	public String[] getMethodNames()
	{
		if(getInv() == null)
		{
			return null;
		}

		return getInv().getMethodNames();
	}

	@Override
	@Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws Exception
	{
		if(getInv() == null)
		{
			return null;
		}

		return getInv().callMethod(computer, context, method, arguments);
	}

	@Override
	@Method(modid = "ComputerCraft")
	public void attach(IComputerAccess computer)
	{
		if(getInv() == null)
		{
			return;
		}

		getInv().attach(computer);
	}

	@Override
	@Method(modid = "ComputerCraft")
	public void detach(IComputerAccess computer)
	{
		if(getInv() == null)
		{
			return;
		}

		getInv().detach(computer);
	}

	@Override
	@Method(modid = "ComputerCraft")
	public boolean equals(IPeripheral other)
	{
		return this == other;
	}

	@Override
	public NBTTagCompound getFilterData(NBTTagCompound nbtTags)
	{
		if(getInv() == null)
		{
			return null;
		}
		
		return getInv().getFilterData(nbtTags);
	}

	@Override
	public void setFilterData(NBTTagCompound nbtTags)
	{
		if(getInv() == null)
		{
			return;
		}
		
		getInv().setFilterData(nbtTags);
	}

	@Override
	public String getDataType()
	{
		if(getInv() == null)
		{
			return "null";
		}
		
		return getInv().getDataType();
	}
}
