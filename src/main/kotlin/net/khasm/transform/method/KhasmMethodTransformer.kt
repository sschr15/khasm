@file:Suppress("SpellCheckingInspection")

package net.khasm.transform.method

import codes.som.anthony.koffee.insns.jvm.*
import codes.som.anthony.koffee.insns.sugar.push_int
import codes.som.anthony.koffee.koffee
import net.khasm.transform.method.action.*
import net.khasm.transform.method.target.AbstractKhasmTarget
import net.khasm.transform.method.target.CursorRanges
import net.khasm.transform.method.target.CursorsFixed
import net.khasm.util.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.lang.Integer.max
import kotlin.math.min

class KhasmMethodTransformer(val modid: String) {
    // Predicates
    private var transformClassPredicate: (ClassNode) -> Boolean = { false }
    private var transformMethodPredicate: (MethodNode) -> Boolean = { false }

    // Marks transformer as targeting a single class and thus can be thrown away once transformed (probably a micro-optimisation idc)
    internal var oneTimeUse = false

    // Counter for smart injects
    private var smartInjectCounter = 0L

    internal val actions = mutableMapOf<AbstractKhasmTarget, MutableList<MethodTransformer>>()

    // Predicate setters
    fun setClassPredicate(predicate: (ClassNode) -> Boolean) {
        transformClassPredicate = predicate
    }

    fun setMethodPredicate(predicate: (MethodNode) -> Boolean) {
        transformMethodPredicate = predicate
    }

    // Predicate testing
    private fun shouldTransformClass(cls: ClassNode): Boolean {
        return transformClassPredicate(cls)
    }

    private fun shouldTransformMethod(method: MethodNode): Boolean {
        return transformMethodPredicate(method)
    }

    // Actual transformation
    fun tryTransformClass(classNode: ClassNode): Boolean {
        if (shouldTransformClass(classNode)) {
            for (method in classNode.methods) {
                if (shouldTransformMethod(method)) {
                    method!!
                    logger.debug("Transforming method " + method.name + method.desc)
                    val transformers = actions.flatMap { (k, v) -> v.map { k to it } }

                    transformers.forEach { (targetPredicate, methodTransformer) ->
                        val cursors = targetPredicate.getCursors(method)

                        if (cursors is CursorRanges) {
                            throw UnsupportedOperationException("Tried to pass a CursorRanges to target!")
                        }

                        val oldInsns = method.instructions.toList()

                        // clear instruction list then tell it that code exists
                        // Reflection is required here (see companion object) to
                        // separate the instructions from the InsnList object
                        insnListRemoveAll(method.instructions, true)
                        method.visitCode()
                        // split method instructions into the sections identified by the requested targets
                        val sections = getInsnSections(oldInsns, (cursors as CursorsFixed).points.sorted())
                        // use Koffee for direct bytecode-style commands (aload_2, iastore, etc)
                        method.koffee {
                            for ((section, nextIdx) in sections.mapIndexed { idx, list -> list to idx + 1 }.filter { it.second < sections.size }) {
                                // if we shouldn't override the method, insert whatever code should go first
                                if (!methodTransformer.isOverwrite) section.forEach { instructions.add(it) }

                                // We use a try/catch block just in case some weird list access stuff would occur
                                when (methodTransformer) {
                                    is RawMethodTransformer -> {
                                        ldc("Added by id '$modid'")
                                        invokestatic("net/khasm/transform/KhasmRuntimeAPI", "comment", void, String::class)
                                        methodTransformer.action(this, try {
                                            sections[nextIdx][0]
                                        } catch (e: IndexOutOfBoundsException) {
                                            UnknownInsnNode()
                                        })
                                        ldc("End addition by id '$modid'")
                                        invokestatic("net/khasm/transform/KhasmRuntimeAPI", "comment", void, String::class)
                                    }
                                    is SmartMethodTransformer -> {
                                        ldc("Smart inject method added by id '$modid'")
                                        invokestatic("net/khasm/transform/KhasmRuntimeAPI", "comment", void, String::class)
                                        // Create the method reference so we can call it
                                        var field = FieldNode(0, null, null, null, null)
                                        classNode.koffee {
                                            field = field(
                                                private + static + synthetic, methodTransformer.internalName,
                                                Function::class
                                            )
                                            KhasmMethodTransformerDispatcher.appliedFunctions
                                                .getOrPut(node.name) { mutableListOf() }
                                                .add(methodTransformer)
                                        }

                                        // Get arguments and return type
                                        val args = methodTransformer.params
                                        val returnType = Type.getReturnType(node.desc)

                                        getstatic(classNode, field)

                                        // load local variables
                                        push_int(args.size)
                                        anewarray(Any::class)

                                        for (i in args.indices) {
                                            dup
                                            push_int(i)

                                            // This is a fiendishly complicated way to turn primitives into an object
                                            val type = when (args[i]) {
                                                "int" -> int
                                                "long" -> long
                                                "byte" -> byte
                                                "char" -> char
                                                "short" -> short
                                                "float" -> float
                                                "double" -> double
                                                "boolean" -> boolean
                                                else -> coerceType(Any::class)
                                            }
                                            when (type) {
                                                byte, short, int, char, boolean -> iload(i)
                                                long -> lload(i)
                                                float -> fload(i)
                                                double -> dload(i)
                                                else -> aload(i)
                                            }
                                            invokestatic("net/khasm/transform/KhasmRuntimeAPI", "toObject", Any::class, type)

                                            aastore
                                        }

                                        // invoke the "correct" way
                                        invokestatic("net/khasm/transform/KhasmRuntimeAPI", "invoke", Any::class, Function::class, Array<Any>::class)

                                        // if overwriting, return the result as intended
                                        if (methodTransformer.isOverwrite) {
                                            val mustConvert = when (returnType.descriptor) {
                                                "I", "J", "B", "C", "S", "F", "D", "Z" -> true
                                                else -> false
                                            }
                                            if (mustConvert)
                                                // it can't just be called `toPrimitive` because Java would say no. We say `toTypeDescriptor`
                                                invokestatic("net/khasm/transform/KhasmRuntimeAPI", "to${returnType.descriptor}", returnType, Any::class)

                                            when (returnType.className) {
                                                "byte", "short", "int", "boolean", "char" -> ireturn
                                                "long" -> lreturn
                                                "float" -> freturn
                                                "double" -> dreturn
                                                "void" -> `return`
                                                else -> areturn
                                            }
                                        } else {
                                            // if it's not overwriting, we need to get rid of the return value
                                            // (we don't have to pop2 because a double or a long is returned as an object)
                                            pop
                                        }
                                    }
                                    is SmarterOverrideTransformer -> {
                                        // yay for overwriting methods
                                        val newMethod = methodTransformer.getMethodNode()
                                        node.instructions = newMethod.instructions
                                        node.maxLocals = newMethod.maxLocals
                                        node.maxStack = newMethod.maxStack
                                        node.tryCatchBlocks = newMethod.tryCatchBlocks
                                        node.localVariables = newMethod.localVariables
                                        node.exceptions = newMethod.exceptions

                                        // access has to be handled specially
                                        val accessibility = max(node.accessLevel, newMethod.accessLevel).getBitmask()
                                        val newAccess = (newMethod.access or AccessLevel.getDefaultBitmask()) and accessibility
                                        val access = node.modifiers
                                        node.access = if (static in access) {
                                            newAccess or static.access // force static
                                        } else {
                                            newAccess and static.access.inv() // force non-static
                                        }
                                        if (final !in access) {
                                            node.access = node.access and final.access.inv() // force non-final
                                        }

                                        node.visitAnnotation("net/khasm/transform/KhasmOverwrite", true).also {
                                            it.visit("modid", modid)
                                            it.visit("declaringClass", coerceType(methodTransformer.parentClass))
                                        }
                                    }
                                }
                            }
                        }

                        // the last group of instructions
                        if (!methodTransformer.isOverwrite) sections.lastOrNull()?.forEach { method.instructions.add(it) }
                        // method's all done being modified, end it off
                        method.visitEnd()
                    }
                }
            }
            return true
        }
        return false
    }

    private fun getInsnSections(instructions: List<AbstractInsnNode>, breakLocations: List<Int>): List<List<AbstractInsnNode>> {
        // Target above the instruction by default (and caps to start of method)
        val offsetLocations = breakLocations.map { max(0, it - 1) }

        val sections = mutableListOf<List<AbstractInsnNode>>()
        var prevLocation = 0
        // List is mapped to be in the list range then turned into a set to prevent duplicates
        for (n in offsetLocations.map { min(it, instructions.size) }.toSet()) {
            val section = instructions.subList(prevLocation, n + 1)
            sections.add(section)
            prevLocation = n + 1
        }

        // get last instructions without a break location after any
        val section = instructions.subList(prevLocation, instructions.size)
        sections.add(section)
        return sections
    }

    companion object {
        private val insnListRemoveAll = InsnList::class.java.getDeclaredMethod("removeAll", Boolean::class.java)
            .also { it.isAccessible = true }
    }
}
