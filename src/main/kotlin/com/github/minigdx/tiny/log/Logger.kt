package com.github.minigdx.tiny.log

import java.time.LocalTime


enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

interface Logger {

    val level: LogLevel

    fun debug(tag: String, message: () -> String)
    fun debug(tag: String, exception: Throwable, message: () -> String)
    fun info(tag: String, message: () -> String)
    fun info(tag: String, exception: Throwable, message: () -> String)

    fun warn(tag: String, message: () -> String)
    fun warn(tag: String, exception: Throwable, message: () -> String)
    fun error(tag: String, message: () -> String)
    fun error(tag: String, exception: Throwable, message: () -> String)
}

class StdOutLogger(override val level: LogLevel = LogLevel.DEBUG) : Logger {

    private fun log(level: LogLevel, tag: String, exception: Throwable?, message: () -> String) {
        if(level.ordinal >= this.level.ordinal) {
            val l = when(level) {
                LogLevel.DEBUG -> "🧰"
                LogLevel.INFO -> "ℹ️"
                LogLevel.WARN -> "⚠️"
                LogLevel.ERROR -> "💥"
            }
            println("$l (${LocalTime.now()}) - [$tag] : " + message())
            exception?.printStackTrace()
        }
    }
    override fun debug(tag: String, message: () -> String) {
        log(LogLevel.DEBUG, tag, null, message)
    }

    override fun debug(tag: String, exception: Throwable, message: () -> String) {
        log(LogLevel.DEBUG, tag, exception, message)
    }

    override fun info(tag: String, message: () -> String) {
        log(LogLevel.INFO, tag, null, message)
    }

    override fun info(tag: String, exception: Throwable, message: () -> String) {
        log(LogLevel.INFO, tag, exception, message)
    }

    override fun warn(tag: String, message: () -> String) {
        log(LogLevel.WARN, tag, null, message)
    }

    override fun warn(tag: String, exception: Throwable, message: () -> String) {
        log(LogLevel.WARN, tag, exception, message)
    }

    override fun error(tag: String, message: () -> String) {
        log(LogLevel.ERROR, tag, null, message)
    }

    override fun error(tag: String, exception: Throwable, message: () -> String) {
        log(LogLevel.ERROR, tag, exception, message)
    }
}
