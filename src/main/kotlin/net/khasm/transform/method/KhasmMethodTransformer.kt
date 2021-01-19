@file:Suppress("SpellCheckingInspection")

package net.khasm.transform.method

import codes.som.anthony.koffee.insns.jvm.*
import codes.som.anthony.koffee.insns.sugar.push_int
import codes.som.anthony.koffee.koffee
import net.khasm.transform.method.action.ActionBuilder
import net.khasm.transform.method.action.RawMethodTransformer
import net.khasm.transform.method.action.SmartMethodTransformer
import net.khasm.transform.method.target.AbstractKhasmTarget
import net.khasm.transform.method.target.CursorRanges
import net.khasm.transform.method.target.CursorsFixed
import net.khasm.util.FunctionCallerAndRegistry
import net.khasm.util.UnknownInsnNode
import net.khasm.util.localVar
import net.khasm.util.logger
import org.objectweb.asm.tree.*
import java.lang.Integer.max
import kotlin.jvm.internal.Intrinsics
import kotlin.math.min

class KhasmMethodTransformer {
    // Predicates
    private var transformClassPredicate: (ClassNode) -> Boolean = { false }
    private var transformMethodPredicate: (MethodNode) -> Boolean = { false }

    // Transforming methods
    private lateinit var targetPredicate: AbstractKhasmTarget
    private lateinit var actionBuilder: ActionBuilder

    // Marks transformer as targeting a single class and thus can be thrown away once transformed (probably a micro-optimisation idc)
    internal var oneTimeUse = false

    // Counter for smart injects
    private var smartInjectCounter = 0L

    // Predicate setters
    fun setClassPredicate(predicate: (ClassNode) -> Boolean) {
        transformClassPredicate = predicate
    }

    fun setMethodPredicate(predicate: (MethodNode) -> Boolean) {
        transformMethodPredicate = predicate
    }

    fun setTargetPredicate(predicate: AbstractKhasmTarget) {
        targetPredicate = predicate
    }

    fun setAction(action: ActionBuilder.() -> Unit) {
        this.actionBuilder = ActionBuilder(action)
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
                    logger.info("Transforming method " + method.name + method.desc)
                    val cursors = targetPredicate.getCursors(method)

                    if (cursors is CursorRanges) {
                        throw UnsupportedOperationException("Tried to pass a CursorRanges to target!")
                    }

                    val methodTransformer = actionBuilder.methodTransformer
                        ?: throw IllegalStateException("Method is targeted with no action set!")

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
                            if (!methodTransformer.isOverwrite()) section.forEach { instructions.add(it) }

                            // We use a try/catch block just in case some weird list access stuff would occur
                            when (methodTransformer) {
                                is RawMethodTransformer -> methodTransformer.action(this, try {
                                        sections[nextIdx][0]
                                    } catch (e: IndexOutOfBoundsException) {
                                        UnknownInsnNode()
                                    })
                                is SmartMethodTransformer -> {
                                    // Save the Function<*> to the registry
                                    val index = FunctionCallerAndRegistry.addFunction(methodTransformer.action)

                                    // Create our labels for the condition
                                    val startLabel = LabelNode()
                                    val endLabel = LabelNode()

                                    // Start inject
                                    instructions.add(startLabel)
                                    // Local variable
                                    localVar("smartKhasmInject${smartInjectCounter}", Object::class, null, startLabel, endLabel, 1)
                                    // Push the index of the function
                                    push_int(index)
                                    // Tell our registry to call the function using the provided index
                                    invokestatic(FunctionCallerAndRegistry::class, "callFunction", "(I)Ljava/lang/Object;")
                                    // Load Unit.INSTANCE
                                    getstatic(Unit::class, "INSTANCE", "kotlin/Unit")
                                    // If not equal, jump to end of inject
                                    invokestatic(Intrinsics::class, "areEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
                                    ifne(endLabel)
                                    // Return nothing, or the value, dpending on method signature
                                    if (method.desc.endsWith("V")) {
                                        _return
                                    } else {
                                        aload_1
                                        areturn
                                    }
                                    // End inject
                                    instructions.add(endLabel)
                                }
                            }
                        }
                    }

                    // the last group of instructions
                    if (!methodTransformer.isOverwrite()) sections.lastOrNull()?.forEach { method.instructions.add(it) }
                    // method's all done being modified, end it off
                    method.visitEnd()
                }
            }
            return oneTimeUse
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
