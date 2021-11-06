package net.khasm.test

import net.khasm.transform.method.KhasmMethodTransformerDispatcher
import net.khasm.transform.method.target.HeadTarget

object KhasmTest {
    /**
     * This is functionally equivalent to the fabric-example-mod example mixin (with a slight text change)
     */
    @Suppress("RedundantLambdaArrow")
    fun registerTest() {
        KhasmMethodTransformerDispatcher.registerMethodTransformer("khasm-tests") {
            // TitleScreen
            classTarget("net.minecraft.class_442")

            // Screen.init (Called on open or resize)
            methodTarget("net.minecraft.class_437", "method_25426", "()V")

            target {
                HeadTarget()
            }

            action {
                smartInject {
                    println("This line is printed by the example khasm transformer!")
                }
            }
        }
    }
}
