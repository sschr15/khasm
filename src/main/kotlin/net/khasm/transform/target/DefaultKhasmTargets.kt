@file:Suppress("unused")

package net.khasm.transform.target

import org.objectweb.asm.tree.MethodNode

/**
 * Inject with your own logic.
 *
 * If injecting this way, consider making your
 * own implementation of [AbstractKhasmTarget]
 * if you'd like to reuse the same logic.
 */
class CustomTarget(private val lambda: MethodNode.() -> List<Int>) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int> {
        return lambda(node).filter(range::contains)
    }
}

/**
 * Inject a certain distance into the method.
 *
 * Note: this can be dangerous as other transformers
 * may have already changed the method.
 */
class RawTarget(private vararg val cursors: Int) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int> {
        return cursors.asList().filter(range::contains)
    }
}

/**
 * Inject at every instance of an opcode.
 */
class OpcodeTarget(private val opcode: Int) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int> {
        val output = mutableListOf<Int>()
        node.instructions.forEachIndexed { index, it ->
            if (it.opcode == opcode) {
                output.add(index )
            }
        }
        return output
    }
}