package net.khasm.util

/**
 * An internal class used for smart injects
 */
internal object FunctionCallerAndRegistry {
    private val functions: MutableList<Function<Unit>> = mutableListOf()

    fun addFunction(function: Function<Unit>): Int {
        if (function.typeArguments().size > 1) {
            throw UnsupportedOperationException("$function has ${function.typeArguments().size - 1} parameters which is more than the currently supported amount")
        }
        functions.add(function)
        return functions.size - 1
    }

    @JvmStatic
    fun callFunction(index: Int) {
        when (val function = functions[index]) {
            is Function0 -> function()
            else -> throw UnsupportedOperationException("Unable to call injected khasm function $function")
        }
    }
}
