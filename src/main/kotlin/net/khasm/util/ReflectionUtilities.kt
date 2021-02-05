@file:Suppress("unused")

package net.khasm.util

import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * A delegate to allow easier reflective access to a [field].
 * [&lt;T&gt;][T] is designed to be the type of the [field].
 *
 * [instance] is the object reference (like `MinecraftClient.getInstance()`),
 * or `null` if the field is static.
 */
class FieldReflectDelegate<T : Any>(private val field: Field, private val instance: Any?) {
    constructor(field: Field, instance: Any?, type: KClass<T>) : this(field, instance)

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, thing: KProperty<*>) = field.get(instance) as T?

    operator fun setValue(thisRef: Any?, thing: KProperty<*>, t: T?) = field.set(instance, t)

    init {
        field.isAccessible = true
    }
}

/**
 * A delegate to allow easier reflective access to a [method].
 * [&lt;T&gt;][T] is designed to be the return type of the [method].
 *
 * [instance] is the object reference (like `MinecraftClient.getInstance()`),
 * or `null` if the method is static.
 */
class MethodReflectDelegate<T : Any>(private val method: Method, private val instance: Any?) {
    constructor(method: Method, instance: Any?, returnType: KClass<T>) : this(method, instance)

    operator fun getValue(thisRef: Any?, thing: KProperty<*>) = MethodInsn()

    /**
     * An instance class to invoke [method] in a simple way.
     */
    inner class MethodInsn {
        @Suppress("UNCHECKED_CAST")
        operator fun invoke(vararg params: Any?) = method.invoke(instance, *params) as T?
    }

    init {
        method.isAccessible = true
    }
}
