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

package net.doubledoordev.inventorylock.cmd;

import com.mojang.authlib.GameProfile;
import net.doubledoordev.inventorylock.InventoryLock;
import net.doubledoordev.inventorylock.util.Action;
import net.doubledoordev.inventorylock.util.Wand;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.*;

import static net.doubledoordev.inventorylock.util.Constants.MOD_ID;
import static net.minecraftforge.common.util.Constants.NBT.TAG_STRING;

/**
 * @author Dries007
 */
public class InventoryLockCommand extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "inventorylock";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "Lock and unlock inventories!";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (args.length == 0 || args[0].equalsIgnoreCase("help"))
        {
            displayHelp(player);
        }
        else if (args[0].equalsIgnoreCase("list"))
        {
            listKeys(player);
        }
        else if (args[0].equalsIgnoreCase("lock"))
        {
            Wand.from(player, EnumHand.MAIN_HAND).setDisplayName("Lock wand").setAction(Action.LOCK);
            player.addChatComponentMessage(new TextComponentString("You are now holding a Lock wand.").setStyle(new Style().setColor(TextFormatting.AQUA)));
        }
        else if (args[0].equalsIgnoreCase("unlock"))
        {
            Wand.from(player, EnumHand.MAIN_HAND).setDisplayName("Unlock wand").setAction(Action.UNLOCK);
            player.addChatComponentMessage(new TextComponentString("You are now holding a Unlock wand.").setStyle(new Style().setColor(TextFormatting.AQUA)));
        }
        else if (args[0].equalsIgnoreCase("clone"))
        {
            Wand.from(player, EnumHand.MAIN_HAND).clone(player, EnumHand.OFF_HAND);
            player.addChatComponentMessage(new TextComponentString("Wand cloned.").setStyle(new Style().setColor(TextFormatting.AQUA)));
        }
        else if (args[0].equalsIgnoreCase("inspect"))
        {
            Wand.from(player, EnumHand.MAIN_HAND).setDisplayName("Inspect wand").setAction(Action.INSPECT);
            player.addChatComponentMessage(new TextComponentString("You are now holding a Inspect wand.").setStyle(new Style().setColor(TextFormatting.AQUA)));
        }
        else if (args[0].equalsIgnoreCase("add")) doAdd(server, player, args);
        else if (args[0].equalsIgnoreCase("remove")) doRemove(server, player, args);
        else displayHelp(player);
    }

    @Override
    public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
    {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "help", "list", "lock", "unlock", "clone", "inspect", "add", "remove");
        if (args.length > 1 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) return getListOfStringsMatchingLastWord(args, server.getAllUsernames());
        return super.getTabCompletionOptions(server, sender, args, pos);
    }

    private void displayHelp(EntityPlayerMP sender) throws CommandException
    {
        for (String s : new String[]{
                TextFormatting.AQUA + getCommandName() + " sub command help:",
                "ProTip: Use TAB to auto complete a command or username!",
                "- help: Display this text.",
                "- list: List what items can become wands.",
                "- lock: Create a 'Lock wand' from the held item.",
                "- unlock: Create a 'Unlock wand' from the held item.",
                "- clone: Copy a wand from primary to secondary hand.",
                "- inspect: Create a 'Inspect wand' from the held item.",
                "If you are holding an existing Add or Remove wand:",
                "- add [name or UUID] ... : Add names to the UUID list.",
                "- remove [name or UUID] ... : Remove names from the UUID list.",
                "If you are NOT holding an existing Add or Remove wand:",
                "- add [name or UUID] ... : Create a 'Add wand'.",
                "- remove [name or UUID] ... : Create a 'Remove wand'.",
        }) sender.addChatComponentMessage(new TextComponentString(s));
    }

    private void listKeys(EntityPlayerMP player)
    {
        List<String> list = InventoryLock.getKeyItems();
        if (list.isEmpty())
        {
            player.addChatComponentMessage(new TextComponentString("Any item can be used.").setStyle(new Style().setColor(TextFormatting.GREEN)));
            return;
        }
        player.addChatComponentMessage(new TextComponentString("List of wand-able items:").setStyle(new Style().setColor(TextFormatting.AQUA)));
        for (String item : list) player.addChatComponentMessage(new TextComponentString(item));
    }

    private void doAdd(MinecraftServer server, EntityPlayerMP player, String[] args) throws CommandException
    {
        Wand wand = Wand.from(player, EnumHand.MAIN_HAND);
        Map<UUID, String> map = wand.getUUIDs();
        boolean newWand = !wand.getAction().hasUUIDs;
        if (newWand) wand.setDisplayName("Add wand").setAction(Action.ADD);
        int diff = map.size();
        parseArgs(server.getPlayerProfileCache(), player, args, map, false);
        diff = map.size() - diff;
        if (newWand) player.addChatComponentMessage(new TextComponentString("You are now holding a Add want with " + diff + " names.").setStyle(new Style().setColor(TextFormatting.AQUA)));
        else if (diff > 0) player.addChatComponentMessage(new TextComponentString("Added " + diff + " names to existing wand.").setStyle(new Style().setColor(TextFormatting.AQUA)));
        else player.addChatComponentMessage(new TextComponentString("Wand was not modified.").setStyle(new Style().setColor(TextFormatting.RED)));
        wand.setUUIDs(map);
    }

    private void doRemove(MinecraftServer server, EntityPlayerMP player, String[] args) throws CommandException
    {
        Wand wand = Wand.from(player, EnumHand.MAIN_HAND);
        Map<UUID, String> map = wand.getUUIDs();
        boolean newWand = !wand.getAction().hasUUIDs;
        if (newWand) wand.setDisplayName("Remove wand").setAction(Action.REMOVE);
        int diff = map.size();
        parseArgs(server.getPlayerProfileCache(), player, args, map, !newWand);
        diff = map.size() - diff;
        if (newWand) player.addChatComponentMessage(new TextComponentString("You are now holding a Add want with " + diff + " names.").setStyle(new Style().setColor(TextFormatting.AQUA)));
        else if (diff < 0) player.addChatComponentMessage(new TextComponentString("Removed " + (-diff) + " names to existing wand.").setStyle(new Style().setColor(TextFormatting.AQUA)));
        else player.addChatComponentMessage(new TextComponentString("Wand was not modified.").setStyle(new Style().setColor(TextFormatting.RED)));
        wand.setUUIDs(map);
    }

    private void parseArgs(PlayerProfileCache ppc, EntityPlayerMP player, String[] args, Map<UUID, String> map, boolean remove)
    {
        for (int i = 1; i < args.length; i++)
        {
            GameProfile profile;
            try
            {
                UUID uuid = UUID.fromString(args[i]);
                profile = ppc.getProfileByUUID(uuid);
                if (profile == null)
                {
                    if (remove) map.remove(uuid);
                    else map.put(uuid, "--ERROR--");
                }
                else continue;
            }
            catch (IllegalArgumentException e)
            {
                profile = ppc.getGameProfileForUsername(args[i]);
            }
            if (profile == null) player.addChatMessage(new TextComponentString("Username " + args[i] + " could not be turned into UUID.").setStyle(new Style().setColor(TextFormatting.RED)));
            else
            {
                if (remove) map.remove(profile.getId());
                else map.put(profile.getId(), profile.getName());
            }
        }
    }
}
