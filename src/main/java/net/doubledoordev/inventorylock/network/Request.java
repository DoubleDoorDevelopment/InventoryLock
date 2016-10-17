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

import io.netty.buffer.ByteBuf;
import net.doubledoordev.inventorylock.util.BetterLockCode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.LockCode;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * @author Dries007
 */
public class Request implements IMessage
{
    public BlockPos key;

    public Request(BlockPos key)
    {
        this.key = key;
    }

    @SuppressWarnings("unused")
    public Request()
    {

    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        key = BlockPos.fromLong(buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeLong(key.toLong());
    }

    public static class Handler implements IMessageHandler<Request, Reply>
    {
        @Override
        public Reply onMessage(Request message, MessageContext ctx)
        {
            TileEntity te = ctx.getServerHandler().playerEntity.getServerWorld().getTileEntity(message.key);
            if (!(te instanceof ILockableContainer)) return null;
            LockCode lc = ((ILockableContainer) te).getLockCode();
            if (!(lc instanceof BetterLockCode)) return new Reply(message.key, new BetterLockCode());
            return new Reply(message.key, ((BetterLockCode) lc));
        }
    }
}
