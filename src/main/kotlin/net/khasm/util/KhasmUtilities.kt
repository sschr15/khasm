package net.khasm.util

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
//import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
//import java.lang.reflect.Type

internal val logger: Logger = LogManager.getLogger("khasm")

internal val IntRange.Companion.ANY: IntRange
    get() = IntRange(Int.MIN_VALUE, Int.MAX_VALUE)

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

class UnknownInsnNode : AbstractInsnNode(-1) {
    init {
        logger.error("There was a problem loading an instruction! A placeholder instruction has been used instead.")
    }

    override fun getType(): Int = -1

    override fun accept(methodVisitor: MethodVisitor?) = error("Tried to accept a nonexistent instruction!")

    override fun clone(clonedLabels: MutableMap<LabelNode, LabelNode>?) = UnknownInsnNode()
}

fun Boolean.toInt() = if (this) 1 else 0

/**
 * Returns the type arguments for a Function, last one is always the return type
 */
//fun Function<Unit>.typeArguments(): List<Type> = (this::class.java.genericInterfaces[0] as ParameterizedTypeImpl).actualTypeArguments.toList()
