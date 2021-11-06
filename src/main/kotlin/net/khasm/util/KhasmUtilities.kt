package net.khasm.util

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import net.khasm.transform.ClassLoadingTree
import org.apache.commons.codec.digest.DigestUtils.sha512
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
import sschr15.tools.betterpaths.div
import user11681.reflect.Classes
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

// make things look nicer
internal typealias Functional = FunctionalInterface

internal val logger: Logger = LogManager.getLogger("khasm")

internal val debugFolder = FabricLoader.getInstance().gameDir.resolve("khasm")

internal val IntRange.Companion.ANY: IntRange
    get() = Int.MIN_VALUE..Int.MAX_VALUE

/**
 * Assumes sorted input lists.
 *
 * For each value in A, remove first value in B until it is of equal or higher value
 * Then take the value from A and equal or higher of B and add to output, then continue with the next set
 */
fun higherValueZip(A: MutableList<Int>, B: MutableList<Int>): List<Pair<Int, Int>> {
    val output = mutableListOf<Pair<Int, Int>>()
    outer@ do {
        val minimum = A.removeFirst()
        var possible: Int
        do {
            possible = B.removeFirstOrNull() ?: break@outer
        } while (possible < minimum)
        output.add(Pair(minimum, possible))
    } while (A.isNotEmpty() && B.isNotEmpty())
    return output
}

// in case everything goes wrong
class UnknownInsnNode : AbstractInsnNode(-1) {
    init {
        logger.error("There was a problem loading an instruction! A placeholder instruction has been used instead.")
    }

    override fun getType(): Int = -1

    override fun accept(methodVisitor: MethodVisitor?) = error("Tried to accept a nonexistent instruction!")

    override fun clone(clonedLabels: MutableMap<LabelNode, LabelNode>?) = UnknownInsnNode()
}

// Utility functions for booleans
fun Boolean.toInt() = if (this) 1 else 0
fun Iterable<Boolean>.all() = all { it }
fun Iterable<Boolean>.any() = any { it }
fun Iterable<Boolean>.none() = none { it }

/**
 * Force-casts the given [KClass] to [T] by changing the underlying type.
 * Be warned that this changes the object itself into the new type. This is not a copy.
 *
 * It is useful for casting between classloaders and generics.
 */
fun <T : Any> Any.reinterpret(t: KClass<T>): T = Classes.reinterpret(this, t.java)

/**
 * Inline equivalent of [reinterpret] requiring no parameters.
 * This is why reified types are so useful.
 * :tiny_potato:
 */
inline fun <reified T : Any> Any.reinterpret(): T = reinterpret(T::class)

// :tiny_potato:
@Functional interface Thrower<R> {
    @Throws(Throwable::class) fun run(): R
}

/**
 * Try to run [thing], but if it throws an exception, too bad.
 */
fun <R> rethrowIfException(thing: Thrower<R>) = thing.run()

// make java happy by doing questionable things in Kotlin
fun rethrow(thing: Throwable): RuntimeException = throw thing

/**
 * A delegate to make a lateinit property read-only after initialization.
 */
@Suppress("UNCHECKED_CAST")
fun <V : Any> lateInitVal() = object : ReadWriteProperty<Any?, V> {
    private lateinit var v: Any

    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        if (::v.isInitialized) return v as V else throw NotImplementedError("This value hasn't been initialized!")
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        if (::v.isInitialized) throw IllegalArgumentException("This value is a delegated lateinit val and can't be re-initialized!")
        v = value
    }

}

// this is very yes
inline val currentClassLoader: ClassLoader get() = Thread.currentThread().contextClassLoader

internal val gson = GsonBuilder()
    .registerTypeAdapter(ClassLoadingTree::class.java, ClassLoadingTree) // companion objects go brr
    .create()

val classInheritanceTree: ClassLoadingTree by lazy {
    val classLoader = object {}::class.java.classLoader
    logger.info("Getting class inheritance tree (using classloader $classLoader)")
    // get classpath hash
    val classpath = System.getProperty("java.class.path")
    val classpathHash = sha512(classpath)
    val hashFile = debugFolder / "classpath.hash"

    // check if hash is different
    if (hashFile.exists()) {
        val oldHash = hashFile.readBytes()
        if (classpathHash.contentEquals(oldHash)) {
            // hashes are the same, use inheritance tree that's already there
            val inheritanceTreeFile = debugFolder / "inheritance.json"
            return@lazy gson.fromJson(inheritanceTreeFile.readText(), ClassLoadingTree::class.java)
        }
    } else {
        // hash file doesn't exist, create it
        hashFile.writeBytes(classpathHash)
    }
    // hashes are different or file doesn't exist, we must create a new inheritance tree in a new process
    logger.warn("Creating new inheritance tree. This may take a while if you have a lot of mods / libraries")

    val javaExecutable = "${System.getProperty("java.home")}/bin/java"
    val inheritanceTreeFile = debugFolder / "inheritance.json"
    val fullCommand = "$javaExecutable -cp $classpath net.khasm.init.KhasmInheritanceBuilder $inheritanceTreeFile"
    val process = ProcessBuilder(fullCommand.split(" "))
        .inheritIO()
        .start()
    // wait for process to finish building the inheritance tree
    process.waitFor()

    logger.info("Inheritance tree created")
    return@lazy gson.fromJson(inheritanceTreeFile.readText(), ClassLoadingTree::class.java)
}
