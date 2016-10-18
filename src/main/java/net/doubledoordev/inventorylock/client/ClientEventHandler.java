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

package net.doubledoordev.inventorylock.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.doubledoordev.inventorylock.InventoryLock;
import net.doubledoordev.inventorylock.network.Reply;
import net.doubledoordev.inventorylock.network.Request;
import net.doubledoordev.inventorylock.util.Action;
import net.doubledoordev.inventorylock.util.Constants;
import net.doubledoordev.inventorylock.util.Wand;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.ILockableContainer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Dries007
 */
public class ClientEventHandler
{
    public static final ClientEventHandler CLIENT_EVENT_HANDLER = new ClientEventHandler();
    public static final Cache<BlockPos, Reply> LOCK_CACHE = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();

    private ClientEventHandler()
    {
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event)
    {
        Wand wand = new Wand(event.getItemStack());
        if (wand.getAction() != Action.NONE) event.getToolTip().add(1, Constants.MOD_NAME + " Wand");
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderGameOverlayText(RenderGameOverlayEvent.Text event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.gameSettings.showDebugInfo) return;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK && mc.objectMouseOver.getBlockPos() != null)
        {
            BlockPos blockpos = mc.objectMouseOver.getBlockPos();
            TileEntity te = mc.theWorld.getTileEntity(blockpos);

            if (!(te instanceof ILockableContainer)) return;
            
            List<String> right = event.getRight();

            right.add("");
            right.add(TextFormatting.GREEN + "Lockable!");
            right.add("This info updates every 5 seconds.");

            Reply reply = LOCK_CACHE.getIfPresent(blockpos);
            if (reply == null)
            {
                right.add("No information (yet)...");
                return;
            }
            if (reply.value.isEmpty())
            {
                right.add(TextFormatting.GOLD + "Unlocked");
                return;
            }
            right.add(TextFormatting.GOLD + "Locked");
            right.add(reply.pub ? TextFormatting.GOLD + "Public" : (reply.value.contains(mc.thePlayer.getName()) ? TextFormatting.GREEN + "You have access!" : TextFormatting.RED + "You do not have access."));
            right.add("List of people with access:");
            right.addAll(reply.value);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onTickClientTick(TickEvent.ClientTickEvent event)
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        if (event.phase != TickEvent.Phase.START) return;
        if (!mc.gameSettings.showDebugInfo) return;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK && mc.objectMouseOver.getBlockPos() != null)
        {
            BlockPos blockpos = mc.objectMouseOver.getBlockPos();
            TileEntity te = mc.theWorld.getTileEntity(blockpos);
            if (!(te instanceof ILockableContainer)) return;
            if (LOCK_CACHE.getIfPresent(blockpos) == null) InventoryLock.getSnw().sendToServer(new Request(blockpos));
        }
    }

    public static class Handler implements IMessageHandler<Reply, IMessage>
    {
        @Override
        public IMessage onMessage(Reply message, MessageContext ctx)
        {
            LOCK_CACHE.put(message.key, message);
            return null;
        }
    }
}
