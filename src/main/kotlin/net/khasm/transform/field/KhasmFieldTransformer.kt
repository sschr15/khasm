@file:Suppress("unused")

package net.khasm.transform.field

import codes.som.anthony.koffee.types.TypeLike
import codes.som.anthony.koffee.types.coerceType
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.FieldNode
import kotlin.reflect.KProperty

class KhasmFieldTransformer(private val node: FieldNode) {
    // Access
    var public by AccessModifier(ACC_PUBLIC)
    var private by AccessModifier(ACC_PRIVATE)
    var protected by AccessModifier(ACC_PROTECTED)
    var static by AccessModifier(ACC_STATIC)
    var final by AccessModifier(ACC_FINAL)
    var volatile by AccessModifier(ACC_VOLATILE)
    var transient by AccessModifier(ACC_TRANSIENT)
    var synthetic by AccessModifier(ACC_SYNTHETIC)
    var mandated by AccessModifier(ACC_MANDATED)
    var deprecated by AccessModifier(ACC_DEPRECATED)

    // Default value, if any
    var value: Any?
        get() = node.value
        set(value) { node.value = value }

    fun addAnnotation(type: TypeLike, visible: Boolean = true, builder: AnnotationVisitor.() -> Unit = {}) {
        node.visitAnnotation(coerceType(type).internalName, visible).builder()
    }

    var type: Type
        get() = coerceType(node.desc)
        set(value) {
            node.desc = value.descriptor
        }

    private inner class AccessModifier(private val access: Int) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) =
            node.access and access != 0

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            node.access = if (value) {
                // set flag
                node.access or access
            } else {
                // clear flag (flip access bits then AND it)
                node.access and access.inv()
            }
        }
    }
}

