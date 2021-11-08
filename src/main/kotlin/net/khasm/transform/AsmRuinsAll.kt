package net.khasm.transform

import org.objectweb.asm.ClassWriter

/**
 * A modified version of [ClassWriter] that changes the class loader used for finding common superclasses.
 */
class KhasmClassWriter(flags: Int, private val modifiedClassLoader: ClassLoader) : ClassWriter(flags) {
    override fun getClassLoader(): ClassLoader {
        return modifiedClassLoader
    }
}
