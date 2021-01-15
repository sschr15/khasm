package net.khasm.transform.target.impl

import net.khasm.transform.target.AbstractKhasmTarget
import org.objectweb.asm.tree.MethodNode

class RawTarget(private vararg val cursors: Int) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int> {
        return cursors.asList().filter(range::contains)
    }
}
