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

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static net.doubledoordev.inventorylock.asm.Plugin.LOGGER;

/**
 * @author Dries007
 */
@SuppressWarnings("unused")
public class Transformer implements IClassTransformer
{
    private static final String LOCK_CODE_OWNER = "net/minecraft/world/LockCode";
    private static final String LOCK_CODE_DESC = "(Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/world/LockCode;";
    private static final String LOCK_CODE_NAME = "fromNBT";
    private static final String LOCK_CODE_OWNER_REPLACE = "net/doubledoordev/inventorylock/asm/Hooks";
    private static final String LOCK_CODE_TARGET = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(LOCK_CODE_OWNER, LOCK_CODE_NAME, LOCK_CODE_DESC);
    private static final String ENTITY_PLAYER_OWNER = "net/minecraft/entity/player/EntityPlayer";
    private static final String ENTITY_PLAYER_OWNER_NAME = ENTITY_PLAYER_OWNER.replace('/', '.');
    private static final String ENTITY_PLAYER_DESC = "(Lnet/minecraft/world/LockCode;)Z";
    private static final String ENTITY_PLAYER_TARGET = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(ENTITY_PLAYER_OWNER, "canOpen", ENTITY_PLAYER_DESC);
//    private static final String ENTITY_PLATER_GET_UUID_DESC = "()Ljava/util/UUID;";
//    private static final String ENTITY_PLAYER_GET_UUID = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(ENTITY_PLAYER_OWNER, "getUniqueID", ENTITY_PLATER_GET_UUID_DESC);
    private static final String BETTER_LOCK_CONTAINS_DESC = "(Lnet/minecraft/entity/player/EntityPlayer;)Z";
    private static final String BETTER_LOCK_CONTAINS = "contains";
    private static final String BETTER_LOCK_TYPE = "net/doubledoordev/inventorylock/util/BetterLockCode";

    public Transformer()
    {
        Plugin.LOGGER.info("Loaded Black magic aka Transformer with target name {}", LOCK_CODE_TARGET);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

        boolean isPlayer = name.equals(ENTITY_PLAYER_OWNER_NAME);
        if (isPlayer) LOGGER.info("Found EntityPlayer");

        for (MethodNode method : classNode.methods)
        {
            InsnList list = method.instructions;
            if (isPlayer && method.name.equals(ENTITY_PLAYER_TARGET) && method.desc.equals(ENTITY_PLAYER_DESC))
            {
                final LabelNode newLabel = new LabelNode();
                LOGGER.info("Found canOpen");
                AbstractInsnNode node = list.getFirst();
                while (node.getOpcode() != Opcodes.IRETURN && node != list.getLast())
                {
                    if (node.getOpcode() == Opcodes.IFEQ) ((JumpInsnNode) node).label = newLabel;
                    node = node.getNext();
                }
                if (node.getOpcode() != Opcodes.IRETURN) throw new RuntimeException("ASM failed. (return not found)");
                final AbstractInsnNode target = node;
                while (node.getType() != AbstractInsnNode.LABEL && node != list.getLast()) node = node.getNext();
                if (node.getType() != AbstractInsnNode.LABEL) throw new RuntimeException("ASM failed. (label not found)");
                final LabelNode label = ((LabelNode) node);

                //Adding "else if (code instanceof BetterLockCode) return ((BetterLockCode) code).contains(this.getUniqueID());"
                InsnList inject = new InsnList();

                inject.add(newLabel);
                inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                inject.add(new TypeInsnNode(Opcodes.INSTANCEOF, BETTER_LOCK_TYPE));
                inject.add(new JumpInsnNode(Opcodes.IFEQ, label));
                inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                inject.add(new TypeInsnNode(Opcodes.CHECKCAST, BETTER_LOCK_TYPE));
                inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                //inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, ENTITY_PLAYER_OWNER, ENTITY_PLAYER_GET_UUID, ENTITY_PLATER_GET_UUID_DESC, false));
                inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BETTER_LOCK_TYPE, BETTER_LOCK_CONTAINS, BETTER_LOCK_CONTAINS_DESC, false));
                inject.add(new InsnNode(Opcodes.IRETURN));

                list.insert(target, inject);
                LOGGER.info("Injected elseif into EntityPlayer's canOpen");
            }
            for (AbstractInsnNode node = list.getFirst(); node != list.getLast(); node = node.getNext())
            {
                if (node.getOpcode() != Opcodes.INVOKESTATIC) continue;
                MethodInsnNode methodInsnNode = ((MethodInsnNode) node);
                if (methodInsnNode.owner.equals(LOCK_CODE_OWNER) && methodInsnNode.desc.equals(LOCK_CODE_DESC))
                {
                    if (methodInsnNode.name.equals(LOCK_CODE_TARGET) || methodInsnNode.name.equals(LOCK_CODE_NAME))
                    {
                        methodInsnNode.owner = LOCK_CODE_OWNER_REPLACE;
                        methodInsnNode.name = LOCK_CODE_NAME;
                        LOGGER.info("Replaced call in class {} ({}), method {}{}", name, transformedName, method.name, method.desc);
                    }
                }
            }
        }

        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
