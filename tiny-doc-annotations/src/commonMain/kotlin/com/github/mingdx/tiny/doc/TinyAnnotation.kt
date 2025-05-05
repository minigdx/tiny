package com.github.mingdx.tiny.doc

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TinyLib(
    /**
     * Name of the Library.
     */
    val name: String = "",
    /**
     * Description of the Library.
     */
    val description: String = "",
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class TinyFunction(
    /**
     * Description of the function.
     */
    val description: String = "",
    /**
     * Name of the function.
     * By default, will use the name of the class.
     */
    val name: String = "",
    /**
     * Lua Code use as example.
     * This code will be injected in the web documentation.
     */
    val example: String = "",
    /**
     * Expected sprite path associated to the example.
     */
    val spritePath: String = "",
    /**
     * Expected level path associated to the example.
     */
    val levelPath: String = "",
)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class TinyArg(
    /**
     * Name of the argument.
     */
    val name: String,
    /**
     * Description of the argument.
     */
    val description: String = "",
)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class TinyArgs(
    /**
     * Name of the arguments, in order.
     */
    val names: Array<String>,
    /**
     * Documentations associated to arguments, in order.
     */
    val documentations: Array<String> = [],
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class TinyCall(
    /**
     * Description of the call when called with those arguments.
     */
    val description: String = "",
)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class TinyVariable(
    /**
     * Name of the variable
     */
    val name: String,
    /**
     * Description of the variable
     */
    val description: String,
    /**
     * Should this be hidden in the asciidoctor?
     */
    val hideInDocumentation: Boolean = false,
)
