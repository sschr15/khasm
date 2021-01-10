package net.khasm.test

import net.khasm.transform.KhasmTransformerBuilder
import net.khasm.transform.KhasmTransformerDispatcher
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

object KhasmTest {
    fun registerTest() {
        KhasmTransformerDispatcher.registerTransformer(KhasmTransformerBuilder {
            classTarget {
                name.endsWith("TitleScreen")
            }

            methodTarget {
                name + desc == "tick()V"
            }

            targets {
                listOf(0)
            }

            action { node: MethodNode, ins: AbstractInsnNode ->
                node.visitCode()
                node.instructions.clear()
                node.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
                node.visitLdcInsn("Hello world")
                node.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
                node.visitInsn(RETURN)
                node.visitMaxs(2, 1)

                node.instructions.forEach {
                    println(it)
                }
            }
        }.build())
    }
}
