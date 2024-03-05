package ch.ubique.eiam.common.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class SingleEventFlow<T> {

    private val bufferedChannel = Channel<T>(Channel.BUFFERED)

    var hasFiredAtLeastOnce = false
        private set

    fun emit(value: T) {
        bufferedChannel.trySend(value)
        hasFiredAtLeastOnce = true
    }

    fun asFlow() = bufferedChannel.receiveAsFlow()

}

/**
 * Used for cases where T is Unit, to make calls cleaner.
 */
fun SingleEventFlow<Unit>.emit() {
    emit(Unit)
}
