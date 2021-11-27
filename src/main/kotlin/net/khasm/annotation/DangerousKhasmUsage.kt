@file:Suppress("unused")

package net.khasm.annotation

/**
 * Using something annotated with this may result in causing
 * unintended behavior within Minecraft itself.
 * Opt in with [`@DangerousKhasmUsage`][DangerousKhasmUsage]
 * or with a compile flag.
 */
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Usage of this may result in issues with further transformers or improper usage, " +
            "by using this I understand that any issues are my fault and my fault only " +
            "(Annotate with `@DangerousKhasmUsage`)"
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class DangerousKhasmUsage(
    val reason: String
)
