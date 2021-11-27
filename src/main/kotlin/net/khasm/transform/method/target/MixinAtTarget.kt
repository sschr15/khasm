package net.khasm.transform.method.target

import net.khasm.annotation.DangerousKhasmUsage
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.MethodNode
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.IInjectionPointContext
import org.spongepowered.asm.mixin.injection.InjectionPoint

/*
 * This gets to be its own file because working with Mixin's At annotation is a pain.
 * also it's still very beta.
 */

/**
 * Inject based on an [@At][At] Mixin annotation.
 *
 * This can be useful if you're used to Mixin's injection points.
 */
@DangerousKhasmUsage("""
    Kotlin currently requires *all* fields to be specified in the annotation,
    including default values. This means that all fields, including the ones
    that you don't use, must be specified.

    If (when) Kotlin gets a chance to support default values, this
    annotation will be removed.
""")
class AtTarget(private val at: At) : AbstractKhasmTarget() {
    override fun getPossibleCursors(range: IntRange, node: MethodNode): CursorsFixed {
        val injectionPoint = InjectionPoint.parse(
            MixinAtTargetInjectionPointContext(node),
            at
        )
        val nodes = mutableListOf<AbstractInsnNode>()
        val found = injectionPoint.find(node.desc, node.instructions, nodes)
        return if (found) {
            CursorsFixed(nodes.map { node.instructions.indexOf(it) })
        } else {
            CursorsFixed(emptyList())
        }
    }
}

class MixinAtTargetInjectionPointContext(
    private val method: MethodNode
) : IInjectionPointContext {
    override fun getParent() = null

    override fun getMixin() = null

    override fun getMethod() = method

    override fun getAnnotation() = null

    override fun getSelectorAnnotation() = null

    override fun getSelectorCoordinate(leaf: Boolean) = null

    override fun remap(reference: String?) = null

    override fun addMessage(format: String?, vararg args: Any?) {
    }

    override fun getAnnotationNode(): AnnotationNode = AnnotationNode(
        "Lorg/spongepowered/asm/mixin/injection/At;"
    )
}
