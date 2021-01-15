package net.khasm.transform.target

import net.khasm.util.ANY
import net.khasm.util.higherValueZip
import org.objectweb.asm.tree.MethodNode

@Suppress("unused")
abstract class AbstractKhasmTarget {
    private var after: AbstractKhasmTarget? = null
    private var afterAction: TargetChainAction? = null

    private var cursorFilter: (List<Int>) -> List<Int> = {it}

    protected abstract fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int>

    private fun getFilteredCursors(range: IntRange, node: MethodNode): List<Int> {
        return cursorFilter(getPossibleCursors(range, node))
    }

    fun getCursors(node: MethodNode): List<Int> {
        if (after == null) {
            return getFilteredCursors(IntRange.ANY, node)
        }

        return when(afterAction) {
            TargetChainAction.INSIDE -> {
                val unpackedRanges = after?.getCursors(node)?.toMutableList() ?: return emptyList()
                val ranges = mutableListOf<IntRange>()
                while (unpackedRanges.isNotEmpty()) {
                    ranges.add(IntRange(unpackedRanges.removeFirst(), unpackedRanges.removeFirst()))
                }
                getFilteredCursors(IntRange.ANY, node).filter { int ->
                    ranges.any { range ->
                        range.contains(int)
                    }
                }
            }
            TargetChainAction.UNTIL -> {
                val startPoints = getFilteredCursors(IntRange.ANY, node).sorted()
                val stoppingPoints =
                    after?.getFilteredCursors(IntRange.ANY, node)?.sorted()
                        ?: return emptyList()
                val possible = higherValueZip(startPoints.toMutableList(), stoppingPoints.toMutableList())

                val out = mutableListOf<Int>()
                possible.forEach { out.add(it.first); out.add(it.second) }
                out
            }
            TargetChainAction.AND_OR -> {
                val output = getFilteredCursors(IntRange.ANY, node).toMutableList()
                output.addAll(after?.getFilteredCursors(IntRange.ANY, node) ?: emptyList())
                output
            }
            null -> emptyList()
        }
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

        after = other
        afterAction = TargetChainAction.INSIDE

        return this
    }

    infix fun until(other: AbstractKhasmTarget): AbstractKhasmTarget {
        println("UNTIL: $this -> $other")

        after = other
        afterAction = TargetChainAction.UNTIL

        return this
    }

    infix fun andOr(other: AbstractKhasmTarget): AbstractKhasmTarget {
        println("AND_OR: $this -> $other")

        after = other
        afterAction = TargetChainAction.AND_OR

        return this
    }
}
