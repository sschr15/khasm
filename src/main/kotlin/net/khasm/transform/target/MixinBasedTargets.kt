@file:Suppress("unused")

package net.khasm.transform.target

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode
import org.spongepowered.asm.mixin.injection.points.*

/**
 * Inject at the very beginning.
 * This target is equivalent to [`@At("HEAD")`][MethodHead]
 *
 * If some other target does not trigger
 * at the beginning, this does nothing.
 */
class HeadTarget : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int> {
        return if (range.contains(0)) listOf(0) else emptyList()
    }
}

/**
 * Inject at every return opcode.
 * This target is equivalent to [`@At("RETURN")`][BeforeReturn]
 */
class ReturnTarget : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int> {
        val output = mutableListOf<Int>()
        node.instructions.forEachIndexed { index, it ->
            if (it.opcode in Opcodes.IRETURN..Opcodes.RETURN) {
                output.add(index)
            }
        }
        return output
    }
}

/**
 * Inject at the last return opcode.
 * This target is equivalent to [`@At("TAIL")`][BeforeFinalReturn]
 */
class LastReturnTarget : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int> {
        return listOf(node.instructions.map { it.opcode }.lastIndexOf(Type.getType(node.desc).getOpcode(Opcodes.IRETURN)))
    }
}
