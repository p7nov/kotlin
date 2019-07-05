@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER
)
annotation class Foo

val x: @Foo Int get() = 10