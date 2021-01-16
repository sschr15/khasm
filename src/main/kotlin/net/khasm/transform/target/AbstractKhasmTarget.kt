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
    private var dependsOn: AbstractKhasmTarget? = null
    private var dependentAction: TargetChainAction? = null

    private var cursorFilter: (CursorsFixed) -> CursorsFixed = { it }

    protected abstract fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed

    private fun getFilteredCursors(range: IntRange, node: MethodNode): CursorsFixed {
        return cursorFilter(getPossibleCursors(range, node))
    }

    /**
     * Returns all the valid target locations for [node]
     */
    fun getCursors(node: MethodNode): Cursors {
        return if (dependsOn == null) {
            getFilteredCursors(IntRange.ANY, node)
        } else {
            if (dependsOn != null) {
                when (dependentAction) {
                    TargetChainAction.INSIDE -> {
                        val first = getFilteredCursors(IntRange.ANY, node).points.sorted()

                        when (val depends = dependsOn!!.getCursors(node)) {
                            is CursorsFixed -> throw UnsupportedOperationException("Can't use inside on CursorsFixed! (Create a range with until, and/or make sure your range is grouped with ()!)")
                            is CursorRanges -> {
                                val out: MutableList<Int> = mutableListOf()

                                first.forEach {
                                    if (depends.ranges.any { intRange ->
                                        intRange.contains(it)
                                    }) {
                                        out.add(it)
                                    }
                                }

                                CursorsFixed(out.sorted())
                            }
                        }
                    }
                    TargetChainAction.UNTIL -> {
                        val first = getFilteredCursors(IntRange.ANY, node).points.sorted()
                        val depends = dependsOn!!.getFilteredCursors(IntRange.ANY, node).points.sorted()

                        val zip = higherValueZip(first.toMutableList(), depends.toMutableList())

                        val out: MutableList<IntRange> = mutableListOf()

                        for (pair in zip) {
                            out.add(IntRange(pair.first, pair.second))
                        }

                        CursorRanges(out)
                    }
                    TargetChainAction.AND_OR -> {
                        val first = getFilteredCursors(IntRange.ANY, node).points.sorted()
                        val depends = dependsOn!!.getFilteredCursors(IntRange.ANY, node).points.sorted()
                        val mut = first.toMutableList()
                        mut.addAll(depends)
                        CursorsFixed(mut)
                    }
                    null -> CursorsFixed()
                }
            } else {
                CursorsFixed()
            }
        }
    }

    /**
     * Give only the first target.
     */
    fun first(): AbstractKhasmTarget {
        cursorFilter = {
            CursorsFixed(it.points.first())
        }
        return this
    }

    /**
     * Give only the last target.
     */
    fun last(): AbstractKhasmTarget {
        cursorFilter = {
            CursorsFixed(it.points.last())
        }
        return this
    }

    /**
     * Give only the [index]th target.
     */
    fun ordinal(index: Int): AbstractKhasmTarget {
        cursorFilter = {
            CursorsFixed(it.points[index])
        }
        return this
    }

    /**
     * Given a list of targets, return an interpreted list of targets.
     *
     * One example for this might be to use the offset of the reported targets:
     * ```
     * target.filter { cursorsFixed -> CursorsFixed(cursorsFixed.points.map { it + 1 }) }
     * ```
     */
    fun filter(lambda: (CursorsFixed) -> CursorsFixed): AbstractKhasmTarget {
        cursorFilter = lambda
        return this
    }

    /**
     * Only targets that also fall within the range of [other]
     *
     * If [other] does not return a [CursorRanges] this will throw
     */
    infix fun inside(other: AbstractKhasmTarget): AbstractKhasmTarget {
        println("INSIDE: $this -> $other")

        verifyNotSet()

        dependsOn = other
        dependentAction = TargetChainAction.INSIDE

        return this
    }

    /**
     * Generates a [CursorRanges] between the 2 targets using [higherValueZip]
     *
     * This can not be passed directly! Use [inside] or custom logic to make this passable
     */
    infix fun until(other: AbstractKhasmTarget): AbstractKhasmTarget {
        println("UNTIL: $this -> $other")

        verifyNotSet()

        dependsOn = other
        dependentAction = TargetChainAction.UNTIL

        return this
    }

    /**
     * All targets matched by either this or [other]
     */
    infix fun andOr(other: AbstractKhasmTarget): AbstractKhasmTarget {
        println("AND_OR: $this -> $other")

        verifyNotSet()

        dependsOn = other
        dependentAction = TargetChainAction.AND_OR

        return this
    }

    private fun verifyNotSet() {
        if (dependsOn != null || dependentAction != null) {
            throw UnsupportedOperationException("Improper chaining! $this is already chained to $dependsOn through $dependentAction! (Try using () to group targets into proper order)")
        }
    }
}
