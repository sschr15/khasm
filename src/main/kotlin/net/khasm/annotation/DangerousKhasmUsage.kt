package net.khasm.annotation

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
    AnnotationTarget.FIELD
)
annotation class DangerousKhasmUsage
