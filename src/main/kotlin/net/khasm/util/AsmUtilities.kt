package net.khasm.util

import codes.som.anthony.koffee.modifiers.private
import codes.som.anthony.koffee.modifiers.protected
import codes.som.anthony.koffee.modifiers.public
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode

enum class AccessLevel(private val value: Int) {
    PUBLIC(Opcodes.ACC_PUBLIC),
    PRIVATE(Opcodes.ACC_PRIVATE),
    PROTECTED(Opcodes.ACC_PROTECTED),
    PACKAGE(0);

    fun getBitmask(): Int {
        // start with all 1s
        var mask = Int.MAX_VALUE
        // remove the access bits
        mask = mask and (public + private + protected).access.inv()
        // re-add the specified access bit
        mask = mask or value
        // return the mask
        return mask
    }

    companion object {
        fun getDefaultBitmask() = (public + private + protected).access
    }
}

val MethodNode.accessLevel: AccessLevel
    get() {
        return if (access and Opcodes.ACC_PUBLIC != 0) AccessLevel.PUBLIC
        else if (access and Opcodes.ACC_PROTECTED != 0) AccessLevel.PROTECTED
        else if (access and Opcodes.ACC_PRIVATE != 0) AccessLevel.PRIVATE
        else AccessLevel.PACKAGE
    }

fun max(a: AccessLevel, b: AccessLevel): AccessLevel {
    return when (a) {
        AccessLevel.PUBLIC -> a
        AccessLevel.PROTECTED -> if (b == AccessLevel.PUBLIC) b else a
        AccessLevel.PACKAGE -> if (b == AccessLevel.PUBLIC || b == AccessLevel.PROTECTED) b else a
        AccessLevel.PRIVATE -> b
    }
}
