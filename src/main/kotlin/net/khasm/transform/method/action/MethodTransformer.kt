package net.khasm.transform.method.action

import codes.som.anthony.koffee.MethodAssembly
import org.objectweb.asm.tree.AbstractInsnNode
import java.util.*

sealed class MethodTransformer(val type: MethodActionType) {
    val isOverwrite get(): Boolean {
        return when(type) {
            MethodActionType.RAW_OVERWRITE, MethodActionType.SMART_OVERWRITE -> true
            MethodActionType.SMART_INJECT, MethodActionType.RAW_INJECT -> false
        }
    }
}

class RawMethodTransformer(typeRaw: MethodActionType, val action: MethodAssembly.(AbstractInsnNode) -> Unit): MethodTransformer(typeRaw)

class SmartMethodTransformer(typeSmart: MethodActionType, val action: Function<*>) : MethodTransformer(typeSmart) {
    val internalName = "\$khasm\$smartInject\$${Integer.toHexString(Random().nextInt())}"
}

enum class MethodActionType {
    RAW_INJECT,
    RAW_OVERWRITE,
    SMART_INJECT,
    SMART_OVERWRITE
}
