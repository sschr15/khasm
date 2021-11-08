package net.khasm.test

import codes.som.anthony.koffee.insns.jvm.*
import net.khasm.KhasmInitializer
import net.khasm.annotation.DangerousKhasmUsage
import net.khasm.transform.`class`.KhasmClassTransformerDispatcher
import net.khasm.transform.method.target.HeadTarget
import net.khasm.transform.method.target.MethodInvocationTarget
import net.khasm.transform.method.target.ReturnTarget
import net.khasm.util.*
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.TitleScreen
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.TableSwitchGenerator
import java.io.PrintStream
import java.lang.annotation.Documented

@Suppress("RemoveRedundantQualifierName", "MemberVisibilityCanBePrivate")
class AdvancedKhasmTest : KhasmInitializer() {
    /**
     * A more advanced test to use more than the example mixin's reach.
     * This test targets [TitleScreen]
     */
    fun registerTest() {
        KhasmClassTransformerDispatcher.registerClassTransformer("khasm-tests") {
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

                // isMinceraft :tiny_potato:
                transformField(mapField("net.minecraft.class_442", "field_17776", "Z")) {
                    private = false
                    public = true
                    final = false

                    value = true
                }

                // transformMethod automatically sets the class target to the current class target.
                @Suppress("ConstantConditionIf") // disabled because casting exceptions that I don't want to attempt to fix right now
                if (false) transformMethod {
                    // Screen.init (see KhasmTest)
                    methodTarget("net.minecraft.class_437", "method_25426", "()V")

                    target { HeadTarget() }

                    action {
                        rawInject {
                            aload_0 // this
                            forcecast(TableSwitchGenerator::class)
                            invokeinterface(TableSwitchGenerator::class, "generateDefault", void)
                        }
                    }
                }

                transformMethod {
                    // Screen.init (see KhasmTest)
                    methodTarget("net.minecraft.class_437", "method_25426", "()V")

                    target { HeadTarget() }

                    action {
                        smartInject(mapClass("net.minecraft.class_442"), action = ::thing)
                    }
                }

                transformMethod {
                    // Drawable.render(MatrixStack, int, int, float)
                    @OptIn(DangerousKhasmUsage::class)
                    methodTarget("net.minecraft.class_4068", "method_25394", "(Lnet/minecraft/class_4587;IIF)V", true)

                    target {
                        // boolean MinecraftClient.isModded()
                        MethodInvocationTarget("net.minecraft.class_310", "method_24289", "()Z")
                    }

                    action { rawInject {
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

        KhasmClassTransformerDispatcher.registerClassTransformer("khasm-tests") {
            // MinecraftClient
            val minecraftClient = "net.minecraft.class_310"

            classTarget(minecraftClient)

            action {
                transformField(mapField(minecraftClient, "field_1762", "Lorg/apache/logging/log4j/Logger;")) {
                    private = false
                    final = false
                }

                transformMethod {
                    @OptIn(DangerousKhasmUsage::class)
                    methodTarget("net.minecraft.class_310", "<init>", "(Lnet/minecraft/class_542;)V", true)

                    target { ReturnTarget() }

                    action {
                        smartInject(action = ::overrideMinecraftClientLogger)
                    }
                }
            }
        }
    }

    fun overrideMinecraftClientLogger() {
        val mcLoggerField = MinecraftClient::class.java.getDeclaredField(mapField("net.minecraft.class_310", "field_1762", "Lorg/apache/logging/log4j/Logger;"))
        var mcLogger by FieldReflectDelegate<Logger>(mcLoggerField, null)

        val mcGetWindowTitle by MethodReflectDelegate<String>(
            MinecraftClient::class.java.getDeclaredMethod(mapMethod("net.minecraft.class_310", "method_24287", "()Ljava/lang/String;")),
            MinecraftClient.getInstance()
        )

        mcLogger = LogManager.getLogger("Minecraft (khasm-was-here)")
        mcLogger!!.info("The window title is ${mcGetWindowTitle()}")
    }

    override fun init() {
        registerTest()
    }
}

fun thing(thiz: Any) {
    val `this`: TitleScreen = thiz.reinterpret()
    println(`this`.title)
}
