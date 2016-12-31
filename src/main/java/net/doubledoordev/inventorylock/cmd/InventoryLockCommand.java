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

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import net.doubledoordev.inventorylock.InventoryLock;
import net.doubledoordev.inventorylock.util.Constants;
import net.doubledoordev.inventorylock.util.Helper;
import net.doubledoordev.inventorylock.util.Wand;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.doubledoordev.inventorylock.util.Action.*;
import static net.doubledoordev.inventorylock.util.Constants.BYPASS_KEY;
import static net.doubledoordev.inventorylock.util.Constants.MOD_ID;
import static net.minecraft.entity.player.EntityPlayer.PERSISTED_NBT_TAG;
import static net.minecraft.util.EnumHand.MAIN_HAND;
import static net.minecraft.util.EnumHand.OFF_HAND;
import static net.minecraft.util.text.TextFormatting.*;

/**
 * @author Dries007
 */
public class InventoryLockCommand extends CommandBase
{
    @Override
    public String getName()
    {
        return MOD_ID;
    }

    @Override
    public List<String> getAliases()
    {
        return ImmutableList.of("invlock");
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        return true;
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "Use '/invlock help' for more info.";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) displayHelp(player);
        else if (args[0].equalsIgnoreCase("list")) listKeys(player);
        else if (args[0].equalsIgnoreCase("lock"))
        {
            Wand.from(player, MAIN_HAND).setDisplayName("Lock wand").setAction(LOCK);
            Helper.chat(player, "You are now holding a Lock wand.", AQUA);
        }
        else if (args[0].equalsIgnoreCase("unlock"))
        {
            Wand.from(player, MAIN_HAND).setDisplayName("Unlock wand").setAction(UNLOCK);
            Helper.chat(player, "You are now holding a Unlock wand.", AQUA);
        }
        else if (args[0].equalsIgnoreCase("clone"))
        {
            Wand.from(player, MAIN_HAND).clone(player, OFF_HAND);
            Helper.chat(player, "Wand cloned.", AQUA);
        }
        else if (args[0].equalsIgnoreCase("inspect"))
        {
            Wand.from(player, MAIN_HAND).setDisplayName("Inspect wand").setAction(INSPECT);
            Helper.chat(player, "You are now holding a Inspect wand.", AQUA);
        }
        else if (args[0].equalsIgnoreCase("public"))
        {
            Wand.from(player, MAIN_HAND).setDisplayName("Public wand").setAction(PUBLIC);
            Helper.chat(player, "You are now holding a Public wand.", AQUA);
        }
        else if (args[0].equalsIgnoreCase("add")) doAdd(server, player, args);
        else if (args[0].equalsIgnoreCase("remove")) doRemove(server, player, args);
        else if (args[0].equalsIgnoreCase("bypass"))
        {
            if (!sender.canUseCommand(1, getName())) throw new CommandException("Permission denied.");
            NBTTagCompound persist = player.getEntityData().getCompoundTag(PERSISTED_NBT_TAG);
            persist.setBoolean(Constants.BYPASS_KEY,  !persist.getBoolean(BYPASS_KEY));
            player.getEntityData().setTag(PERSISTED_NBT_TAG, persist);
            Helper.chat(player, "OP bypass now " + persist.getBoolean(BYPASS_KEY), AQUA);
        }
        else displayHelp(player);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
    {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "help", "list", "lock", "unlock", "clone", "inspect", "public", "add", "remove", "bypass");
        if (args.length > 1 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        return super.getTabCompletions(server, sender, args, pos);
    }

    private void displayHelp(EntityPlayerMP sender) throws CommandException
    {
        for (String s : new String[]{
                AQUA + getName() + " sub command help:",
                GREEN + "ProTip: Use TAB to auto complete a command or username!",
                "- help: Display this text.",
                "- list: List what items can become wands.",
                "- lock: Create a 'Lock wand' from the held item.",
                "- unlock: Create a 'Unlock wand' from the held item.",
                "- clone: Copy a wand from primary to secondary hand.",
                "- inspect: Create a 'Inspect wand' from the held item.",
                "- public: Create a 'Public wand' from the held item.",
                AQUA + "If you are holding an existing Add or Remove wand:",
                "- add [name or UUID] ... : Add names to the UUID list.",
                "- remove [name or UUID] ... : Remove names from the UUID list.",
                AQUA + "If you are NOT holding an existing Add or Remove wand:",
                "- add [name or UUID] ... : Create a 'Add wand'.",
                "- remove [name or UUID] ... : Create a 'Remove wand'.",
        }) sender.sendMessage(new TextComponentString(s));
    }

    private void listKeys(EntityPlayerMP player)
    {
        List<String> list = InventoryLock.getKeyItems();
        if (list.isEmpty())
        {
            Helper.chat(player, "Any item can be used.", GREEN);
            return;
        }
        Helper.chat(player, "List of wand-able items:", AQUA);
        for (String item : list) player.sendMessage(new TextComponentString(item));
    }

    private void doAdd(MinecraftServer server, EntityPlayerMP player, String[] args) throws CommandException
    {
        Wand wand = Wand.from(player, MAIN_HAND);
        Map<UUID, String> map = wand.getUUIDs();
        boolean newWand = !wand.getAction().hasUUIDs;
        if (newWand) wand.setDisplayName("Add wand").setAction(ADD);
        int diff = map.size();
        parseArgs(server.getPlayerProfileCache(), player, args, map, false);
        diff = map.size() - diff;
        if (newWand) Helper.chat(player, "You are now holding a Add wand with " + diff + " names.", AQUA);
        else if (diff > 0) Helper.chat(player, "Added " + diff + " names to existing wand.", AQUA);
        else Helper.chat(player, "Wand was not modified.", RED);
        wand.setUUIDs(map);
    }

    private void doRemove(MinecraftServer server, EntityPlayerMP player, String[] args) throws CommandException
    {
        Wand wand = Wand.from(player, MAIN_HAND);
        Map<UUID, String> map = wand.getUUIDs();
        boolean newWand = !wand.getAction().hasUUIDs;
        if (newWand) wand.setDisplayName("Remove wand").setAction(REMOVE);
        int diff = map.size();
        parseArgs(server.getPlayerProfileCache(), player, args, map, !newWand);
        diff = map.size() - diff;
        if (newWand) Helper.chat(player, "You are now holding a Remove wand with " + diff + " names.", AQUA);
        else if (diff < 0) Helper.chat(player, "Removed " + (-diff) + " names to existing wand.", AQUA);
        else Helper.chat(player, "Wand was not modified.", RED);
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
            if (profile == null) Helper.chat(player, "Username " + args[i] + " could not be turned into UUID.", RED);
            else
            {
                if (remove) map.remove(profile.getId());
                else map.put(profile.getId(), profile.getName());
            }
        }
    }
}
