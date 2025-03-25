package com.github.minigdx.tiny.cli.debug

import kotlinx.coroutines.channels.Channel

/**
 * [CoroutineBlocker] is a utility class designed to manage the blocking and unblocking of coroutines.
 * It uses a [Channel] to act as a signal mechanism.
 */
class CoroutineBlocker {
    /**
     * The [Channel] used for signaling. It's a channel of [Unit] because we're only interested in the signal, not the data.
     */
    private val channel = Channel<Unit>()

    /**
     * Suspends the coroutine until a signal is received.
     */
    suspend fun block() {
        channel.receive()
    }

    /**
     * Sends a signal to unblock a coroutine that is currently suspended in the [block] function.
     */
    fun unblock() {
        channel.trySend(Unit)
    }
}
