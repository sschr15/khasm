package net.khasm.transform.`class`

import org.objectweb.asm.tree.ClassNode

object KhasmClassTransformerDispatcher {
    private val transformers : MutableList<KhasmClassTransformer> = mutableListOf()
    private val transformersToRemove: MutableList<KhasmClassTransformer> = mutableListOf()

    fun registerClassTransformer(methodTransformer: KhasmClassTransformer) {
        transformers.add(methodTransformer)
    }

    fun registerClassTransformer(transformer: KhasmClassTransformerBuilder.() -> Unit) {
        registerClassTransformer(KhasmClassTransformerBuilder(transformer).build())
    }

    fun tryTransform(node: ClassNode) {
        transformers.forEach {
            if (it.tryTransformClass(node)) {
                transformersToRemove.add(it)
            }
        }
        transformers.removeAll(transformersToRemove)
        transformersToRemove.clear()
    }
}