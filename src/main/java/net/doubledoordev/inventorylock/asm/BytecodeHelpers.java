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

package net.doubledoordev.inventorylock.asm;

import com.mojang.authlib.GameProfile;
import net.doubledoordev.inventorylock.util.BetterLockCode;
import net.minecraft.item.ItemStack;
import net.minecraft.world.LockCode;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

/**
 * Anything in this class is only used to make use "view bytecode".
 *
 * DO NOT EVER LOAD THIS CLASS.
 *
 * @author Dries007
 */
@SuppressWarnings("all")
public class BytecodeHelpers
{
    static
    {
        if (true) throw new RuntimeException("NO!");
    }

    class EntityPlayerBS extends FakePlayer
    {
        public EntityPlayerBS(WorldServer world, GameProfile name) {super(world, name);}
        public boolean canOpen(LockCode code)
        {
            if (code.isEmpty()) return true;
            else if (code instanceof BetterLockCode) return ((BetterLockCode) code).contains(this);
            else
            {
                ItemStack itemstack = this.getHeldItemMainhand();
                return itemstack != null && itemstack.hasDisplayName() ? itemstack.getDisplayName().equals(code.getLock()) : false;
            }
        }
    }
}
