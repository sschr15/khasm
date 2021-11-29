package net.khasm.transform.method.action

import codes.som.anthony.koffee.MethodAssembly
import codes.som.anthony.koffee.sugar.TypesAccess
import codes.som.anthony.koffee.types.TypeLike
import net.khasm.annotation.DangerousKhasmUsage
import org.objectweb.asm.tree.AbstractInsnNode

// TypesAccess interface is for accessing the `int` and other primitive types for smartInject
@Deprecated(
    """
        ActionBuilder is now deprecated, generate transformations with the
        methods in `TransformerMethods` instead.
        """,
    level = DeprecationLevel.WARNING // for now
)
class ActionBuilder(@Suppress("DEPRECATION") method: ActionBuilder.() -> Unit) : TypesAccess {
    var methodTransformer: MethodTransformer? = null

    init {
        method()
    }

    /**
     * Allows you to write and inject raw ASM into the positions specified by targets
     */
    @Deprecated(
        """
            ActionBuilder is now deprecated, generate transformations with the
            methods in `TransformerMethods` instead.
        """,
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("rawInject(action)", "net.khasm.transform.method.action.rawInject")
    )
    fun rawInject(action: MethodAssembly.(AbstractInsnNode) -> Unit) {
        verifyNotSet()
        methodTransformer = RawMethodTransformer(MethodActionType.RAW_INJECT, action)
    }

    /**
     * Allows you to completely replace the bytecode of a method with raw ASM
     */
    @Deprecated(
        """
            ActionBuilder is now deprecated, generate transformations with the
            methods in `TransformerMethods` instead.
        """,
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("rawOverwrite(action)", "net.khasm.transform.method.action.rawOverwrite")
    )
    fun rawOverwrite(action: MethodAssembly.(AbstractInsnNode) -> Unit) {
        verifyNotSet()
        methodTransformer = RawMethodTransformer(MethodActionType.RAW_OVERWRITE, action)
    }

    //TODO get local variables working again
    /**
     * Allows you to inject the method with ASM that calls the provided lambda/function specified by targets
     */
    @Deprecated(
        """
            ActionBuilder is now deprecated, generate transformations with the
            methods in `TransformerMethods` instead.
        """,
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("smartInject(action)", "net.khasm.transform.method.action.smartInject")
    )
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
    @Deprecated(
        """
            ActionBuilder is now deprecated, generate transformations with the
            methods in `TransformerMethods` instead.
        """,
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("oldSmartOverwrite(action)", "net.khasm.transform.method.action.oldSmartOverwrite")
    )
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
