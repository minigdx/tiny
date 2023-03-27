package com.github.minigdx.tiny.lua


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class DocFunction(
    val name: String,
    val documentation: String = "",
)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class DocArg(
    val name: String,
    val documentation: String = "",
)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class DocArgs(
    val names: Array<String>,
    val documentations: Array<String> = [],
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class DocCall(
    val documentation: String = "",
    val mainCall: Boolean = false
)
