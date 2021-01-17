package net.khasm.transform.method.target

sealed class Cursors

class CursorRanges(val ranges: List<IntRange>): Cursors()

class CursorsFixed(val points: List<Int>): Cursors() {
    constructor(vararg points: Int) : this(points.toList())
}
