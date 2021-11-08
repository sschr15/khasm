package net.khasm.transform.`class`

import org.objectweb.asm.tree.ClassNode

object KhasmClassTransformerDispatcher {
    private val transformers : MutableList<KhasmClassTransformer> = mutableListOf()
    private val transformersToRemove: MutableList<KhasmClassTransformer> = mutableListOf()

    fun registerClassTransformer(transformer: KhasmClassTransformer) {
        transformers.add(transformer)
    }

    fun registerClassTransformer(modid: String, transformer: KhasmClassTransformerBuilder.() -> Unit) {
        registerClassTransformer(KhasmClassTransformerBuilder(modid, transformer).build())
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