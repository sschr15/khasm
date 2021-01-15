package net.khasm.transform.target.impl

import net.khasm.transform.target.AbstractKhasmTarget
import org.objectweb.asm.tree.MethodNode

class ReturnTarget : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int> {
        val output = mutableListOf<Int>()
        node.instructions.forEachIndexed { index, it ->
            if (it.opcode in 172..177) {
                output.add(index - 1)
            }
        }
        return output
    }
}
