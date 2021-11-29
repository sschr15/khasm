package net.khasm.transform.method.action

import codes.som.anthony.koffee.MethodAssembly
import codes.som.anthony.koffee.types.TypeLike
import net.khasm.annotation.DangerousKhasmUsage
import org.objectweb.asm.tree.AbstractInsnNode
import java.lang.reflect.Method

typealias RawAction = MethodAssembly.(AbstractInsnNode) -> Unit

/**
 * Allows you to write and inject raw ASM into the positions specified by the transformer's target.
 */
fun rawInject(action: RawAction): MethodTransformer {
    return RawMethodTransformer(MethodActionType.RAW_INJECT, action)
}

/**
 * Allows you to completely replace the target method with the specified ASM.
 */
fun rawOverwrite(action: RawAction): MethodTransformer {
    return RawMethodTransformer(MethodActionType.RAW_OVERWRITE, action)
}

/**
 * Allows you to inject normal code (partially) in the target method.
 */
fun smartInject(vararg localVariableTypes: TypeLike, action: Function<*>): MethodTransformer {
    return SmartMethodTransformer(MethodActionType.SMART_INJECT, action, *localVariableTypes)
}

/**
 * Allows you to completely replace the target method with code which calls the specified method.
 */
@DangerousKhasmUsage("""
    This is a very dangerous method to use.
    It will completely replace the method with the code specified by the lambda/function.
    This is not compatible with further transformations or mixins.
""")
fun oldSmartOverwrite(action: Function<*>): MethodTransformer {
    return SmartMethodTransformer(MethodActionType.SMART_OVERWRITE, action)
}

/**
 * Allows you to replace the target method with the specified method's code,
 * rather than with a reference to the method.
 */
fun smartOverwrite(method: Method): MethodTransformer {
    return SmarterOverrideTransformer(method)
}
