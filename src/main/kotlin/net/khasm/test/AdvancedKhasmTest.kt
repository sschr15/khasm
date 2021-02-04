package net.khasm.test

import codes.som.anthony.koffee.insns.jvm.*
import net.khasm.transform.`class`.KhasmClassTransformerDispatcher
import net.khasm.transform.method.target.CursorsFixed
import net.khasm.transform.method.target.HeadTarget
import net.khasm.transform.method.target.MethodInvocationTarget
import net.khasm.util.mapClass
import net.minecraft.client.gui.screen.TitleScreen
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.TableSwitchGenerator
import java.io.PrintStream
import java.lang.annotation.Documented

object AdvancedKhasmTest {
    /**
     * A more advanced test to use more than the example mixin's reach.
     * This test targets [TitleScreen]
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

                // transformMethod automatically sets the class target to the current class target.
                transformMethod {
                    // Screen.init (see KhasmTest)
                    methodTarget("net.minecraft.class_437", "method_25426", "()V")

                    target { HeadTarget() }

                    action {
                        rawInject {
                            aload_0 // this
                            checkcast(TableSwitchGenerator::class)
                            invokeinterface(TableSwitchGenerator::class, "generateDefault", void)
                        }
                    }
                }

                transformMethod {
                    // Screen.init (see KhasmTest)
                    methodTarget("net.minecraft.class_437", "method_25426", "()V")

                    target { HeadTarget() }

                    action {
                        smartInject(mapClass("net.minecraft.class_442"), action = AdvancedKhasmTest::thing)
                    }
                }

                transformMethod {
                    val matrices = mapClass("net.minecraft.class_4587").replace('.', '/')
                    // Drawable.render(MatrixStack, int, int, float)
                    methodTarget("net.minecraft.class_4068", "method_25394", "(L$matrices;IIF)V")

                    target {
                        // I18n.translate(String, Object...)
                        MethodInvocationTarget("class_1074", "method_4662", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String")
                            .filter { CursorsFixed(it.points.map { i -> i + 3 }) }
                    }

                    action { rawInject {
                        astore(11)
                        new(StringBuilder::class)
                        dup
                        invokespecial(StringBuilder::class, "<init>", "()V")
                        aload(11)
                        invokevirtual(StringBuilder::class, "append", StringBuilder::class, String::class)
                        ldc(" (Khasm)")
                        invokevirtual(StringBuilder::class, "append", StringBuilder::class, String::class)
                        invokevirtual(StringBuilder::class, "toString", String::class)
                        astore(11)
                    } }
                }
            }
        }
    }

    @JvmStatic
    fun thing(thiz: TitleScreen) {
        println(thiz.height)
    }
}
