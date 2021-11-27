package net.khasm.transform.method.action

import codes.som.anthony.koffee.MethodAssembly
import codes.som.anthony.koffee.sugar.TypesAccess
import codes.som.anthony.koffee.types.TypeLike
import net.khasm.annotation.DangerousKhasmUsage
import org.objectweb.asm.tree.AbstractInsnNode

// TypesAccess interface is for accessing the `int` and other primitive types for smartInject
class ActionBuilder(method: ActionBuilder.() -> Unit) : TypesAccess {
    var methodTransformer: MethodTransformer? = null

    init {
        method()
    }

    /**
     * Allows you to write and inject raw ASM into the positions specified by targets
     */
    fun rawInject(action: MethodAssembly.(AbstractInsnNode) -> Unit) {
        verifyNotSet()
        methodTransformer = RawMethodTransformer(MethodActionType.RAW_INJECT, action)
    }

    /**
     * Allows you to completely replace the bytecode of a method with raw ASM
     */
    fun rawOverwrite(action: MethodAssembly.(AbstractInsnNode) -> Unit) {
        verifyNotSet()
        methodTransformer = RawMethodTransformer(MethodActionType.RAW_OVERWRITE, action)
    }

    //TODO get local variables working again
    /**
     * Allows you to inject the method with ASM that calls the provided lambda/function specified by targets
     */
    fun smartInject(vararg localVariableTypes: TypeLike, action: Function<*>) {
        verifyNotSet()
        methodTransformer = SmartMethodTransformer(MethodActionType.SMART_INJECT, action, *localVariableTypes)
    }

    /**
     * Allows you to overwrite the method with ASM that calls the provided lambda/function.
     */
    @DangerousKhasmUsage("""
        This is a very dangerous method to use.
        It will completely replace the method with the code specified by the lambda/function.
        This is not compatible with further transformations or mixins.
    """)
    fun smartOverwrite(action: Function<*>) {
        verifyNotSet()
        methodTransformer = SmartMethodTransformer(MethodActionType.SMART_OVERWRITE, action)
    }

    private fun verifyNotSet() {
        if (methodTransformer != null) {
            throw UnsupportedOperationException("Multiple method actions are not supported")
        }
    }
}
