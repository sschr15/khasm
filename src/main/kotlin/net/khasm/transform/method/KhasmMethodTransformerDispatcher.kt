package net.khasm.transform.method

import net.khasm.transform.method.action.SmartMethodTransformer
import org.objectweb.asm.tree.ClassNode

object KhasmMethodTransformerDispatcher {
    private val transformers : MutableList<KhasmMethodTransformer> = mutableListOf()
    private val transformersToRemove: MutableList<KhasmMethodTransformer> = mutableListOf()

    fun registerMethodTransformer(methodTransformer: KhasmMethodTransformer) {
        transformers.add(methodTransformer)
    }

    fun registerMethodTransformer(transformer: KhasmMethodTransformerBuilder.() -> Unit) {
        registerMethodTransformer(KhasmMethodTransformerBuilder(transformer).build())
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

    internal val appliedFunctions = mutableMapOf<String, MutableList<SmartMethodTransformer>>()
}
