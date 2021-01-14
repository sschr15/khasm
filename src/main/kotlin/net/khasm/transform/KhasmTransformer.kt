@file:Suppress("SpellCheckingInspection")

package net.khasm.transform

import codes.som.anthony.koffee.MethodAssembly
import codes.som.anthony.koffee.koffee
import net.khasm.util.logger
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import kotlin.math.min

class KhasmTransformer {
    // Predicates
    private lateinit var transformClassPredicate: (ClassNode) -> Boolean
    private var transformMethodPredicate: (MethodNode) -> Boolean = { false }

    // Transforming methods
    private lateinit var targetPredicate: MethodNode.() -> List<Int>
    private lateinit var action: MethodAssembly.(AbstractInsnNode?) -> Unit

    internal var overrideMethod = false

    // Predicate setters
    fun setClassPredicate(predicate: (ClassNode) -> Boolean) {
        transformClassPredicate = predicate
    }

    fun setMethodPredicate(predicate: (MethodNode) -> Boolean) {
        transformMethodPredicate = predicate
    }

    fun setTargetPredicate(predicate: MethodNode.() -> List<Int>) {
        targetPredicate = predicate
    }

    fun setAction(action: MethodAssembly.(AbstractInsnNode?) -> Unit) {
        this.action = action
    }

    // Predicate testing
    private fun shouldTransformClass(cls: ClassNode): Boolean {
        return if (::transformClassPredicate.isInitialized) transformClassPredicate(cls) else false
    }

    private fun shouldTransformMethod(method: MethodNode): Boolean {
        return transformMethodPredicate(method)
    }

    // Actual transformation
    fun tryTransformClass(classNode: ClassNode) {
        if (shouldTransformClass(classNode)) {
            logger.info(classNode.name + " is being transformed")
            for (method in classNode.methods) {
                if (shouldTransformMethod(method)) {
                    logger.info("Transforming method " + method.name + method.desc)
                    val cursors = targetPredicate(method)
                    val oldInsns = method.instructions.toList()

                    // clear instruction list then tell it that code exists
                    // Reflection is required here (see companion object) to
                    // separate the instructions from the InsnList object
                    insnListRemoveAll(method.instructions, true)
                    method.visitCode()
                    // split method instructions into the sections identified by the requested targets
                    val sections = getInsnSections(oldInsns, cursors.sorted())
                    for (section in sections.filterIndexed { idx, _ -> idx + 1 < sections.size }) {
                        // if we shouldn't override the method, insert whatever code should go first
                        if (!overrideMethod) section.forEach { method.instructions.add(it) }
                        // use Koffee for direct bytecode-style commands (aload_2, iastore, etc)
                        method.koffee { this.action(section.lastOrNull()) }
                    }

                    // the last group of instructions
                    if (!overrideMethod) sections.lastOrNull()?.forEach { method.instructions.add(it) }
                    // method's all done being modified, end it off
                    method.visitEnd()
                }
            }
        }
    }

    private fun getInsnSections(instructions: List<AbstractInsnNode>, breakLocations: List<Int>): List<List<AbstractInsnNode>> {
        val sections = mutableListOf<List<AbstractInsnNode>>()
        var prevLocation = 0
        // List is mapped to be in the list range then turned into a set to prevent duplicates
        for (n in breakLocations.map { min(it, instructions.size) }.toSet()) {
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
