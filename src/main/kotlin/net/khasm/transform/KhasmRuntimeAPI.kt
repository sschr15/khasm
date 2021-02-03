@file:JvmName("KhasmRuntimeAPI")

package net.khasm.transform

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
    val method = function.javaClass.declaredMethods[1]
    method.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return method(function, *args) as? T
}