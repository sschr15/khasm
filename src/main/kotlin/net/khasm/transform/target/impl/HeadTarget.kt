package net.khasm.transform.target.impl

import net.khasm.transform.target.AbstractKhasmTarget
import org.objectweb.asm.tree.MethodNode

class HeadTarget : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int> {
        return if (range.contains(0)) listOf(0) else emptyList()
    }
}
