package net.khasm.util

import codes.som.anthony.koffee.MethodAssembly
import codes.som.anthony.koffee.labels.LabelLike
import codes.som.anthony.koffee.labels.coerceLabel
import codes.som.anthony.koffee.modifiers.Modifiers
import codes.som.anthony.koffee.types.TypeLike
import org.objectweb.asm.tree.*

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

var MethodAssembly.access
    get() = Modifiers(node.access)
    set(value) {
        node.access = value.access
    }

var ClassNode.modifiers
    get() = Modifiers(access)
    set(value) {
        access = value.access
    }
var MethodNode.modifiers
    get() = Modifiers(access)
    set(value) {
        access = value.access
    }
var FieldNode.modifiers
    get() = Modifiers(access)
    set(value) {
        access = value.access
    }

fun Int.asModifiers() = Modifiers(this)

fun Modifiers.containsAll(modifiers: Modifiers) =
    (this.access and modifiers.access) == this.access
fun Modifiers.containsAny(modifiers: Modifiers) =
    (this.access and modifiers.access) != 0
fun Modifiers.containsNone(modifiers: Modifiers) =
    (this.access and modifiers.access) == 0
fun Modifiers.containsOthers(modifiers: Modifiers) =
    (this.access and modifiers.access.inv()) != 0

operator fun Modifiers.contains(modifier: Modifiers) =
    (this.access and modifier.access) != 0
operator fun Modifiers.contains(modifier: Int) =
    (this.access and modifier) != 0
