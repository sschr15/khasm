package net.khasm.transform

import org.objectweb.asm.tree.ClassNode

object KhasmTransformerDispatcher {
    private val transformers : MutableList<KhasmTransformer> = mutableListOf()

    fun registerTransformer(transformer: KhasmTransformer) {
        transformers.add(transformer)
    }

    fun registerTransformer(transformer: KhasmTransformerBuilder.() -> Unit) {
        registerTransformer(KhasmTransformerBuilder(transformer).build())
    }

    fun tryTransform(node: ClassNode) {
        transformers.forEach {
            it.tryTransformClass(node)
        }
    }
}
