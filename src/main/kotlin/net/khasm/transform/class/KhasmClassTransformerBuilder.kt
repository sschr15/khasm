package net.khasm.transform.`class`

import codes.som.anthony.koffee.ClassAssembly
import codes.som.anthony.koffee.types.TypeLike
import net.khasm.annotation.DangerousKhasmUsage
import net.khasm.util.mapClass
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.tree.ClassNode

/**
 * A class transformer. Useful for adding fields & methods
 * and changing inheritance and annotations.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class KhasmClassTransformerBuilder(method: KhasmClassTransformerBuilder.() -> Unit) {
    private val working = KhasmClassTransformer()

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
        working.transformClassPredicate = lambda
    }

    fun action(action: ClassAssembly.() -> Unit) {
        working.action = action
    }

    /**
     * Add an annotation to this class.
     * If the annotation has fields, [action]
     * can be used to set them.
     */
    fun ClassAssembly.annotation(type: TypeLike, visible: Boolean = true, action: AnnotationVisitor.() -> Unit = {}) {
        val name = coerceType(type).descriptor
        node.visitAnnotation(name, visible).action()
    }

    /**
     * Set the superclass of this class.
     *
     * This is marked as dangerous as this will overwrite
     * a parent if it's set.
     */
    @DangerousKhasmUsage
    fun ClassAssembly.extend(type: TypeLike) {
        node.superName = coerceType(type).internalName
    }

    /**
     * Cause this class to implement another class.
     *
     * This is not marked as dangerous as a given class
     * can have many implemented interfaces.
     */
    fun ClassAssembly.implement(type: TypeLike) {
        node.interfaces.add(coerceType(type).internalName)
    }

    fun build(): KhasmClassTransformer = working
}