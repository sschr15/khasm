package net.khasm.transform.method

import net.khasm.annotation.DangerousKhasmUsage
import net.khasm.transform.method.action.ActionBuilder
import net.khasm.transform.method.target.AbstractKhasmTarget
import net.khasm.util.mapClass
import net.khasm.util.mapMethod
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

@Suppress("unused", "MemberVisibilityCanBePrivate")
class KhasmMethodTransformerBuilder(method: KhasmMethodTransformerBuilder.() -> Unit) {
    private val working = KhasmMethodTransformer()

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
     * This parameter is marked as dangerous as it may match more methods than expected.
     */
    @OptIn(DangerousKhasmUsage::class)
    fun methodTarget(owner: String, intermediary: String, descriptor: String, @DangerousKhasmUsage skipDescriptorChecks: Boolean = false) {
        methodTarget {
            name == mapMethod(owner, intermediary, descriptor) && (skipDescriptorChecks || desc == descriptor)
        }
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
