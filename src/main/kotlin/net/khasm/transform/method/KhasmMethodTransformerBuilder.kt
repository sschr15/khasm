package net.khasm.transform.method

import codes.som.anthony.koffee.insns.InstructionAssembly
import codes.som.anthony.koffee.insns.jvm.checkcast
import codes.som.anthony.koffee.insns.jvm.invokestatic
import codes.som.anthony.koffee.insns.jvm.ldc
import codes.som.anthony.koffee.types.TypeLike
import codes.som.anthony.koffee.types.coerceType
import net.khasm.annotation.DangerousKhasmUsage
import net.khasm.transform.method.action.*
import net.khasm.transform.method.target.AbstractKhasmTarget
import net.khasm.transform.method.target.AtTarget
import net.khasm.util.mapClass
import net.khasm.util.mapMethod
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.spongepowered.asm.mixin.injection.At
import user11681.reflect.Classes

@Suppress("unused", "MemberVisibilityCanBePrivate")
class KhasmMethodTransformerBuilder(method: KhasmMethodTransformerBuilder.() -> Unit, modid: String) {
    private val working = KhasmMethodTransformer(modid)
    private val legacyActionTarget = object {
        var targetPredicate: AbstractKhasmTarget? = null
        @Suppress("DEPRECATION")
        var action: (ActionBuilder.() -> Unit)? = null
        var built = false
    }

    /**
     * Acts like [checkcast(TypeLike)][codes.som.anthony.koffee.insns.jvm.checkcast]
     * but it does a call to a different method to forcefully cast it to the designated type instead.
     *
     * This is intended for ClassLoader-related problems, but it could be used for other purposes as well
     */
    fun InstructionAssembly.forcecast(type: TypeLike) {
        ldc(coerceType(type))
        invokestatic(Classes::class, "reinterpret", Any::class, Any::class, Class::class)
        checkcast(type)
    }

    init {
        method()
    }

    fun classTarget(targetClass: String) {
        classTarget {
            name.replace('/', '.') == mapClass(targetClass)
        }
        working.oneTimeUse = true
    }

    fun classTarget(lambda: ClassNode.() -> Boolean) {
        working.setClassPredicate(lambda)
    }

    @DangerousKhasmUsage("""
        The `skipDescriptorChecks` parameter is intended for use in cases where
        the method descriptor may need to be remapped.
        As a result, this method may match more methods than expected.
    """)
    fun methodTarget(owner: String, intermediary: String, descriptor: String, skipDescriptorChecks: Boolean) {
        methodTarget {
            name == mapMethod(owner, intermediary, descriptor) && (skipDescriptorChecks || desc == descriptor)
        }
    }

    @OptIn(DangerousKhasmUsage::class)
    fun methodTarget(owner: String, intermediary: String, descriptor: String) {
        methodTarget(owner, intermediary, descriptor, false)
    }

    fun methodTarget(lambda: MethodNode.() -> Boolean) {
        working.setMethodPredicate(lambda)
    }

    /**
     * Add an injection to this transformer.
     * [`code`][code] can be generated from [rawInject], [rawOverwrite], [smartInject], [smartOverwrite], or [oldSmartOverwrite]
     */
    fun addInject(action: AbstractKhasmTarget, code: MethodTransformer) {
        working.actions.getOrPut(action) { mutableListOf() }.add(code)
    }

    @Deprecated("Use addInject with the target instead", ReplaceWith("addInject(target, code)"), DeprecationLevel.WARNING)
    fun target(lambda: () -> AbstractKhasmTarget) {
        legacyActionTarget.targetPredicate = lambda()
        if (legacyActionTarget.action != null && legacyActionTarget.targetPredicate != null && !legacyActionTarget.built) {
            @Suppress("DEPRECATION")
            addInject(legacyActionTarget.targetPredicate!!, ActionBuilder(legacyActionTarget.action!!).methodTransformer!!)
            legacyActionTarget.built = true
        }
    }

    @DangerousKhasmUsage("""
        Kotlin currently requires *all* fields to be specified in the annotation,
        including default values. This means that all fields, including the ones
        that you don't use, must be specified.
        
        If (when) Kotlin gets a chance to support default values, this
        annotation will be removed.
    """)
    @Deprecated(
        "Use addInject with the target instead",
        ReplaceWith(
            "addInject(AtTarget(at), code)",
            "net.khasm.transform.method.target.AtTarget"
        ),
        DeprecationLevel.WARNING
    )
    fun target(at: At) {
        legacyActionTarget.targetPredicate = AtTarget(at)
        if (legacyActionTarget.action != null && legacyActionTarget.targetPredicate != null && !legacyActionTarget.built) {
            @Suppress("DEPRECATION")
            addInject(legacyActionTarget.targetPredicate!!, ActionBuilder(legacyActionTarget.action!!).methodTransformer!!)
            legacyActionTarget.built = true
        }
    }

    @Deprecated("Use addInject with the target instead", ReplaceWith("addInject(target, code)"), DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    fun action(action: ActionBuilder.() -> Unit) {
        legacyActionTarget.action = action
        if (legacyActionTarget.action != null && legacyActionTarget.targetPredicate != null && !legacyActionTarget.built) {
            addInject(legacyActionTarget.targetPredicate!!, ActionBuilder(legacyActionTarget.action!!).methodTransformer!!)
            legacyActionTarget.built = true
        }
    }

    fun build(): KhasmMethodTransformer {
        return working
    }
}
