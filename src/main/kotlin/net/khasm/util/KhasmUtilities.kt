package net.khasm.util

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

internal val logger: Logger = LogManager.getLogger("khasm")

internal val IntRange.Companion.ANY: IntRange
    get() {return IntRange(Int.MIN_VALUE, Int.MAX_VALUE)}

/**
 * Assumes sorted input lists.
 *
 * For each value in A, remove first value in B until it is of equal or higher value
 * Then take the value from A and equal or higher of B and add to output, then continue with the next set
 */
fun higherValueZip(A: MutableList<Int>, B: MutableList<Int>): List<Pair<Int, Int>> {
    val output = mutableListOf<Pair<Int, Int>>()
    outer@ do {
        val minimum = A.removeFirst()
        var possible: Int
        do {
            possible = B.removeFirstOrNull() ?: break@outer
        } while (possible < minimum)
        output.add(Pair(minimum, possible))
    } while (A.isNotEmpty() && B.isNotEmpty())
    return output
}
