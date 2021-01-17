package net.khasm.transform.method.target

import net.khasm.util.ANY
import net.khasm.util.higherValueZip
import net.khasm.util.logger
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
                                first.forEach { int ->
                                    if (depends.ranges.any { it.contains(int) }) {
                                        out.add(int)
                                    }
                                }
                                CursorsFixed(out.sorted())
                            }
                        }
                    }
                    TargetChainAction.EXCLUDING -> {
                        val first = getFilteredCursors(IntRange.ANY, node).points.sorted()

                        when (val depends = dependsOn!!.getCursors(node)) {
                            is CursorsFixed -> throw UnsupportedOperationException("Can't use inside on CursorsFixed! (Create a range with until, and/or make sure your range is grouped with ()!)")
                            is CursorRanges -> {
                                val out: MutableList<Int> = mutableListOf()
                                first.forEach { int ->
                                    if (depends.ranges.none { it.contains(int) }) {
                                        out.add(int)
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
        verifyNotSet()

        dependsOn = other
        dependentAction = TargetChainAction.INSIDE

        logger.info("INSIDE: $this")
        return this
    }

    /**
     * Only targets that do not fall within the range of [other]
     *
     * If [other] does not return a [CursorRanges] this will throw
     */
    infix fun excluding(other: AbstractKhasmTarget): AbstractKhasmTarget {
        verifyNotSet()

        dependsOn = other
        dependentAction = TargetChainAction.EXCLUDING

        logger.info("EXCLUDING: $this")

        return this
    }

    /**
     * Generates a [CursorRanges] between the 2 targets using [higherValueZip]
     *
     * This can not be passed directly! Use [inside] or custom logic to make this passable
     */
    infix fun until(other: AbstractKhasmTarget): AbstractKhasmTarget {
        verifyNotSet()

        dependsOn = other
        dependentAction = TargetChainAction.UNTIL

        logger.info("UNTIL: $this")

        return this
    }

    /**
     * All targets matched by either this or [other]
     */
    infix fun andOr(other: AbstractKhasmTarget): AbstractKhasmTarget {
        verifyNotSet()

        dependsOn = other
        dependentAction = TargetChainAction.AND_OR

        logger.info("AND_OR: $this")

        return this
    }

    private fun verifyNotSet() {
        if (dependsOn != null || dependentAction != null) {
            throw UnsupportedOperationException("Improper chaining! $this is already chained to $dependsOn through $dependentAction! (Try using () to group targets into proper order)")
        }
    }

    override fun toString(): String {
        return (this::class.simpleName ?: "MISSING") + if (dependsOn != null) "($dependentAction -> $dependsOn)" else ""
    }
}
