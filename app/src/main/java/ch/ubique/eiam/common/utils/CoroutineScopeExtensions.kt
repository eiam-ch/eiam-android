package ch.ubique.eiam.common.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

fun <T> CoroutineScope.fetchAndUpdateStateFlow(
    mutableStateFlow: MutableStateFlow<ViewState<T>>,
    silent: Boolean = true,
    action: suspend CoroutineScope.() -> T
): Job {
    mutableStateFlow.value = mutableStateFlow.value.toLoading(silent = silent)
    return this.launch {
        try {
            mutableStateFlow.value = mutableStateFlow.value.toSuccess(action())
        } catch (e: Exception) {
            mutableStateFlow.value = mutableStateFlow.value.toError(e)
        }
    }
}