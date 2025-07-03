package com.github.mingdx.tiny.doc

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class CliAnnotation(
    val hidden: Boolean = true,
)
