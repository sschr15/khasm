package net.khasm.transform.`class`

import codes.som.anthony.koffee.ClassAssembly
import codes.som.anthony.koffee.types.TypeLike
import net.khasm.annotation.DangerousKhasmUsage
import net.khasm.transform.field.KhasmFieldTransformer
import net.khasm.transform.method.KhasmMethodTransformerBuilder
import net.khasm.transform.method.KhasmMethodTransformerDispatcher
import net.khasm.util.mapClass
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.tree.ClassNode

/**
 * A class transformer. Useful for adding fields & methods
 * and changing inheritance and annotations.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class KhasmClassTransformerBuilder(private val modid: String, method: KhasmClassTransformerBuilder.() -> Unit) {
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
     */
    @DangerousKhasmUsage("""
        This will change the superclass of the class.
        This is not recommended unless you know what you're doing.
    """)
    fun ClassAssembly.extend(type: TypeLike) {
        node.superName = coerceType(type).internalName
    }

    /**
     * Cause this class to implement another class.
     */
    fun ClassAssembly.implement(type: TypeLike) {
        node.interfaces.add(coerceType(type).internalName)
    }

    fun ClassAssembly.transformField(fieldName: String, transformer: KhasmFieldTransformer.() -> Unit) {
        node.fields.firstOrNull { it.name == fieldName }?.let {
            KhasmFieldTransformer(it).transformer()
        }
    }

    fun ClassAssembly.transformMethod(transformer: KhasmMethodTransformerBuilder.() -> Unit) {
        KhasmMethodTransformerDispatcher.registerMethodTransformer(modid) {
            classTarget(working.transformClassPredicate)
            transformer()
        }
    }

    fun build(): KhasmClassTransformer = working
}