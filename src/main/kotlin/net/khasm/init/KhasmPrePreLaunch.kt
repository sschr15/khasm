package net.khasm.init

import net.devtech.grossfabrichacks.entrypoints.PrePreLaunch
import net.devtech.grossfabrichacks.transformer.TransformerApi
import net.fabricmc.loader.api.FabricLoader
import net.khasm.test.KhasmTest
import net.khasm.transform.KhasmTransformerDispatcher
import net.khasm.util.logger
import java.nio.file.Files

@Suppress("unused")
object KhasmPrePreLaunch : PrePreLaunch {
    private val debugFolder = FabricLoader.getInstance().gameDir.resolve("khasm")

    override fun onPrePreLaunch() {
        logger.info("Khasm is running from prePreLaunch!")

        if (Files.exists(debugFolder)) KhasmTest.registerTest()

        TransformerApi.registerPreMixinAsmClassTransformer { name, node ->
            // No recursion, maybe security as well? idk
            if (!name.startsWith("net/khasm")) {
                KhasmTransformerDispatcher.tryTransform(node)
                node.visitEnd()
            }
        }

        if (Files.exists(debugFolder) && Files.isDirectory(debugFolder)) {
            logger.warn("Khasm folder exists, every class will be exported!")

            TransformerApi.registerPostMixinRawClassTransformer { name, bytes ->
                    val output = debugFolder.resolve("$name.class")
                    Files.createDirectories(output.parent)
                    Files.write(output, bytes)
                bytes
            }
        }
    }
}
