@file:Suppress("unused", "UNUSED_EXPRESSION", "LocalVariableName", "UNCHECKED_CAST", "DEPRECATION")

package net.khasm.init

import codes.som.anthony.koffee.modifiers.abstract
import codes.som.anthony.koffee.modifiers.enum
import codes.som.anthony.koffee.modifiers.private
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.LanguageAdapter
import net.khasm.KhasmInitializer
import net.khasm.KhasmLoad
import net.khasm.exception.AlreadyTransformingException
import net.khasm.instrumentation.retransformClassNode
import net.khasm.transform.KhasmClassWriter
import net.khasm.transform.`class`.KhasmClassTransformerDispatcher
import net.khasm.transform.method.KhasmMethodTransformerDispatcher
import net.khasm.util.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import sschr15.tools.betterpaths.div
import java.lang.reflect.Field
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import net.fabricmc.loader.ModContainer as LoaderPackageModContainer
import org.objectweb.asm.ClassWriter as AsmClassWriter

val dummyVal = if (object {}::class.java.classLoader.name == "app") error("AppClassLoader loaded NewInitializationKt!") else null

object Dummy

fun onInstrumentationLoaded() {
    logger.info("Khasm is running from new launch!")

    val knotPkg = "net/fabricmc/loader/impl/launch/knot"

    val transformDataClassNode = ClassNode()
    Dummy::class.java.getResourceAsStream("/$knotPkg/TransformData.class").use { ClassReader(it).accept(transformDataClassNode, 0) }

    retransformClassNode("$knotPkg/KnotClassDelegate") {
        logger.info("Transforming KnotClassDelegate")

        sourceFile = transformDataClassNode.sourceFile // hack to make debugging easier

        // replace methods because knot is :concerned_tater: and :concern: and :ihaveconcerns:
        // tryLoadClass adds khasm smart inject methods to the modified class
        with(transformDataClassNode.methods.find { it.name == "tryLoadClass" && it.desc == "(Ljava/lang/String;Z)Ljava/lang/Class;" }) {
            this ?: error("Could not find method tryLoadClass(Ljava/lang/String;Z)Ljava/lang/Class;")
            methods[methods.indexOfFirst { it.name == name && it.desc == desc }] = this
            instructions.forEach { when (it) {
                    is MethodInsnNode -> if (it.owner == "net/fabricmc/loader/impl/launch/knot/TransformData") {
                        it.owner = "net/fabricmc/loader/impl/launch/knot/KnotClassDelegate"
                    }
                    is FieldInsnNode -> if (it.owner == "net/fabricmc/loader/impl/launch/knot/TransformData") {
                        it.owner = "net/fabricmc/loader/impl/launch/knot/KnotClassDelegate"
                    }
//                    is LineNumberNode -> instructions.remove(it)
                } }
        }
        // getPreMixinClassByteArray calls khasm transformers
        with(transformDataClassNode.methods.find { it.name == "getPreMixinClassByteArray" && it.desc == "(Ljava/lang/String;Z)[B" }) {
            this ?: error("Could not find method getPreMixinClassByteArray(Ljava/lang/String;Z)[B")
            methods[methods.indexOfFirst { it.name == name && it.desc == desc }] = this
            instructions.forEach { when (it) {
                    is MethodInsnNode -> if (it.owner == "net/fabricmc/loader/impl/launch/knot/TransformData") {
                        it.owner = "net/fabricmc/loader/impl/launch/knot/KnotClassDelegate"
                    }
                    is FieldInsnNode -> if (it.owner == "net/fabricmc/loader/impl/launch/knot/TransformData") {
                        it.owner = "net/fabricmc/loader/impl/launch/knot/KnotClassDelegate"
                    }
//                    is LineNumberNode -> instructions.remove(it)
                } }
        }
        /* code below is for live debugging
        instructions.map { "${it::class.simpleName}: ${when (it) {
            is LabelNode -> it.label
            is LineNumberNode -> it.line
            is FrameNode -> "locals: ${it.local}, stack: ${it.stack}"
            is AbstractInsnNode -> Opcodes::class.java.fields.first { i -> i.get(null) as Int == it.opcode }.name + " " + when (it) {
                is VarInsnNode -> it.`var`
                is LdcInsnNode -> it.cst
                is MethodInsnNode -> "${it.owner} ${it.name}${it.desc}"
                is JumpInsnNode -> it.label.label
                else -> ""
            }.toString()
            else -> "other?"
        }}" }
         */

        // also export the class as a .class file
        val classFile = debugFolder / "KnotClassDelegate.class"
        KhasmClassWriter(AsmClassWriter.COMPUTE_FRAMES, currentClassLoader).also {
            this.accept(it)
            classFile.writeBytes(it.toByteArray())
        }
    }

    logger.info("Finished redefining classes")

    val initializers = initializerClassNames
        .map { it.replace('/', '.') }
        .map { Class.forName(it, true, currentClassLoader) as Class<out KhasmInitializer> }
        .filter { it.modifiers.asModifiers().containsNone(abstract + private + enum) }
        .associate { it.newInstance() to it.getDeclaredMethod("init") }

    initializers.forEach { (initializer, init) ->
        try {
            init(initializer)
        } catch (e: Throwable) {
            logger.error("Error running initializer ${initializer::class.simpleName}", e)
        }
    }

    // some mods may not be using the new initializers, so we need to initialize them separately
    loadLegacyInitializers()

    logger.info("Finished running initializers")
}

// this might not work but too bad. switch to using the new initializers
internal fun loadLegacyInitializers() {
    val mods = FabricLoader.getInstance().allMods
    val initializers = mutableListOf<KhasmLoad>()
    // we have to use net.fabricmc.loader.ModContainer instead of net.fabricmc.loader.api.ModContainer in order to get entrypoints
    val entrypoints = mods
        .map { it as LoaderPackageModContainer }
        .associateWith { it.info.getEntrypoints("khasm:code-setup") }
    for ((mod, entrypointList) in entrypoints) {
        for (entrypoint in entrypointList) {
            val className = entrypoint.value
            val languageAdapter: String? = entrypoint.adapter
            val languageAdapterInstance = if (!languageAdapter.isNullOrBlank()) {
                val languageAdapterClass = Class.forName(languageAdapter) as Class<out LanguageAdapter>
                languageAdapterClass.getDeclaredConstructor().newInstance()
            } else LanguageAdapter.getDefault()
            val obj = languageAdapterInstance.create(mod, className, KhasmLoad::class.java)
            initializers.add(obj)
        }
    }

    initializers.forEach {
        logger.debug("Initializing ${it::class.simpleName}")
        it.loadTransformers()
    }
}

private val currentlyTransforming: MutableList<String> = mutableListOf()

fun khasmTransform(bytes: ByteArray?, s: String?): ByteArray? {
    if (bytes == null || s == null) return null
    val name = s.replace('.', '/')
    return if (!name.startsWith("net/khasm")) {
        if (currentlyTransforming.contains(name)) {
            throw AlreadyTransformingException(name)
        } else {
            currentlyTransforming.add(name)
        }

        val node = ClassNode()
        ClassReader(bytes).accept(node, 0)
        // check if the class gets transformed by seeing if any transformers run
        var transformed = KhasmClassTransformerDispatcher.tryTransform(node)
        transformed = KhasmMethodTransformerDispatcher.tryTransform(node) || transformed
        currentlyTransforming.remove(name)

        // if no changes were made, return the original bytes (hopefully circumnavigates ClassCircularityError)
        if (!transformed) bytes
        else KhasmClassWriter(AsmClassWriter.COMPUTE_FRAMES, currentClassLoader).also { node.accept(it) }.toByteArray().also {
            if (debugFolder.exists()) {
                // export if modified by khasm
                val exportPath = debugFolder / "$name.class"
                exportPath.parent.createDirectories()
                exportPath.writeBytes(it)
            }
        }
    } else bytes
}

// delegating here to keep things in KnotClassLoader rather than AppClassLoader
fun setupSmartInjects(`class`: Class<*>, name: String) {
    val slashedName = name.replace('.', '/')
    val map = KhasmMethodTransformerDispatcher.appliedFunctions

    map[slashedName]?.forEach { transformer ->
        logger.debug("Setting up {} in {}", transformer.internalName, name)
        val field: Field = `class`.getDeclaredField(transformer.internalName)
        field.isAccessible = true
        field[null] = transformer.action
    }
}

fun ByteArray.newFrames(): ByteArray =
    KhasmClassWriter(AsmClassWriter.COMPUTE_FRAMES, currentClassLoader).also { ClassReader(this).accept(it, 0) }.toByteArray()

private class LocalFunctionVar<A>(val action: () -> A) : ReadOnlyProperty<Any?, A> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = action()
}
