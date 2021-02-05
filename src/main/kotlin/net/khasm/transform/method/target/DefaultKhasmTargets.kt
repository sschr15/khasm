@file:Suppress("unused")

package net.khasm.transform.method.target

import net.khasm.annotation.DangerousKhasmUsage
import org.objectweb.asm.tree.*
import kotlin.reflect.KClass

/**
 * Inject with your own logic.
 *
 * If injecting this way, consider making your
 * own implementation of [AbstractKhasmTarget]
 * if you'd like to reuse the same logic.
 */
class CustomTarget(private val lambda: MethodNode.() -> List<Int>) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        return CursorsFixed(lambda(node).filter(range::contains))
    }
}

/**
 * Inject a certain distance into the method.
 *
 * Note: this can be dangerous as other transformers
 * may have already changed the method.
 */
@DangerousKhasmUsage
class RawTarget(private vararg val cursors: Int) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        return CursorsFixed(cursors.asList().filter(range::contains))
    }
}

/**
 * Inject at every instance of an opcode.
 */
class OpcodeTarget(private val opcode: Int) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        val output = mutableListOf<Int>()
        node.instructions.forEachIndexed { index, it ->
            if (it.opcode == opcode) {
                output.add(index)
            }
        }
        return CursorsFixed(output)
    }
}

/**
 * Inject at a given ASM class instance
 * ([LdcInsnNode], [VarInsnNode], [LabelNode], etc).
 */
class AsmInstructionTarget(private val type: KClass<out AbstractInsnNode>) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        return CursorsFixed(node.instructions.mapIndexedNotNull { index, insnNode ->
            if (type.isInstance(insnNode)) index else null
        })
    }
}

class LineNumberTarget(private val searchedNumber: Int) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        return CursorsFixed(node.instructions.mapIndexedNotNull { index, insnNode ->
            if (insnNode is LineNumberNode && insnNode.line == searchedNumber) index else null
        })
    }
}
