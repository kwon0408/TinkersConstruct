package mods.tinker.tconstruct.util.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import mods.tinker.tconstruct.TConstruct;
import mods.tinker.tconstruct.blocks.logic.ToolForgeLogic;
import mods.tinker.tconstruct.blocks.logic.ToolStationLogic;
import mods.tinker.tconstruct.library.SkillRegistry;
import mods.tinker.tconstruct.library.blocks.InventoryLogic;
import mods.tinker.tconstruct.skill.Skill;
import mods.tinker.tconstruct.util.player.TPlayerStats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;

public class TPacketHandler implements IPacketHandler
{

	@Override
	public void onPacketData (INetworkManager manager, Packet250CustomPayload packet, Player player)
	{
		Side side = FMLCommonHandler.instance().getEffectiveSide();

		if (packet.channel.equals("TConstruct"))
		{
			if (side == Side.SERVER)
				handleServerPacket(packet, (EntityPlayerMP) player);
			else
				handleClientPacket(packet, (EntityPlayer) player);
		}
	}

	void handleClientPacket (Packet250CustomPayload packet, EntityPlayer player)
	{
		DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));

		byte packetID;

		try
		{
			packetID = inputStream.readByte();
			if (packetID == 1)
			{
				TPlayerStats stats = TConstruct.playerTracker.getPlayerStats(player.username);
				stats.skillList = new ArrayList<Skill>();
				int size = inputStream.readInt();
				for (int i = 0; i < size; i++)
				{
					Skill skill = SkillRegistry.skillMapping.get(inputStream.readInt()).copy();
					skill.setActive(inputStream.readBoolean());
					stats.skillList.add(skill);
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed at reading client packet for TConstruct.");
			e.printStackTrace();
			return;
		}
	}

	void handleServerPacket (Packet250CustomPayload packet, EntityPlayerMP player)
	{
		DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));

		byte packetID;

		try
		{
			packetID = inputStream.readByte();

			if (packetID == 1) //Tool Station
			{
				int dimension = inputStream.readInt();
				World world = DimensionManager.getWorld(dimension);
				int x = inputStream.readInt();
				int y = inputStream.readInt();
				int z = inputStream.readInt();
				TileEntity te = world.getBlockTileEntity(x, y, z);
				
				String toolName = inputStream.readUTF();
				if (te instanceof ToolStationLogic)
				{
					((ToolStationLogic) te).setToolname(toolName);
				}
				if (te instanceof ToolForgeLogic)
                {
                    ((ToolForgeLogic) te).setToolname(toolName);
                }
			}
			else if (packetID == 2) //Stencil Table
			{
				int dimension = inputStream.readInt();
				World world = DimensionManager.getWorld(dimension);
				int x = inputStream.readInt();
				int y = inputStream.readInt();
				int z = inputStream.readInt();
				TileEntity te = world.getBlockTileEntity(x, y, z);
				
				Short itemID = inputStream.readShort();
				Short itemDamage = inputStream.readShort();
				if (te instanceof InventoryLogic)
				{
					((InventoryLogic) te).setInventorySlotContents(1, new ItemStack(itemID, 1, itemDamage));
				}
			}
			
			else if (packetID == 3) //Armor
            {
                //String user = inputStream.readUTF();
                //EntityPlayer player = TConstruct.playerTracker.getEntityPlayer(user);
                player.openGui(TConstruct.instance, TConstruct.proxy.armorGuiID, player.worldObj, (int)player.posX, (int)player.posY, (int)player.posZ);
            }
			
			else if (packetID == 4) //Active Skills
			{
				Byte id = inputStream.readByte();
				TConstruct.playerTracker.activateSkill(player, id);
			}
			
			else if (packetID == 10) //Double jump
			{
				//String user = inputStream.readUTF();
				//EntityPlayer player = TConstruct.playerTracker.getEntityPlayer(user);
				player.fallDistance = 0;
			}
		}
		catch (IOException e)
		{
			System.out.println("Failed at reading server packet for TConstruct.");
			e.printStackTrace();
			return;
		}
	}

	Entity getEntity (World world, int id)
	{
		for (Object o : world.loadedEntityList)
		{
			if (((Entity)o).entityId == id)
				return (Entity) o;
		}
		return null;
	}
}
