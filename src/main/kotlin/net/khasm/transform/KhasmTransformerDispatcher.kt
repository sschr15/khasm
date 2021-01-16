package net.khasm.transform

import org.objectweb.asm.tree.ClassNode

object KhasmTransformerDispatcher {
    private val transformers : MutableList<KhasmTransformer> = mutableListOf()
    private val transformersToRemove: MutableList<KhasmTransformer> = mutableListOf()

    fun registerTransformer(transformer: KhasmTransformer) {
        transformers.add(transformer)
    }

    fun registerTransformer(transformer: KhasmTransformerBuilder.() -> Unit) {
        registerTransformer(KhasmTransformerBuilder(transformer).build())
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
