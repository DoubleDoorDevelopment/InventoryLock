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

package net.doubledoordev.inventorylock.network;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import net.doubledoordev.inventorylock.util.BetterLockCode;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Dries007
 */
public class Reply implements IMessage
{
    public BlockPos key;
    public boolean pub;
    public Set<String> value = new LinkedHashSet<String>();

    public Reply(BlockPos key, BetterLockCode blc)
    {
        this.key = key;
        PlayerProfileCache ppc = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerProfileCache();
        for (UUID uuid : blc.list)
        {
            GameProfile gp = ppc.getProfileByUUID(uuid);
            if (gp == null) value.add(uuid.toString());
            else value.add(gp.getName());
        }
        pub = blc.isPublic();
    }

    @SuppressWarnings("unused")
    public Reply()
    {
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        key = BlockPos.fromLong(buf.readLong());
        int i = buf.readInt();
        while (i-- > 0) value.add(ByteBufUtils.readUTF8String(buf));
        pub = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeLong(key.toLong());
        buf.writeInt(value.size());
        for (String name : value) ByteBufUtils.writeUTF8String(buf, name);
        buf.writeBoolean(pub);
    }
}
