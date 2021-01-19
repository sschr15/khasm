package net.khasm.transform.method.action

import codes.som.anthony.koffee.MethodAssembly
import org.objectweb.asm.tree.AbstractInsnNode

sealed class MethodTransformer(val type: MethodActionType) {
    fun isOverwrite(): Boolean {
        return when(type) {
            MethodActionType.RAW_OVERWRITE, MethodActionType.SMART_OVERWRITE -> true
            MethodActionType.SMART_INJECT, MethodActionType.RAW_INJECT -> false
        }
    }
}

data class RawMethodTransformer(val typeRaw: MethodActionType, val action: MethodAssembly.(AbstractInsnNode) -> Unit): MethodTransformer(typeRaw)

data class SmartMethodTransformer(val typeSmart: MethodActionType, val action: Function<*>) : MethodTransformer(typeSmart)

enum class MethodActionType {
    RAW_INJECT,
    RAW_OVERWRITE,
    SMART_INJECT,
    SMART_OVERWRITE
}
