package net.khasm.test

import codes.som.anthony.koffee.insns.jvm.*
import net.khasm.transform.KhasmTransformerDispatcher
import org.objectweb.asm.tree.LineNumberNode
import java.io.PrintStream
import java.util.*

object KhasmTest {
    fun registerTest() {
        KhasmTransformerDispatcher.registerTransformer {
            classTarget {
                // TitleScreen
                name.replace('/', '.') == mapClass("net.minecraft.class_442")
            }

            methodTarget {
                // TickableElement.tick() (TitleScreen is an implementation of TickableElement)
                name == mapMethod("net.minecraft.class_4893", "method_25393", "()V")
                        && desc == "()V"
            }

            targets {
                listOf(instructions.indexOfFirst { it is LineNumberNode && it.line == 82 })
            }

            action {
                getstatic(System::class, "out", PrintStream::class)
                new(StringBuilder::class)
                dup
                invokespecial(StringBuilder::class, "<init>", void)
                new(Random::class)
                dup
                invokespecial(Random::class, "<init>", void)
                invokevirtual(Random::class, "nextInt", int)
                invokevirtual(StringBuilder::class, "append", StringBuilder::class, int)
                ldc(" ")
                invokevirtual(StringBuilder::class, "append", StringBuilder::class, String::class)
                invokevirtual(StringBuilder::class, "toString", String::class)
                invokevirtual(PrintStream::class, "print", void, String::class)

                maxStack = 2
                maxLocals = 1
            }
        }
    }
}
