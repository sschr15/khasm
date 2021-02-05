package net.khasm.util

import codes.som.anthony.koffee.MethodAssembly
import codes.som.anthony.koffee.labels.LabelLike
import codes.som.anthony.koffee.labels.coerceLabel
import codes.som.anthony.koffee.types.TypeLike
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.TryCatchBlockNode

fun MethodAssembly.localVar(
    name: String, desc: TypeLike, signature: String?,
    start: LabelLike, end: LabelLike, index: Int
) = LocalVariableNode(name, coerceType(desc).descriptor, signature, coerceLabel(start), coerceLabel(end), index)
    .also { node.localVariables.add(it) }

/**
 * Create a try/catch block which catches [type] exceptions
 * (or `null` for finally blocks), starts at [start] (inclusive),
 * ends at [end] (exclusive), and is handled at [handler].
 */
fun MethodAssembly.tryCatchBlock(start: LabelLike, end: LabelLike, handler: LabelLike, type: TypeLike?) {
    val a = coerceLabel(start)
    val b = coerceLabel(end)
    val c = coerceLabel(handler)
    val t = type?.let { coerceType(it).internalName }

    tryCatchBlocks.add(TryCatchBlockNode(a, b, c, t))
}
