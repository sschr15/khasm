package net.khasm.transform.target

import net.khasm.util.ANY
import net.khasm.util.higherValueZip
import org.objectweb.asm.tree.MethodNode

/**
 * A Khasm injection target.
 *
 * This is used for defining where to inject a
 * Khasm transformer's code. Many targets present
 * are meant to imitate the default Mixin targets
 * ([HeadTarget], [ReturnTarget]). However, since
 * this is able to inject anywhere within the
 * method body, a few other targets are designed
 * for more control over where exactly to inject
 * ([RawTarget], [CustomTarget]).
 */
@Suppress("unused")
abstract class AbstractKhasmTarget {
    private var after: AbstractKhasmTarget? = null
    private var afterAction: TargetChainAction? = null

    private var cursorFilter: (List<Int>) -> List<Int> = { it }

    protected abstract fun getPossibleCursors(range: IntRange, node: MethodNode): List<Int>

    private fun getFilteredCursors(range: IntRange, node: MethodNode): List<Int> {
        return cursorFilter(getPossibleCursors(range, node))
    }

    /**
     * Returns all the valid target locations for [node]
     */
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

    /**
     * Give only the first target.
     */
    fun first(): AbstractKhasmTarget {
        cursorFilter = {
            listOf(it.first())
        }
        return this
    }

    /**
     * Give only the last target.
     */
    fun last(): AbstractKhasmTarget {
        cursorFilter = {
            listOf(it.last())
        }
        return this
    }

    /**
     * Give only the [index]th target.
     */
    fun ordinal(index: Int): AbstractKhasmTarget {
        cursorFilter = {
            listOf(it[index])
        }
        return this
    }

    /**
     * Given a list of targets, return an interpreted list of targets.
     *
     * One example for this might be to use the offset of the reported targets:
     * ```
     * target.filter { it.map { n -> n + 1 } }
     * ```
     */
    fun filter(lambda: (List<Int>) -> List<Int>): AbstractKhasmTarget {
        cursorFilter = lambda
        return this
    }

    /**
     * Only targets that also fall within the options of [other]
     */
    infix fun inside(other: AbstractKhasmTarget): AbstractKhasmTarget {
        println("INSIDE: $this -> $other")

        after = other
        afterAction = TargetChainAction.INSIDE

        return this
    }

    @Deprecated("Current implementation is broken and does not work as the intended result.")
    infix fun until(other: AbstractKhasmTarget): AbstractKhasmTarget {
        println("UNTIL: $this -> $other")

        after = other
        afterAction = TargetChainAction.UNTIL

        return this
    }

    /**
     * All targets matched by either this or [other]
     */
    infix fun andOr(other: AbstractKhasmTarget): AbstractKhasmTarget {
        println("AND_OR: $this -> $other")

        after = other
        afterAction = TargetChainAction.AND_OR

        return this
    }
}
