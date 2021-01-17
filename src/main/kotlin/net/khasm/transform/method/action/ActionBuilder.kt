package net.khasm.transform.method.action

import codes.som.anthony.koffee.MethodAssembly
import org.objectweb.asm.tree.AbstractInsnNode

class ActionBuilder(method: ActionBuilder.() -> Unit) {
    var methodTransformer: MethodTransformer? = null

    init {
        method()
    }

    fun rawInject(action: MethodAssembly.(AbstractInsnNode) -> Unit) {
        verifyNotSet()
        methodTransformer = RawMethodTransformer(MethodActionType.RAW_INJECT, action)
    }

    fun rawOverwrite(action: MethodAssembly.(AbstractInsnNode) -> Unit) {
        verifyNotSet()
        methodTransformer = RawMethodTransformer(MethodActionType.RAW_OVERWRITE, action)
    }

    fun smartInject(action: Function<Unit>) {
        verifyNotSet()
        methodTransformer = SmartMethodTransformer(MethodActionType.SMART_INJECT, action)
    }

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
