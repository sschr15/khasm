package net.khasm.test

import codes.som.anthony.koffee.insns.jvm.*
import net.khasm.transform.`class`.KhasmClassTransformerDispatcher
import net.khasm.transform.method.KhasmMethodTransformerDispatcher
import net.khasm.transform.method.target.HeadTarget
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.TableSwitchGenerator
import java.io.PrintStream
import java.lang.annotation.Documented

object AdvancedKhasmTest {
    /**
     * A more advanced test to use more than the example mixin's reach
     */
    fun registerTest() {
        KhasmClassTransformerDispatcher.registerClassTransformer {
            // TitleScreen
            classTarget("net.minecraft.class_442")

            action {
                // :tiny_potato:
                annotation(Documented::class)

                implement(Opcodes::class)
                implement(TableSwitchGenerator::class)

                // implementing members of TableSwitchGenerator
                method(public, "generateCase", void, int, Label::class) {
                    `return`
                }

                method(public, "generateDefault", void) {
                    getstatic(System::class, "out", PrintStream::class)
                    ldc("Screen.generateDefault()V called!")
                    invokevirtual(PrintStream::class, "println", void, String::class)
                    `return`
                }
            }
        }

        KhasmMethodTransformerDispatcher.registerMethodTransformer {
            // TitleScreen
            classTarget("net.minecraft.class_442")

            // Screen.init (see KhasmTest)
            methodTarget("net.minecraft.class_437", "method_25426", "()V")

            target { HeadTarget() }

            action {
                aload_0 // this
                checkcast(TableSwitchGenerator::class)
                invokeinterface(TableSwitchGenerator::class, "generateDefault", void)
            }
        }
    }
}