package net.khasm.transform.`class`

import codes.som.anthony.koffee.ClassAssembly
import codes.som.anthony.koffee.koffee
import net.khasm.util.logger
import org.objectweb.asm.tree.ClassNode

class KhasmClassTransformer {
    internal var transformClassPredicate: (ClassNode) -> Boolean = { false }
    internal var action: ClassAssembly.() -> Unit = {}

    // Marks transformer as targeting a single class and thus can be thrown away once transformed (probably a micro-optimisation idc)
    internal var oneTimeUse = false

    fun tryTransformClass(classNode: ClassNode): Boolean {
        if (transformClassPredicate(classNode)) {
            logger.debug("Transforming class ${classNode.name}")
            classNode.koffee(action)
            return true
        }
        return false
    }
}