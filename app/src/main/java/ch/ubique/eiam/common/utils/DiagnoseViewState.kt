package ch.ubique.eiam.common.utils

sealed class DiagnoseViewState<Result,Loading> {
    abstract val result : Result?
    abstract val loadingState: Loading?

    data class Success<Result, Loading>(override val result: Result,
                                        override val loadingState: Loading? = null) : DiagnoseViewState<Result, Loading>()
    data class Idle<Result, Loading>(override val result: Result? = null,
                                     override val loadingState: Loading? = null) : DiagnoseViewState<Result, Loading>()
    data class Error<Result, Loading>(val throwable: Throwable, override val result: Result,
                                      override val loadingState: Loading? = null) : DiagnoseViewState<Result, Loading>()
    data class Loading<Result, Loading>(override val result: Result? = null, override val loadingState: Loading) : DiagnoseViewState<Result, Loading>()

}