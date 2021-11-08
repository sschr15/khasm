@file:JvmName("KhasmRuntimeAPI")
@file:Suppress("unused")

package net.khasm.transform

import net.khasm.util.all
import net.khasm.util.logger
import user11681.reflect.Classes

// These methods exist for the purpose of making bytecode easier.
// These turn primitives into their object representations.
fun toObject(obj: Int): Any = obj
fun toObject(obj: Byte): Any = obj
fun toObject(obj: Short): Any = obj
fun toObject(obj: Long): Any = obj
fun toObject(obj: Double): Any = obj
fun toObject(obj: Float): Any = obj
fun toObject(obj: Boolean): Any = obj
fun toObject(obj: Char): Any = obj
fun toObject(obj: Any?): Any? = obj

// These turn objects into their primitive representations.
fun toI(obj: Any?): Int = obj as Int
fun toB(obj: Any?): Byte = obj as Byte
fun toS(obj: Any?): Short = obj as Short
fun toJ(obj: Any?): Long = obj as Long
fun toD(obj: Any?): Double = obj as Double
fun toF(obj: Any?): Float = obj as Float
fun toZ(obj: Any?): Boolean = obj as Boolean
fun toC(obj: Any?): Char = obj as Char

/**
 * Invoke a [function] with the given [args].
 */
fun <T> invoke(function: Function<T>, vararg args: Any?): T? {
    val method = function.javaClass.declaredMethods
        .firstOrNull { it.parameterTypes.mapIndexed { i: Int, clazz: Class<*>? -> clazz?.isInstance(args[i]) == true }.all() }
        ?: throw IllegalArgumentException("AAAAAAAAAAAA")
    method.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return method(function, *args) as? T
}

/**
 * Runtime comment.
 */
fun comment(comment: String) {
    logger.debug("Class comment: $comment")
}

/**
 * Force cast an [obj] to [class]
 */
fun cast(obj: Any?, `class`: Class<*>): Any? = Classes.reinterpret(obj, `class`)
