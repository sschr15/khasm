package net.khasm.util

import codes.som.anthony.koffee.MethodAssembly
import codes.som.anthony.koffee.labels.LabelLike
import codes.som.anthony.koffee.labels.coerceLabel
import codes.som.anthony.koffee.types.TypeLike
import org.objectweb.asm.tree.LocalVariableNode

fun MethodAssembly.localVar(
    name: String, desc: TypeLike, signature: String?,
    start: LabelLike, end: LabelLike, index: Int
) = LocalVariableNode(name, coerceType(desc).descriptor, signature, coerceLabel(start), coerceLabel(end), index)
    .also { node.localVariables.add(it) }