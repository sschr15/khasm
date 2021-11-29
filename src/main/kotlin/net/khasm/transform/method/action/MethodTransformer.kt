package net.khasm.transform.method.action

import codes.som.anthony.koffee.MethodAssembly
import codes.som.anthony.koffee.types.TypeLike
import codes.som.anthony.koffee.types.coerceType
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Method
import java.util.*

sealed class MethodTransformer(val type: MethodActionType) {
    val isOverwrite get(): Boolean {
        return when(type) {
            MethodActionType.RAW_OVERWRITE, MethodActionType.SMART_OVERWRITE -> true
            MethodActionType.SMART_INJECT, MethodActionType.RAW_INJECT -> false
        }
    }
}

class RawMethodTransformer(typeRaw: MethodActionType, val action: MethodAssembly.(AbstractInsnNode) -> Unit): MethodTransformer(typeRaw)

class SmartMethodTransformer(typeSmart: MethodActionType, val action: Function<*>, vararg params: TypeLike) : MethodTransformer(typeSmart) {
    val params = params.map { p -> coerceType(p).let { if ('.' in it.className) it.internalName else it.className }!! }
    val internalName = "\$khasm\$smartInject\$${Integer.toHexString(Random().nextInt())}"
}

class SmarterOverrideTransformer(val method: Method) : MethodTransformer(MethodActionType.SMART_OVERWRITE) {
    val parentClass: Class<*> = method.declaringClass

    fun getMethodNode(): MethodNode {
        val classNode = ClassNode()
        ClassReader(parentClass.getResourceAsStream(parentClass.name.replace('.', '/') + ".class"))
            .accept(classNode, 0)
        return classNode.methods.first { it.name == method.name && it.desc == Type.getMethodDescriptor(method) }
    }
}

enum class MethodActionType {
    RAW_INJECT,
    RAW_OVERWRITE,
    SMART_INJECT,
    SMART_OVERWRITE
}
