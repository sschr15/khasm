package net.khasm.transform

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class KhasmTransformerBuilder(method: KhasmTransformerBuilder.() -> Unit) {
    private val working = KhasmTransformer()

    init {
        method()
    }

    fun classTarget(lambda: ClassNode.() -> Boolean) {
        working.setClassPredicate(lambda)
    }

    fun methodTarget(lambda: MethodNode.() -> Boolean) {
        working.setMethodPredicate(lambda)
    }

    fun targets(lambda: MethodNode.() -> List<Int>) {
        working.setTargetPredicate(lambda)
    }

    fun action(action: (MethodNode, AbstractInsnNode) -> Unit) {
        working.setAction(action)
    }

    fun build(): KhasmTransformer {
        return working
    }
}
