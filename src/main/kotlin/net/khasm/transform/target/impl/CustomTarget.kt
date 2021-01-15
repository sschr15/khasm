package net.khasm.transform.target.impl

import net.khasm.transform.target.AbstractKhasmTarget
import org.objectweb.asm.tree.MethodNode

class CustomTarget(private val lambda: MethodNode.() -> List<Int>) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int> {
        return lambda(node).filter(range::contains)
    }
}
