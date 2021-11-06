package net.khasm.transform.`class`

import net.fabricmc.loader.api.FabricLoader
import net.khasm.KhasmLoad
import net.khasm.test.AdvancedKhasmTest
import net.khasm.test.KhasmTest
import net.khasm.util.debugFolder
import org.objectweb.asm.tree.ClassNode
import sschr15.tools.betterpaths.createDirectories
import sschr15.tools.betterpaths.doesNotExist
import sschr15.tools.betterpaths.exists

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

    init {
        KhasmTest.registerTest()

        val exportAll = System.getProperty("khasm.exportAll", "false") == "true"
        if (exportAll && debugFolder.doesNotExist()) debugFolder.createDirectories()

        if (debugFolder.exists()) AdvancedKhasmTest.registerTest()

        FabricLoader.getInstance()
            .getEntrypoints("khasm", KhasmLoad::class.java)
            .forEach(KhasmLoad::loadTransformers)
    }
}