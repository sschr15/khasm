package net.khasm.transform

import codes.som.anthony.koffee.MethodAssembly
import net.khasm.transform.target.AbstractKhasmTarget
import net.khasm.util.mapClass
import net.khasm.util.mapMethod
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

@Suppress("unused", "MemberVisibilityCanBePrivate")
class KhasmTransformerBuilder(method: KhasmTransformerBuilder.() -> Unit) {
    private val working = KhasmTransformer()

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

    fun methodTarget(owner: String, intermediary: String, descriptor: String) {
        methodTarget {
            name == mapMethod(owner, intermediary, descriptor) && desc == descriptor
        }
    }

    fun methodTarget(lambda: MethodNode.() -> Boolean) {
        working.setMethodPredicate(lambda)
    }

    /**
     * Call this method at some point to
     * set the transformer to overwriting the
     * method instead of injecting within it.
     */
    fun overwrite() {
        working.overrideMethod = true
    }

    fun target(lambda: () -> AbstractKhasmTarget) {
        working.setTargetPredicate(lambda())
    }

    fun action(action: MethodAssembly.(AbstractInsnNode) -> Unit) {
        working.setAction(action)
    }

    fun build(): KhasmTransformer {
        return working
    }
}
