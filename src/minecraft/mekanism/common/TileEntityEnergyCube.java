package mekanism.common;

import ic2.api.Direction;
import ic2.api.ElectricItem;
import ic2.api.IElectricItem;
import ic2.api.IEnergyStorage;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileSourceEvent;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;

import java.util.EnumSet;

import mekanism.api.IEnergyCube;
import mekanism.api.Tier.EnergyCubeTier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import universalelectricity.core.electricity.ElectricInfo;
import universalelectricity.core.electricity.ElectricityConnections;
import universalelectricity.core.implement.IConductor;
import universalelectricity.core.implement.IItemElectric;
import universalelectricity.core.implement.IJouleStorage;
import universalelectricity.core.implement.IVoltage;
import universalelectricity.core.vector.Vector3;
import buildcraft.api.power.IPowerProvider;
import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerFramework;
import buildcraft.api.power.PowerProvider;

import com.google.common.io.ByteArrayDataInput;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;

public class TileEntityEnergyCube extends TileEntityElectricBlock implements IEnergySink, IEnergySource, IEnergyStorage, IPowerReceptor, IJouleStorage, IVoltage, IPeripheral
{
	public EnergyCubeTier tier = EnergyCubeTier.BASIC;
	
	/** Output per tick this machine can transfer. */
	public int output;
	
	/**
	 * A block used to store and transfer electricity.
	 */
	public TileEntityEnergyCube()
	{
		this("Energy Cube", 0, 256);
	}
	
	/**
	 * A block used to store and transfer electricity.
	 * @param energy - maximum energy this block can hold.
	 * @param i - output per tick this block can handle.
	 */
	public TileEntityEnergyCube(String name, int maxEnergy, int i)
	{
		super(name, maxEnergy);
		ElectricityConnections.registerConnector(this, EnumSet.allOf(ForgeDirection.class));
		inventory = new ItemStack[2];
		output = i;
	}
	
	@Override
	public void onUpdate()
	{
		super.onUpdate();
		
		if(powerProvider != null)
		{
			int received = (int)(powerProvider.useEnergy(0, (float)((tier.MAX_ELECTRICITY-electricityStored)*Mekanism.TO_BC), true)*10);
			setJoules(electricityStored + received);
		}
		
		if(!worldObj.isRemote)
		{
			for(ForgeDirection direction : ForgeDirection.values())
			{
				if(direction != ForgeDirection.getOrientation(facing))
				{
					TileEntity tileEntity = Vector3.getTileEntityFromSide(worldObj, new Vector3(this), direction);
					if(tileEntity != null)
					{
						if(tileEntity instanceof IConductor)
						{
							if(electricityStored < tier.MAX_ELECTRICITY)
							{
								double electricityNeeded = tier.MAX_ELECTRICITY - electricityStored;
								((IConductor)tileEntity).getNetwork().startRequesting(this, electricityNeeded, electricityNeeded >= getVoltage() ? getVoltage() : electricityNeeded);
								setJoules(electricityStored + ((IConductor)tileEntity).getNetwork().consumeElectricity(this).getWatts());
							}
							else if(electricityStored >= tier.MAX_ELECTRICITY)
							{
								((IConductor)tileEntity).getNetwork().stopRequesting(this);
							}
						}
					}
				}
			}
		}
		
		if(inventory[0] != null && electricityStored > 0)
		{
			if(inventory[0].getItem() instanceof IItemElectric)
			{
				IItemElectric electricItem = (IItemElectric)inventory[0].getItem();
				double sendingElectricity = 0;
				double actualSendingElectricity = 0;
				double rejectedElectricity = 0;
				double itemElectricityNeeded = electricItem.getMaxJoules(inventory[0]) - electricItem.getJoules(inventory[0]);
				
				if(electricItem.getVoltage() <= electricityStored)
				{
					sendingElectricity = electricItem.getVoltage();
				}
				else if(electricItem.getVoltage() > electricityStored)
				{
					sendingElectricity = electricityStored;
				}
				
				if(sendingElectricity <= itemElectricityNeeded)
				{
					actualSendingElectricity = sendingElectricity;
				}
				else if(sendingElectricity > itemElectricityNeeded)
				{
					rejectedElectricity = sendingElectricity-itemElectricityNeeded;
					actualSendingElectricity = itemElectricityNeeded;
				}
				
				electricItem.setJoules((electricItem.getJoules(inventory[0]) + actualSendingElectricity), inventory[0]);
				setJoules(electricityStored - (actualSendingElectricity - rejectedElectricity));
			}
			else if(inventory[0].getItem() instanceof IElectricItem)
			{
				double sent = ElectricItem.charge(inventory[0], (int)(electricityStored*Mekanism.TO_IC2), 3, false, false)*Mekanism.FROM_IC2;
				setJoules(electricityStored - sent);
			}
		}
		
		if(inventory[1] != null && electricityStored < tier.MAX_ELECTRICITY)
		{
			if(inventory[1].getItem() instanceof IItemElectric)
			{
				IItemElectric electricItem = (IItemElectric)inventory[1].getItem();

				if (electricItem.canProduceElectricity())
				{
					double joulesNeeded = tier.MAX_ELECTRICITY-electricityStored;
					double joulesReceived = 0;
					
					if(electricItem.getVoltage() <= joulesNeeded)
					{
						joulesReceived = electricItem.onUse(electricItem.getVoltage(), inventory[1]);
					}
					else if(electricItem.getVoltage() > joulesNeeded)
					{
						joulesReceived = electricItem.onUse(joulesNeeded, inventory[1]);
					}
					
					setJoules(electricityStored + joulesReceived);
				}
			}
			else if(inventory[1].getItem() instanceof IElectricItem)
			{
				IElectricItem item = (IElectricItem)inventory[1].getItem();
				if(item.canProvideEnergy())
				{
					double gain = ElectricItem.discharge(inventory[1], (int)((tier.MAX_ELECTRICITY - electricityStored)*Mekanism.TO_IC2), 3, false, false)*Mekanism.FROM_IC2;
					setJoules(electricityStored + gain);
				}
			}
			else if(inventory[1].itemID == Item.redstone.shiftedIndex)
			{
				setJoules(electricityStored + 1000);
				--inventory[1].stackSize;
				
                if (inventory[1].stackSize <= 0)
                {
                    inventory[1] = null;
                }
			}
		}
		
		if(electricityStored > 0)
		{
			TileEntity tileEntity = Vector3.getTileEntityFromSide(worldObj, new Vector3(this), ForgeDirection.getOrientation(facing));
			
			if(Mekanism.hooks.IC2Loaded)
			{
				if(electricityStored >= output)
				{
					MinecraftForge.EVENT_BUS.post(new EnergyTileSourceEvent(this, output));
				}
			}
			
			if(tileEntity != null)
			{
				if(isPowerReceptor(tileEntity))
				{
					IPowerReceptor receptor = (IPowerReceptor)tileEntity;
	            	double electricityNeeded = Math.min(receptor.getPowerProvider().getMinEnergyReceived(), receptor.getPowerProvider().getMaxEnergyReceived())*Mekanism.FROM_BC;
	            	float transferEnergy = (float)Math.max(Math.min(Math.min(electricityNeeded, electricityStored), 80000), 0);
	            	receptor.getPowerProvider().receiveEnergy((float)(transferEnergy*Mekanism.FROM_BC), ForgeDirection.getOrientation(facing).getOpposite());
	            	setJoules(electricityStored - (int)transferEnergy);
				}
				else if(tileEntity instanceof IConductor)
				{
					double joulesNeeded = ((IConductor)tileEntity).getNetwork().getRequest().getWatts();
					double transferAmps = Math.max(Math.min(Math.min(ElectricInfo.getAmps(joulesNeeded, getVoltage()), ElectricInfo.getAmps(electricityStored, getVoltage())), 80), 0);

					if (!worldObj.isRemote && transferAmps > 0)
					{
						((IConductor)tileEntity).getNetwork().startProducing(this, transferAmps, getVoltage());
						setJoules(electricityStored - ElectricInfo.getWatts(transferAmps, getVoltage()));
					}
					else
					{
						((IConductor)tileEntity).getNetwork().stopProducing(this);
					}
				}
			}
		}
	}

	@Override
	public boolean acceptsEnergyFrom(TileEntity emitter, Direction direction)
	{
		return direction.toForgeDirection() != ForgeDirection.getOrientation(facing);
	}

	@Override
	public int getStored() 
	{
		return (int)(electricityStored*Mekanism.TO_IC2);
	}

	@Override
	public int getCapacity() 
	{
		return (int)(tier.MAX_ELECTRICITY*Mekanism.TO_IC2);
	}

	@Override
	public int getOutput() 
	{
		return output;
	}

	@Override
	public int demandsEnergy() 
	{
		return (int)((tier.MAX_ELECTRICITY - electricityStored)*Mekanism.TO_IC2);
	}

	@Override
    public int injectEnergy(Direction direction, int i)
    {
		double givenEnergy = i*Mekanism.FROM_IC2;
    	double rejects = 0;
    	double neededEnergy = tier.MAX_ELECTRICITY-electricityStored;
    	
    	if(givenEnergy <= neededEnergy)
    	{
    		electricityStored += givenEnergy;
    	}
    	else if(givenEnergy > neededEnergy)
    	{
    		electricityStored += neededEnergy;
    		rejects = givenEnergy-neededEnergy;
    	}
    	
    	return (int)(rejects*Mekanism.TO_IC2);
    }

	@Override
	public boolean emitsEnergyTo(TileEntity receiver, Direction direction)
	{
		return direction.toForgeDirection() == ForgeDirection.getOrientation(facing);
	}

	@Override
	public int getMaxEnergyOutput()
	{
		return output;
	}

	@Override
	public double getJoules(Object... data) 
	{
		return electricityStored;
	}

	@Override
	public void setJoules(double joules, Object... data)
	{
		electricityStored = Math.max(Math.min(joules, getMaxJoules()), 0);
	}

	@Override
	public double getMaxJoules(Object... data) 
	{
		return tier.MAX_ELECTRICITY;
	}
	
	@Override
	public int getStartInventorySide(ForgeDirection side) 
	{
		if(side == ForgeDirection.getOrientation(1))
		{
			return 0;
		}
		
		return 1;
	}

	@Override
	public int getSizeInventorySide(ForgeDirection side)
	{
		return 1;
	}

	@Override
	public double getVoltage() 
	{
		return 120;
	}
	
	/**
	 * Whether or not the declared Tile Entity is an instance of a BuildCraft power receptor.
	 * @param tileEntity - tile entity to check
	 * @return if the tile entity is a power receptor
	 */
	public boolean isPowerReceptor(TileEntity tileEntity)
	{
		if(tileEntity instanceof IPowerReceptor) 
		{
			IPowerReceptor receptor = (IPowerReceptor)tileEntity;
			IPowerProvider provider = receptor.getPowerProvider();
			return provider != null && provider.getClass().getSuperclass().equals(PowerProvider.class);
		}
		return false;
	}

	@Override
	public String getType() 
	{
		return getInvName();
	}

	@Override
	public String[] getMethodNames() 
	{
		return new String[] {"getStored", "getOutput", "getMaxEnergy", "getEnergyNeeded"};
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, int method, Object[] arguments) throws Exception 
	{
		switch(method)
		{
			case 0:
				return new Object[] {electricityStored};
			case 1:
				return new Object[] {output};
			case 2:
				return new Object[] {tier.MAX_ELECTRICITY};
			case 3:
				return new Object[] {(tier.MAX_ELECTRICITY-electricityStored)};
			default:
				System.err.println("[Mekanism] Attempted to call unknown method with computer ID " + computer.getID());
				return null;
		}
	}

	@Override
	public boolean canAttachToSide(int side) 
	{
		return true;
	}

	@Override
	public void attach(IComputerAccess computer, String computerSide) {}

	@Override
	public void detach(IComputerAccess computer) {}
	
	@Override
	public void handlePacketData(INetworkManager network, Packet250CustomPayload packet, EntityPlayer player, ByteArrayDataInput dataStream)
	{
		try {
			facing = dataStream.readInt();
			electricityStored = dataStream.readDouble();
			tier = EnergyCubeTier.getFromName(dataStream.readUTF());
			worldObj.markBlockForRenderUpdate(xCoord, yCoord, zCoord);
		} catch (Exception e)
		{
			System.out.println("[Mekanism] Error while handling tile entity packet.");
			e.printStackTrace();
		}
	}
	
	@Override
    public void readFromNBT(NBTTagCompound nbtTags)
    {
        super.readFromNBT(nbtTags);

        tier = EnergyCubeTier.getFromName(nbtTags.getString("tier"));
    }

	@Override
    public void writeToNBT(NBTTagCompound nbtTags)
    {
        super.writeToNBT(nbtTags);
        
        nbtTags.setString("tier", tier.name);
    }
	
	@Override
    public void sendPacket()
    {
		PacketHandler.sendTileEntityPacketToClients(this, 0, facing, electricityStored, tier.name);
    }
    
	@Override
    public void sendPacketWithRange()
    {
		PacketHandler.sendTileEntityPacketToClients(this, 50, facing, electricityStored, tier.name);
    }
	
	@Override
	public int getMaxSafeInput()
	{
		return 2048;
	}

	@Override
	public void setStored(int energy)
	{
		setJoules(energy*Mekanism.FROM_IC2);
	}

	@Override
	public int addEnergy(int amount)
	{
		setJoules(electricityStored + amount*Mekanism.FROM_IC2);
		return (int)electricityStored;
	}

	@Override
	public boolean isTeleporterCompatible(Direction side) 
	{
		return side.toForgeDirection() == ForgeDirection.getOrientation(facing);
	}
	
	@Override
	public ItemStack getWrenchDrop(EntityPlayer entityPlayer)
	{
		ItemStack itemStack = new ItemStack(Mekanism.EnergyCube);
		
        IEnergyCube energyCube = (IEnergyCube)itemStack.getItem();
        energyCube.setTier(itemStack, tier);
        
        IItemElectric electricItem = (IItemElectric)itemStack.getItem();
        electricItem.setJoules(electricityStored, itemStack);
        
        return itemStack;
	}
}