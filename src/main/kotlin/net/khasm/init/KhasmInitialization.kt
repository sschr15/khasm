@file:Suppress("unused")

package net.khasm.init

import net.fabricmc.api.ModInitializer
import net.khasm.transform.method.KhasmMethodTransformerDispatcher
import net.khasm.util.logger

open class KhasmInit : ModInitializer {
    override fun onInitialize() {
        KhasmMethodTransformerDispatcher.appliedFunctions.forEach { (className, transformers) ->
            run {
                // Doing it this way that cleans up the lists after we use them
                while (transformers.isNotEmpty()) {
                    val transformer = transformers.removeFirst()
                    val clazz = Class.forName(className.replace('/', '.'))
                    logger.info("Setting up ${transformer.internalName} in $className")
                    clazz.getDeclaredField(transformer.internalName)
                        .also { it.isAccessible = true }
                        .set(null, transformer.action)
                }
            }
        }
    }
}
