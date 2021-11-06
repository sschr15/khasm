package net.khasm.transform.method

import codes.som.anthony.koffee.insns.InstructionAssembly
import codes.som.anthony.koffee.insns.jvm.checkcast
import codes.som.anthony.koffee.insns.jvm.invokestatic
import codes.som.anthony.koffee.insns.jvm.ldc
import codes.som.anthony.koffee.types.TypeLike
import codes.som.anthony.koffee.types.coerceType
import net.khasm.annotation.DangerousKhasmUsage
import net.khasm.transform.method.action.ActionBuilder
import net.khasm.transform.method.target.AbstractKhasmTarget
import net.khasm.util.mapClass
import net.khasm.util.mapMethod
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import user11681.reflect.Classes

@Suppress("unused", "MemberVisibilityCanBePrivate")
class KhasmMethodTransformerBuilder(method: KhasmMethodTransformerBuilder.() -> Unit, modid: String) {
    private val working = KhasmMethodTransformer(modid)

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

    /**
     * [skipDescriptorChecks] is used for if the descriptor may need to be remapped.
     * This overloaded method is marked as dangerous as it may match more methods than expected.
     */
    @DangerousKhasmUsage
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

    fun target(lambda: () -> AbstractKhasmTarget) {
        working.setTargetPredicate(lambda())
    }

    fun action(action: ActionBuilder.() -> Unit) {
        working.setAction(action)
    }

    fun build(): KhasmMethodTransformer {
        return working
    }
}
