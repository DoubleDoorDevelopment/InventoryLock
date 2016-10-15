/*
 * MIT License
 *
 * Copyright (c) 2016 Dries007 & DoubleDoorDevelopment
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.doubledoordev.inventorylock.util;

import com.mojang.authlib.GameProfile;
import net.doubledoordev.inventorylock.InventoryLock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.LockCode;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

import static net.doubledoordev.inventorylock.util.Constants.*;
import static net.minecraftforge.common.util.Constants.NBT.TAG_STRING;

/**
 * @author Dries007
 */
public class ServerEventHandler
{
    public static final ServerEventHandler EVENT_HANDLER = new ServerEventHandler();

    private ServerEventHandler()
    {

    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event)
    {
        final int protection = InventoryLock.getBreakProtection();
        if (protection == 0) return;
        final EntityPlayer player = event.getPlayer();
        //noinspection ConstantConditions
        if (protection == 1 && (player == null || player instanceof FakePlayer || player.getGameProfile() == null || player.getUniqueID() == null)) return;
        TileEntity te = event.getWorld().getTileEntity(event.getPos());
        if (!(te instanceof ILockableContainer)) return;
        if (!player.canOpen(((ILockableContainer) te).getLockCode())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerInteractRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player instanceof FakePlayer) return;
        ItemStack stack = event.getItemStack();
        if (stack == null) return;
        NBTTagCompound nbt = stack.getSubCompound(MOD_ID, false);
        //noinspection ConstantConditions
        if (nbt == null) return;

        event.setCanceled(true);
        Action action = Action.values()[nbt.getByte(ACTION)];

        TileEntity te = event.getWorld().getTileEntity(event.getPos());
        if (te == null) return;
        if (!(te instanceof ILockableContainer))
        {
            player.addChatComponentMessage(new TextComponentString("This block is not lockable :(").setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }
        ILockableContainer lc = ((ILockableContainer) te);
        Block block = event.getWorld().getBlockState(event.getPos()).getBlock();
        if (block instanceof BlockChest)
        {
            lc = ((BlockChest) block).getLockableContainer(event.getWorld(), event.getPos());
            if (lc == null) lc = (ILockableContainer) te;
        }

        LockCode existingLc = lc.getLockCode();

        if (action == Action.LOCK) // We want to lock
        {
            //noinspection ConstantConditions
            if (existingLc == null || existingLc.isEmpty()) // There is no lock yet, OK
            {
                lc.setLockCode(new BetterLockCode().add(player.getUniqueID()));
                player.addChatComponentMessage(new TextComponentString("Locked!").setStyle(new Style().setColor(TextFormatting.GREEN)));
            }
            else player.addChatComponentMessage(new TextComponentString("This block is already locked.").setStyle(new Style().setColor(TextFormatting.RED)));

            return; // End of any LOCK case
        }
        // Beyond here the block needs to already be locked, via OUR lock, and the player needs to have access.
        //noinspection ConstantConditions
        if (existingLc == null || existingLc.isEmpty())
        {
            player.addChatComponentMessage(new TextComponentString("This block is not locked.").setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }
        if (!(existingLc instanceof BetterLockCode))
        {
            player.addChatComponentMessage(new TextComponentString("This block is not locked via " + MOD_ID + ".").setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }
        BetterLockCode blc = ((BetterLockCode) existingLc);
        if (!blc.contains(player))
        {
            player.addChatComponentMessage(new TextComponentString("You do not have access to this block.").setStyle(new Style().setColor(TextFormatting.RED)));
            return;
        }
        // LOCK is already handled.
        if (action == Action.UNLOCK) // We want to unlock (set back to the EMPTY_CODE singleton)
        {
            lc.setLockCode(LockCode.EMPTY_CODE);
            player.addChatComponentMessage(new TextComponentString("Unlocked!").setStyle(new Style().setColor(TextFormatting.GREEN)));
            return;
        }
        else if (action == Action.ADD) // We want to add uuids
        {
            NBTTagList list = nbt.getTagList(UUIDS, TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++) blc.add(UUID.fromString(list.getStringTagAt(i)));
        }
        else if (action == Action.REMOVE) // We want to remove uuids
        {
            NBTTagList list = nbt.getTagList(UUIDS, TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++)
            {
                UUID uuid = UUID.fromString(list.getStringTagAt(i));
                if (!player.getUniqueID().equals(uuid)) blc.remove(uuid); // Anti self lockout
            }
        }
        // Print this for ADD, REMOVE & INSPECT (LOCK & UNLOCK return early)
        player.addChatComponentMessage(new TextComponentString("People with access:").setStyle(new Style().setColor(TextFormatting.AQUA)));
        for (UUID uuid : blc.list)
        {
            PlayerProfileCache ppc = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerProfileCache();
            GameProfile gp = ppc.getProfileByUUID(uuid);
            if (gp == null) player.addChatComponentMessage(new TextComponentString(uuid.toString()).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Missing username...")))));
            else player.addChatComponentMessage(new TextComponentString(gp.getName()).setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(uuid.toString())))));
        }
    }
}
