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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.LockCode;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static net.doubledoordev.inventorylock.util.Constants.MOD_ID;

/**
 * @author Dries007
 */
public class BetterLockCode extends LockCode
{
    public final Set<UUID> list = new LinkedHashSet<UUID>();

    public BetterLockCode()
    {
        super(MOD_ID);
    }

    public BetterLockCode add(UUID uuid)
    {
        list.add(uuid);
        return this;
    }

    public BetterLockCode remove(UUID uuid)
    {
        list.remove(uuid);
        return this;
    }

    public boolean contains(EntityPlayer player)
    {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null && server.getPlayerList().getOppedPlayers().getPermissionLevel(player.getGameProfile()) > 0)
        {
            if (!contains(player.getUniqueID())) player.addChatComponentMessage(new TextComponentString("OP Bypass").setStyle(new Style().setColor(TextFormatting.GRAY)));
            return true;
        }
        return contains(player.getUniqueID());
    }

    public boolean contains(UUID uuid)
    {
        return list.contains(uuid);
    }

    @Override
    public boolean isEmpty()
    {
        return list.isEmpty();
    }

    @Override
    public void toNBT(NBTTagCompound nbt)
    {
        NBTTagList list = new NBTTagList();
        for (UUID uuid : this.list) list.appendTag(new NBTTagString(uuid.toString()));
        nbt.setTag(MOD_ID, list);
    }
}
