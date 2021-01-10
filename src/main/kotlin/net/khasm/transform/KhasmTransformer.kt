package net.khasm.transform

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class KhasmTransformer {
    // Predicates
    private lateinit var transformClassPredicate: (ClassNode) -> Boolean
    private var transformMethodPredicate: (MethodNode) -> Boolean = { false }

    // Transforming methods
    private lateinit var targetPredicate: MethodNode.() -> List<Int>
    private lateinit var action: (MethodNode, AbstractInsnNode) -> Unit

    // Predicate setters
    fun setClassPredicate(predicate: (ClassNode) -> Boolean) {
        transformClassPredicate = predicate
    }

    fun setMethodPredicate(predicate: (MethodNode) -> Boolean) {
        transformMethodPredicate = predicate
    }

    fun setTargetPredicate(predicate: MethodNode.() -> List<Int>) {
        targetPredicate = predicate
    }

    fun setAction(action: (MethodNode, AbstractInsnNode) -> Unit) {
        this.action = action
    }

    // Predicate testing
    private fun shouldTransformClass(cls: ClassNode): Boolean {
        return transformClassPredicate(cls)
    }

    private fun shouldTransformMethod(method: MethodNode): Boolean {
        return transformMethodPredicate(method)
    }

    // Actual transformation
    fun tryTransformClass(classNode: ClassNode) {
        if (shouldTransformClass(classNode)) {
            println(classNode.name + " is being transformed")
            for (method in classNode.methods) {
                if (shouldTransformMethod(method)) {
                    println("Transforming method " + method.name + method.desc)
                    val cursors = targetPredicate(method)
                    cursors.forEach {
                        var corrected = it
                        if (it > method.instructions.size() - 1) {
                            corrected = method.instructions.size() - 1
                        }
                        method.visitCode()
                        action(method, method.instructions[corrected])
                        method.visitEnd()
                    }
                }
            }
        }
    }
}
