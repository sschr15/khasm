package net.khasm.transform.target

import org.objectweb.asm.tree.MethodNode

abstract class AbstractKhasmTarget {
    private var before: AbstractKhasmTarget? = null
    private var beforeAction: TargetChainAction? = null

    private var after: AbstractKhasmTarget? = null
    private var afterAction: TargetChainAction? = null

    protected abstract fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int>

    fun getCursors(node: MethodNode): List<Int> {
        if (before == null && after == null) {
            return getPossibleCursors(IntRange(Int.MIN_VALUE, Int.MAX_VALUE), node)
        }
        return emptyList()
    }

    infix fun inside(other: AbstractKhasmTarget): AbstractKhasmTarget {
        println("INSIDE: $this -> $other")

        after = other
        afterAction = TargetChainAction.INSIDE

        return other
    }

    infix fun until(other: AbstractKhasmTarget): AbstractKhasmTarget {
        println("UNTIL: $this -> $other")

        other.before = this
        other.beforeAction = TargetChainAction.UNTIL

        after = other
        afterAction = TargetChainAction.UNTIL

        return other
    }
}
