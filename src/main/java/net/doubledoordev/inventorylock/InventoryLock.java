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

package net.doubledoordev.inventorylock;

import com.google.common.collect.ImmutableList;
import net.doubledoordev.inventorylock.client.ClientEventHandler;
import net.doubledoordev.inventorylock.cmd.InventoryLockCommand;
import net.doubledoordev.inventorylock.network.Reply;
import net.doubledoordev.inventorylock.network.Request;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static net.doubledoordev.inventorylock.client.ClientEventHandler.CLIENT_EVENT_HANDLER;
import static net.doubledoordev.inventorylock.server.ServerEventHandler.EVENT_HANDLER;
import static net.doubledoordev.inventorylock.util.Constants.MOD_ID;
import static net.doubledoordev.inventorylock.util.Constants.MOD_NAME;

@Mod(modid = MOD_ID, name = MOD_NAME, acceptableRemoteVersions = "*")
public class InventoryLock
{
    @Mod.Instance(MOD_ID)
    private static InventoryLock instance;

    private Configuration config;
    private List<String> keyItems;
    private Logger logger;
    private SimpleNetworkWrapper snw;
    private int breakProtection;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        config = new Configuration(event.getSuggestedConfigurationFile());
        syncConfig(config);

        snw = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID);
        snw.registerMessage(ClientEventHandler.Handler.class, Reply.class, 0, Side.CLIENT);
        snw.registerMessage(Request.Handler.class, Request.class, 1, Side.SERVER);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(EVENT_HANDLER);
        if (event.getSide().isClient()) MinecraftForge.EVENT_BUS.register(CLIENT_EVENT_HANDLER);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new InventoryLockCommand());
    }

    @SubscribeEvent
    public void onConfigChangedOnConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MOD_ID)) syncConfig(config);
    }

    private void syncConfig(Configuration config)
    {
        keyItems = ImmutableList.copyOf(config.getStringList("keyItems", MOD_ID, new String[] {"minecraft:paper", "minecraft:wool", "minecraft:dye"}, "The items accepted by the lock command, to be made into keys. No entries means all items will be accepted (Please don't do this. This will cause a mess.)"));
        breakProtection = config.getInt("breakProtection", MOD_ID, 2, 0, 2, "Try to prevent block breakage on locked blocks by not owners. No guarantees!\n0: No extra protection. (Off)\n1: Allow fakeplayers (and other weird things) to still break locked blocks.\n2: Try to prevent all breaking by non owners.");
        if (config.hasChanged()) config.save();
    }

    public static List<String> getKeyItems()
    {
        return instance.keyItems;
    }

    public static int getBreakProtection() { return instance.breakProtection; }

    public static Logger log()
    {
        return instance.logger;
    }

    public static SimpleNetworkWrapper getSnw()
    {
        return instance.snw;
    }
}
