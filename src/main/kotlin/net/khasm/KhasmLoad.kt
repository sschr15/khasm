package net.khasm

/**
 * A base class for Khasm transformers.
 * This is used instead of Fabric's entrypoint system,
 * because Fabric's entrypoint system doesn't work as well
 * especially with concerning things like what we're doing
 */
abstract class KhasmInitializer {
    /**
     * The main entrypoint for Khasm transformers.
     * This is called by Khasm's initialization routine.
     */
    abstract fun init()
}

@Deprecated(
    "KhasmLoad is deprecated due to its difficulty with fabric loader. Use a subclass of KhasmInitializer instead",
    ReplaceWith(
        "KhasmLoadReplacement()",
        "net.khasm.KhasmLoadReplacement"
    ),
)
interface KhasmLoad {
    fun loadTransformers()
}

/**
 * A replacement for [KhasmLoad].
 * This is simply a wrapper for [KhasmInitializer]
 * intended to make it easier to switch to using KhasmInitializer
 * instead of KhasmLoad.
 */
@Suppress("DEPRECATION")
abstract class KhasmLoadReplacement : KhasmInitializer() {
    abstract fun loadTransformers()
    override fun init() = loadTransformers()
}
