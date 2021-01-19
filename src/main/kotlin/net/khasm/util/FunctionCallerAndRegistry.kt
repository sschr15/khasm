package net.khasm.util

import java.lang.reflect.Method
import kotlin.random.Random

/**
 * An internal class used for smart injects
 */
internal object FunctionCallerAndRegistry {
    private val functions: MutableList<Function<*>> = mutableListOf()

    fun addFunction(function: Function<*>): Int {
        if (function.typeArguments().size - 1 >= 1) {
            throw UnsupportedOperationException("$function has ${function.typeArguments().size - 1} parameters which is more than the currently supported amount")
        }
        functions.add(function)
        return functions.size - 1
    }

    @Suppress("unused")
    @JvmStatic
    fun callFunction(index: Int): Any? {
        when (val function = functions.getOrNull(index)) {
            is Function0 -> {
                return function()
            }

            // Doesn't work currently, also probably needs to be more generic
            is Function1<*, *>,
            is Function2<*, *, *>,
            is Function3<*, *, *, *> -> {
                val parameterTypes = function.typeArguments().toMutableList()
                parameterTypes.removeLast()

                val invoke = getInternalInvoke(function)
                return invoke(function, Random.nextInt())
            }

            null -> throw NullPointerException("Injected khasm function $function does not exist")
            else -> throw UnsupportedOperationException("Unable to call injected khasm function $function")
        }
    }

    private fun getInternalInvoke(function: Function<*>): Method {
        val method = function::class.java.methods.first { method -> method.name == "invoke" }
        method.isAccessible = true
        return method
    }
}
