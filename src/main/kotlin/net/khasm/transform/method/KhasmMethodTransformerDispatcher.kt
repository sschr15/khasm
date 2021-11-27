package net.khasm.transform.method

import net.khasm.transform.method.action.SmartMethodTransformer
import org.objectweb.asm.tree.ClassNode

object KhasmMethodTransformerDispatcher {
    @Volatile
    private var transformers : MutableList<KhasmMethodTransformer> = mutableListOf()
    @Volatile
    private var transformersToRemove: MutableList<KhasmMethodTransformer> = mutableListOf()

    private fun registerMethodTransformer(methodTransformer: KhasmMethodTransformer) {
        transformers.add(methodTransformer)
    }

    fun registerMethodTransformer(modid: String, transformer: KhasmMethodTransformerBuilder.() -> Unit) {
        registerMethodTransformer(KhasmMethodTransformerBuilder(transformer, modid).build())
    }

    fun tryTransform(node: ClassNode): Boolean {
        var transformed = false
        transformers.forEach {
            if (it.tryTransformClass(node)) {
                transformersToRemove.add(it)
                transformed = true
            }
        }
        transformers.removeAll(transformersToRemove)
        transformersToRemove.clear()

        return transformed
    }

    val appliedFunctions = mutableMapOf<String, MutableList<SmartMethodTransformer>>()
}
