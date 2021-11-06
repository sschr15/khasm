@file:JvmName("KhasmRuntimeAPI")
@file:Suppress("unused")

package net.khasm.transform

import net.khasm.util.all
import net.khasm.util.logger
import user11681.reflect.Classes

// These methods exist for the purpose of making bytecode easier.
// These automatically turn all primitives into their object representations.
fun toObject(obj: Int): Any {
    return obj
}

fun toObject(obj: Byte): Any {
    return obj
}

fun toObject(obj: Short): Any {
    return obj
}

fun toObject(obj: Long): Any {
    return obj
}

fun toObject(obj: Double): Any {
    return obj
}

fun toObject(obj: Float): Any {
    return obj
}

fun toObject(obj: Boolean): Any {
    return obj
}

fun toObject(obj: Char): Any {
    return obj
}

fun toObject(obj: Any?): Any? {
    return obj
}

fun <T> invoke(function: Function<T>, vararg args: Any?): T? {
    val method = function.javaClass.declaredMethods
        .firstOrNull { it.parameterTypes.mapIndexed { i: Int, clazz: Class<*>? -> clazz?.isInstance(args[i]) == true }.all() }
        ?: throw IllegalArgumentException("AAAAAAAAAAAA")
    method.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return method(function, *args) as? T
}

fun comment(comment: String) {
    logger.debug("Class comment: $comment")
}

fun cast(obj: Any?, `class`: Class<*>): Any? = Classes.reinterpret(obj, `class`)
