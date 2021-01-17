package net.khasm.test

import codes.som.anthony.koffee.insns.jvm.getstatic
import codes.som.anthony.koffee.insns.jvm.invokevirtual
import codes.som.anthony.koffee.insns.jvm.ldc
import net.khasm.transform.method.KhasmMethodTransformerDispatcher
import net.khasm.transform.method.target.HeadTarget
import java.io.PrintStream

object KhasmTest {
    /**
     * This is functionally equivalent to the fabric-example-mod example mixin (with a slight text change)
     */
    fun registerTest() {
        KhasmMethodTransformerDispatcher.registerMethodTransformer {
            // TitleScreen
            classTarget("net.minecraft.class_442")

            // Screen.init (Called on open or resize)
            methodTarget("net.minecraft.class_437", "method_25426", "()V")

            target {
                HeadTarget()
            }

            action {
                rawInject {
                    getstatic(System::class, "out", PrintStream::class)
                    ldc("This line is printed by the example khasm transformer!")
                    invokevirtual(PrintStream::class, "println", void, String::class)
                }
            }
        }
    }
}
