@file:Suppress("unused", "UNUSED_EXPRESSION", "LocalVariableName")

package net.khasm.init

import net.fabricmc.loader.api.FabricLoader
import net.khasm.KhasmLoad
import net.khasm.exception.AlreadyTransformingException
import net.khasm.instrumentation.retransformClassNode
import net.khasm.transform.KhasmClassWriter
import net.khasm.transform.`class`.KhasmClassTransformerDispatcher
import net.khasm.transform.method.KhasmMethodTransformerDispatcher
import net.khasm.util.currentClassLoader
import net.khasm.util.debugFolder
import net.khasm.util.logger
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import sschr15.tools.betterpaths.div
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import org.objectweb.asm.ClassWriter as AsmClassWriter

val dummyVal = if (object {}::class.java.classLoader.name == "app") error("AppClassLoader loaded NewInitializationKt!") else null

interface DelegateDuck {
    @Throws(ClassNotFoundException::class)
    fun tryLoadClass(name: String, allowFromParent: Boolean): Class<*>?
}

object Dummy

fun onInstrumentationLoaded() {
    logger.info("Khasm is running from new launch!")

    val knotPkg = "net/fabricmc/loader/impl/launch/knot"

    val transformDataClassNode = ClassNode()
    ClassReader(Dummy::class.java.getResourceAsStream("/$knotPkg/TransformData.class")).accept(transformDataClassNode, 0)

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

    FabricLoader.getInstance()
        .getEntrypoints("khasm:code-setup", KhasmLoad::class.java)
        .forEach(KhasmLoad::loadTransformers)
}

private val currentlyTransforming: MutableList<String> = mutableListOf()

fun khasmTransform(bytes: ByteArray?, s: String?): ByteArray? {
    if (bytes == null || s == null) return null
    val name = s.replace('.', '/')
    return (if (!name.startsWith("net/khasm")) {
        if (currentlyTransforming.contains(name)) {
            throw AlreadyTransformingException(name)
        } else {
            currentlyTransforming.add(name)
        }

        val node = ClassNode()
        ClassReader(bytes).accept(node, 0)
        // No recursion, maybe security as well? idk
        KhasmClassTransformerDispatcher.tryTransform(node)
        KhasmMethodTransformerDispatcher.tryTransform(node)
        currentlyTransforming.remove(name)
        KhasmClassWriter(AsmClassWriter.COMPUTE_FRAMES, currentClassLoader).also { node.accept(it) }.toByteArray()
    } else bytes).also {
        if (debugFolder.exists() && !it.contentEquals(bytes.newFrames())) {
            val exportPath = debugFolder / "$name.class"
            exportPath.parent.createDirectories()
            exportPath.writeBytes(it)
        }
    }
}

fun ByteArray.newFrames(): ByteArray =
    KhasmClassWriter(AsmClassWriter.COMPUTE_FRAMES, currentClassLoader).also { ClassReader(this).accept(it, 0) }.toByteArray()

private class LocalFunctionVar<A>(val action: () -> A) : ReadOnlyProperty<Any?, A> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = action()
}
