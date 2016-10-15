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

import net.doubledoordev.inventorylock.util.BetterLockCode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.LockCode;

import java.util.UUID;

import static net.doubledoordev.inventorylock.util.Constants.MOD_ID;

/**
 * @author Dries007
 */
@SuppressWarnings("unused")
public class Hooks
{
    /**
     * replaces net.minecraft.world.LockCode.fromNBT(nbt) everywhere(!)
     */
    public static LockCode fromNBT(NBTTagCompound nbt)
    {
        if (nbt.hasKey(MOD_ID, net.minecraftforge.common.util.Constants.NBT.TAG_LIST))
        {
            BetterLockCode betterLockCode = new BetterLockCode();
            NBTTagList list = nbt.getTagList(MOD_ID, net.minecraftforge.common.util.Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++)
            {
                betterLockCode.add(UUID.fromString(list.getStringTagAt(i)));
            }
            return betterLockCode;
        }
        return LockCode.fromNBT(nbt);
    }
}
