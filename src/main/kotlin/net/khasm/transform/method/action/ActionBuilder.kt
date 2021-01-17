package net.khasm.transform.method.action

import codes.som.anthony.koffee.MethodAssembly
import org.objectweb.asm.tree.AbstractInsnNode

class ActionBuilder(method: ActionBuilder.() -> Unit) {
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

    /**
     * Allows you to inject the method with ASM that calls the provided lambda/function specified by targets
     */
    fun smartInject(action: Function<Unit>) {
        verifyNotSet()
        methodTransformer = SmartMethodTransformer(MethodActionType.SMART_INJECT, action)
    }

    /**
     * Allows you to overwrite the method with ASM that calls the provided lambda/function
     */
    fun smartOverwrite(action: Function<Unit>) {
        verifyNotSet()
        methodTransformer = SmartMethodTransformer(MethodActionType.SMART_OVERWRITE, action)
    }

    private fun verifyNotSet() {
        if (methodTransformer != null) {
            throw UnsupportedOperationException("Multiple method actions are not supported")
        }
    }
}
