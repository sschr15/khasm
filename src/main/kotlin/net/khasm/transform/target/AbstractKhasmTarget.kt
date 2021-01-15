package net.khasm.transform.target

import net.khasm.util.higherValueZip
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

        if (after != null && afterAction == TargetChainAction.UNTIL) {
            val startPoints = getPossibleCursors(IntRange(Int.MIN_VALUE, Int.MAX_VALUE), node).sorted()
            val stoppingPoints =
                after?.getPossibleCursors(IntRange(Int.MIN_VALUE, Int.MAX_VALUE), node)?.sorted()
                    ?: return emptyList() // Annoying hacks because mutable
            val possible = higherValueZip(startPoints.toMutableList(), stoppingPoints.toMutableList())

            val out = mutableListOf<Int>()
            possible.forEach { out.add(it.first); out.add(it.second) }
            return out
        }

        if (after != null && afterAction == TargetChainAction.INSIDE) {
            val unpackedRanges = after?.getCursors(node)?.toMutableList() ?: return emptyList()
            val ranges = mutableListOf<IntRange>()
            while (unpackedRanges.isNotEmpty()) {
                ranges.add(IntRange(unpackedRanges.removeFirst(), unpackedRanges.removeFirst()))
            }
            return getPossibleCursors(IntRange(Int.MIN_VALUE, Int.MAX_VALUE), node).filter { int ->
                ranges.any { range ->
                    range.contains(int)
                }
            }
        }

        return emptyList()
    }

    infix fun inside(other: AbstractKhasmTarget): AbstractKhasmTarget {
        println("INSIDE: $this -> $other")

        other.before = this
        other.beforeAction = TargetChainAction.INSIDE

        after = other
        afterAction = TargetChainAction.INSIDE

        return this
    }

    infix fun until(other: AbstractKhasmTarget): AbstractKhasmTarget {
        println("UNTIL: $this -> $other")

        other.before = this
        other.beforeAction = TargetChainAction.UNTIL

        after = other
        afterAction = TargetChainAction.UNTIL

        return this
    }
}
