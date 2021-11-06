@file:JvmName("KhasmInstrumentationApi")

package net.khasm.instrumentation

import codes.som.anthony.koffee.ClassAssembly
import codes.som.anthony.koffee.koffee
import codes.som.anthony.koffee.types.TypeLike
import codes.som.anthony.koffee.types.coerceType
import net.khasm.transform.KhasmClassWriter
import net.khasm.util.Functional
import net.khasm.util.currentClassLoader
import net.khasm.util.lateInitVal
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import sschr15.tools.betterpaths.toPath
import java.io.InputStream
import java.lang.instrument.ClassDefinition
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain
import kotlin.io.path.writeBytes

var instrumentation: Instrumentation by lateInitVal()

/**
 * Since Java 9, [ClassFileTransformer] is not a functional interface: it has 2 `default` methods.
 * The purpose of this class is solving that problem by extending [ClassFileTransformer] and making the pre-Java 9 method abstract.
 */
@Functional interface CompatibilityClassFileTransformer : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader,
        className: String,
        classBeingRedefined: Class<*>,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ): ByteArray
}

fun retransformRaw(`class`: TypeLike, classLoader: ClassLoader? = null, transformer: (name: String, bytes: ByteArray) -> ByteArray) {
    val fileTransformer = object : CompatibilityClassFileTransformer {
        override fun transform(
            loader: ClassLoader,
            className: String,
            classBeingRedefined: Class<*>,
            protectionDomain: ProtectionDomain,
            classfileBuffer: ByteArray
        ): ByteArray {
            return if (coerceType(`class`) == coerceType(classBeingRedefined)) transformer(className, classfileBuffer) else classfileBuffer
        }
    }

    transform(`class`.asClass(classLoader), fileTransformer)
}

fun retransformClassNode(`class`: TypeLike, classLoader: ClassLoader? = null, transformer: ClassNode.() -> Unit) = retransformRaw(`class`, classLoader) { _, bytes ->
    val node = ClassNode(Opcodes.ASM9)
    ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES)
    node.version = 60
    node.transformer()
    KhasmClassWriter(ClassWriter.COMPUTE_FRAMES, currentClassLoader).also { node.accept(it) }.toByteArray()
}

fun retransformKoffee(`class`: TypeLike, classLoader: ClassLoader? = null, transformer: ClassAssembly.() -> Unit) =
    retransformClassNode(`class`, classLoader) { koffee(transformer) }

fun redefineRaw(`class`: TypeLike, export: Boolean = false, classLoader: ClassLoader? = null, redefiner: (ByteArray) -> ByteArray) {
    instrumentation.redefineClasses(ClassDefinition(`class`.asClass(classLoader), redefiner(
        currentClassLoader.getResourceAsStream("${coerceType(`class`).internalName}.class")!!.use(InputStream::readAllBytes)
    ).also { if (export)
        (coerceType(`class`).internalName.substringAfterLast('/') + ".class").toPath().writeBytes(it)
    }))
}

fun redefineClassNode(`class`: TypeLike, export: Boolean = false, classLoader: ClassLoader? = null, redefiner: ClassNode.() -> Unit) = redefineRaw(`class`, export, classLoader) {
        val node = ClassNode(Opcodes.ASM9)
        ClassReader(it).accept(node, 0)
        node.redefiner()
        KhasmClassWriter(ClassWriter.COMPUTE_FRAMES, currentClassLoader).also { cw -> node.accept(cw) }.toByteArray()
    }

fun redefineKoffee(`class`: TypeLike, export: Boolean = false, classLoader: ClassLoader? = null, redefiner: ClassAssembly.() -> Unit) =
    redefineClassNode(`class`, export, classLoader) { koffee(redefiner) }

private fun transform(`class`: Class<*>, transformer: ClassFileTransformer) {
    instrumentation.addTransformer(transformer, true)
    instrumentation.retransformClasses(`class`)
    instrumentation.removeTransformer(transformer)
}

private fun TypeLike.asClass(classLoader: ClassLoader? = null) =
    if (classLoader == null) Class.forName(coerceType(this).className) else Class.forName(coerceType(this).className, true, classLoader)
