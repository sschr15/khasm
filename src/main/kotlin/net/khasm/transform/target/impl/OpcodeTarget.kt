package net.khasm.transform.target.impl

import net.khasm.transform.target.AbstractKhasmTarget
import org.objectweb.asm.tree.MethodNode

class OpcodeTarget(private val opcode: Int) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int> {
        val output = mutableListOf<Int>()
        node.instructions.forEachIndexed { index, it ->
            if (it.opcode == opcode) {
                output.add(index - 1)
            }
        }
        return output
    }
}
