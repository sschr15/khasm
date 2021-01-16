@file:Suppress("unused")

package net.khasm.transform.target

import net.khasm.util.mapClass
import net.khasm.util.mapMethod
import net.khasm.util.toInt
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.spongepowered.asm.mixin.injection.points.*

/**
 * Inject at the very beginning.
 * This target is equivalent to [`@At("HEAD")`][MethodHead]
 *
 * If some other target does not trigger
 * at the beginning, this does nothing.
 */
class HeadTarget : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        return if (range.contains(0)) CursorsFixed(0) else CursorsFixed()
    }
}

/**
 * Inject at every return opcode.
 * This target is equivalent to [`@At("RETURN")`][BeforeReturn]
 */
class ReturnTarget : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        val output = mutableListOf<Int>()
        node.instructions.forEachIndexed { index, it ->
            if (it.opcode in Opcodes.IRETURN..Opcodes.RETURN) {
                output.add(index)
            }
        }
        return CursorsFixed(output)
    }
}

/**
 * Inject at the last return opcode.
 * This target is equivalent to [`@At("TAIL")`][BeforeFinalReturn]
 */
class LastReturnTarget : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        return CursorsFixed(node.instructions.map { it.opcode }.lastIndexOf(Type.getType(node.desc.substringAfter(')')).getOpcode(Opcodes.IRETURN)))
    }
}

/**
 * Inject at every invocation of a method.
 * This target is equivalent to [`@At("INVOKE")`][BeforeInvoke]
 *
 * [owner], [name], and [desc] should be
 * mapped to intermediary if you're accessing
 * a Minecraft method invocation.
 */
class MethodInvocationTarget(private val owner: String, private val name: String, private val desc: String) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        return CursorsFixed(node.instructions.mapIndexed { index, insnNode -> if (insnNode !is MethodInsnNode) -1 else {
            if (insnNode.owner.replace("/", ".") == mapClass(owner) && insnNode.name == mapMethod(owner, name, desc)) index else -1
        } }.filter { it >= 0 })
    }
}

/**
 * Inject at every field read.
 * This target is partially equivalent to [`@At("FIELD")`][BeforeFieldAccess]
 *
 * [owner], [name], and [desc] should be
 * mapped to intermediary if you're accessing
 * a Minecraft method invocation.
 *
 * @see FieldWriteTarget for injecting when fields are written to
 */
class FieldReadTarget(private val owner: String, private val name: String, private val desc: String) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        return CursorsFixed(node.instructions.mapIndexed { index, insnNode -> if (insnNode !is FieldInsnNode) -1 else if (
            insnNode.owner == mapClass(owner) &&
            insnNode.name == mapMethod(owner, name, desc) &&
            insnNode.desc == desc
        ) index else -1
        }.filter { it >= 0 })
    }
}

/**
 * Inject at every field write.
 * This target is partially equivalent to [`@At("FIELD")`][BeforeFieldAccess]
 *
 * [owner], [name], and [desc] should be
 * mapped to intermediary if you're accessing
 * a Minecraft method invocation.
 *
 * @see FieldReadTarget for injecting when fields are read
 */
class FieldWriteTarget(private val owner: String, private val name: String, private val desc: String) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        return CursorsFixed(node.instructions.mapIndexed { index, insnNode -> if (insnNode !is FieldInsnNode) -1 else if (
            insnNode.owner == mapClass(owner) &&
            insnNode.name == mapMethod(owner, name, desc) &&
            insnNode.desc == desc
        ) index else -1
        }.filter { it >= 0 })
    }
}

enum class ConstantType {
    NULL,
    INT, FLOAT, LONG, DOUBLE,
    STRING, CLASS
}

/**
 * Inject at every instance of a constant type loading.
 * This target is based on [`@At("CONSTANT")`][BeforeConstant]
 *
 * Unlike Mixin's target type, this only
 * checks the type of the loaded constant.
 * However, this can check against many
 * constant types.
 */
class ConstantTarget(private vararg val types: ConstantType) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        return CursorsFixed(node.instructions.mapIndexed { index, insnNode ->
            index to (when (insnNode) {
                is InsnNode -> when (insnNode.opcode) {
                    Opcodes.ACONST_NULL -> ConstantType.NULL in types
                    in Opcodes.ICONST_M1..Opcodes.ICONST_5 -> ConstantType.INT in types
                    in Opcodes.LCONST_0..Opcodes.LCONST_1 -> ConstantType.LONG in types
                    in Opcodes.FCONST_0..Opcodes.FCONST_2 -> ConstantType.FLOAT in types
                    in Opcodes.DCONST_0..Opcodes.DCONST_1 -> ConstantType.DOUBLE in types
                    else -> false
                }
                is IntInsnNode -> ConstantType.INT in types
                is LdcInsnNode -> when (insnNode.cst) {
                    is Int -> ConstantType.INT in types
                    is Long -> ConstantType.LONG in types
                    is Float -> ConstantType.FLOAT in types
                    is Double -> ConstantType.DOUBLE in types
                    is String -> ConstantType.STRING in types
                    is Type -> ConstantType.CLASS in types
                    else -> false
                }
                else -> false
            }).toInt()
        }.filter { it.second > 0 }.map { it.first })
    }
}
