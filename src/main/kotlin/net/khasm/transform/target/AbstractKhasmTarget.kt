package net.khasm.transform.target

import net.khasm.util.higherValueZip
import org.objectweb.asm.tree.MethodNode

@Suppress("unused")
abstract class AbstractKhasmTarget {
    private var before: AbstractKhasmTarget? = null
    private var beforeAction: TargetChainAction? = null

    private var after: AbstractKhasmTarget? = null
    private var afterAction: TargetChainAction? = null

    private var cursorFilter: (List<Int>) -> List<Int> = {it}

    protected abstract fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int>

    private fun getFilteredCursors(range: IntRange, node: MethodNode): List<Int> {
        return cursorFilter(getPossibleCursors(range, node))
    }

    fun getCursors(node: MethodNode): List<Int> {
        if (before == null && after == null) {
            return getFilteredCursors(IntRange(Int.MIN_VALUE, Int.MAX_VALUE), node)
        }

        if (after != null && afterAction == TargetChainAction.UNTIL) {
            val startPoints = getFilteredCursors(IntRange(Int.MIN_VALUE, Int.MAX_VALUE), node).sorted()
            val stoppingPoints =
                after?.getFilteredCursors(IntRange(Int.MIN_VALUE, Int.MAX_VALUE), node)?.sorted()
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
            return getFilteredCursors(IntRange(Int.MIN_VALUE, Int.MAX_VALUE), node).filter { int ->
                ranges.any { range ->
                    range.contains(int)
                }
            }
        }

        return emptyList()
    }

    fun first(): AbstractKhasmTarget {
        cursorFilter = {
            listOf(it.first())
        }
        return this
    }

    fun last(): AbstractKhasmTarget {
        cursorFilter = {
            listOf(it.last())
        }
        return this
    }

    fun ordinal(index: Int): AbstractKhasmTarget {
        cursorFilter = {
            listOf(it[index])
        }
        return this
    }

    fun filter(lambda: (List<Int>) -> List<Int>): AbstractKhasmTarget {
        cursorFilter = lambda
        return this
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
