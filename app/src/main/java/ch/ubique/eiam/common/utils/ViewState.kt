package ch.ubique.eiam.common.utils

sealed class ViewState<T> {
    abstract val data: T?

    data class Success<T>(override val data: T) : ViewState<T>()

    data class Error<T>(
        val throwable: Throwable,
        override val data: T? = null,
        var retry: (() -> Unit)? = null
    ) : ViewState<T>()

    data class Loading<T>(
        val silent: Boolean = true,
        override val data: T? = null,
    ) : ViewState<T>()

    data class Idle<T>(override val data: T? = null) : ViewState<T>()

    /**
     * Convenience method to turn any view state into a loading state, applying [transformation] to the current data
     */
    fun toLoading(silent: Boolean = true, data: T? = this.data): Loading<T> = Loading(silent, data)

    /**
     * Convenience method to turn any view state into an success state, applying [transformation] to the current data
     */
    fun toSuccess(data: T): Success<T> = Success(data)

    /**
     * Convenience method to turn any view state into an error state, applying [transformation] to the current data
     */
    fun toError(throwable: Throwable, retry: (() -> Unit)? = null, data: T? = this.data): Error<T> =
        Error(throwable, data, retry)

    /**
     * Convenience method to turn any view state into a idle state, applying [transformation] to the current data
     */
    fun toIdle(data: T? = this.data): Idle<T> = Idle(data)


    /**
     * Convenience method to update the data with the applied [transformation] while keeping the same view state
     */
    fun <Q> updateData(transformation: (T?) -> Q?): ViewState<Q> = when (this) {
        is Loading -> Loading(this.silent, transformation.invoke(this.data))
        is Success -> Success(transformation.invoke(this.data)!!)
        is Error -> Error(this.throwable, transformation.invoke(this.data))
        is Idle -> Idle(transformation.invoke(this.data))
    }

    /**
     * Used to prevent long chains of ViewStates (i.e. this.previousViewState.previousViewState.previousViewState....)
     */
    private fun withoutPreviousState(): ViewState<T> = when (this) {
        is Loading -> Loading(this.silent, this.data)
        is Success -> Success(this.data)
        is Error -> Error(this.throwable, this.data, null)
        is Idle -> Idle(this.data)
    }
}