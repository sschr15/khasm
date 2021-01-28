package net.khasm.init

import net.devtech.grossfabrichacks.entrypoints.PrePreLaunch
import net.devtech.grossfabrichacks.transformer.TransformerApi
import net.fabricmc.loader.api.FabricLoader
import net.khasm.KhasmLoad
import net.khasm.test.AdvancedKhasmTest
import net.khasm.test.KhasmTest
import net.khasm.transform.`class`.KhasmClassTransformerDispatcher
import net.khasm.transform.method.KhasmMethodTransformerDispatcher
import net.khasm.util.logger
import sschr15.tools.betterpaths.*
import java.nio.file.Files
import java.security.MessageDigest

@Suppress("unused")
class KhasmPrePreLaunch : PrePreLaunch {
    private val debugFolder = FabricLoader.getInstance().gameDir.resolve("khasm")
    private val hashes = mutableMapOf<String, ByteArray>()

    override fun onPrePreLaunch() {
        logger.info("Khasm is running from prePreLaunch!")

        KhasmTest.registerTest()

        val exportAll = System.getProperty("khasm.exportAll", "false") == "true"
        if (exportAll && debugFolder.doesNotExist()) debugFolder.createDirectories()

        if (debugFolder.exists()) AdvancedKhasmTest.registerTest()

        FabricLoader.getInstance()
            .getEntrypoints("khasm", KhasmLoad::class.java)
            .forEach(KhasmLoad::loadTransformers)

        TransformerApi.registerPreMixinAsmClassTransformer { name, node ->
            // No recursion, maybe security as well? idk
            if (!name.startsWith("net/khasm")) {
                KhasmClassTransformerDispatcher.tryTransform(node)
                KhasmMethodTransformerDispatcher.tryTransform(node)
                node.visitEnd()
            }
        }

        if (debugFolder.exists() && debugFolder.isDirectory()) {
            logger.warn("Khasm folder exists! Exporting ${if (exportAll) "all" else "modified"} classes!")
            TransformerApi.registerPreMixinRawClassTransformer { name, bytes ->
                // this if condition is just to make sure we don't check hashes if we don't need to
                if (!exportAll) hashes[name] = MessageDigest.getInstance("SHA-1").digest(bytes)
                bytes
            }

            TransformerApi.registerPostMixinRawClassTransformer { name, bytes ->
                val output = debugFolder.resolve("$name.class")
                if (exportAll || !MessageDigest.getInstance("SHA-1").digest(bytes).contentEquals(hashes[name])) {
                    Files.createDirectories(output.parent)
                    Files.write(output, bytes)
                }
                bytes
            }
        }
    }
}
