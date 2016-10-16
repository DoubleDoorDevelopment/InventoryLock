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

import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import net.doubledoordev.inventorylock.InventoryLock;
import net.minecraft.command.CommandException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.doubledoordev.inventorylock.util.Constants.*;
import static net.minecraftforge.common.util.Constants.NBT.TAG_STRING;

/**
 * @author Dries007
 */
public class Wand
{
    private ItemStack stack;

    public Wand(ItemStack stack)
    {
        this.stack = stack;
    }

    public static Wand from(EntityPlayerMP player, EnumHand hand) throws CommandException
    {
        ItemStack stack = player.getHeldItem(hand);
        if (stack == null) throw new CommandException("You need to be holding an item in your " + hand.name().replace('_', ' ').toLowerCase());
        List<String> list = InventoryLock.getKeyItems();
        if (!list.isEmpty() && !list.contains(stack.getItem().getRegistryName().toString())) throw new CommandException("This item can't be a key.");
        return new Wand(stack);
    }

    public Wand setDisplayName(String name)
    {
        if (Strings.isNullOrEmpty(name)) stack.clearCustomName();
        else stack.setStackDisplayName(TextFormatting.AQUA + name);
        return this;
    }

    public Action getAction()
    {
        NBTTagCompound tag = stack.getSubCompound(MOD_ID, false);
        //noinspection ConstantConditions
        if (tag == null) return Action.NONE;
        return Action.values()[(int) tag.getByte(ACTION)];
    }

    public Wand setAction(Action action)
    {
        NBTTagCompound tag = stack.getSubCompound(MOD_ID, true);
        tag.setByte(ACTION, (byte) action.ordinal());
        if (!action.hasUUIDs)
        {
            tag.removeTag(UUIDS);
            NBTTagCompound displayTag = stack.getSubCompound(DISPLAY, false);
            //noinspection ConstantConditions
            if (displayTag != null) displayTag.removeTag(LORE);
        }
        return this;
    }

    public void clone(EntityPlayerMP player, EnumHand hand) throws CommandException
    {
        ItemStack stack = player.getHeldItem(hand);
        if (stack == null) throw new CommandException("You need to be holding an item in your " + hand.name().replace('_', ' ').toLowerCase());
        if (this.getAction() == Action.NONE) throw new CommandException("The item you are holding is not a wand.");
        stack.setTagInfo(MOD_ID, this.stack.getSubCompound(MOD_ID, true));
        stack.setTagInfo(DISPLAY, this.stack.getSubCompound(DISPLAY, true));
    }

    public Map<UUID, String> getUUIDs()
    {
        Map<UUID, String> map = new LinkedHashMap<UUID, String>();
        NBTTagCompound tag = stack.getSubCompound(MOD_ID, true);
        NBTTagList list = tag.getTagList(UUIDS, TAG_STRING);
        PlayerProfileCache ppc = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerProfileCache();
        for (int i = 0; i < list.tagCount(); i++)
        {
            UUID uuid = UUID.fromString(list.getStringTagAt(i));
            GameProfile profile = ppc.getProfileByUUID(uuid);
            map.put(uuid, profile != null ? profile.getName() : "--ERROR--");
        }
        return map;
    }

    public void setUUIDs(Map<UUID, String> map)
    {
        NBTTagList uuidList = new NBTTagList();
        NBTTagList nameList = new NBTTagList();
        nameList.appendTag(new NBTTagString(TextFormatting.RESET.toString() + TextFormatting.GOLD.toString() + "Names & UUIDs:"));
        for (UUID uuid : map.keySet())
        {
            uuidList.appendTag(new NBTTagString(uuid.toString()));
            nameList.appendTag(new NBTTagString(TextFormatting.RESET + map.get(uuid) + ' ' + uuid));
        }
        NBTTagCompound tag = stack.getSubCompound(MOD_ID, true);
        tag.setTag(UUIDS, uuidList);
        stack.getSubCompound(DISPLAY, true).setTag(LORE, nameList);
    }
}
