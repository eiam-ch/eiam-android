package ch.ubique.eiam.eiam

import ch.ubique.eiam.R
import ch.ubique.eiam.auth.storage.AuthStorage
import ch.ubique.eiam.common.utils.DiagnoseViewState
import ch.ubique.eiam.net.AuthorizedHttpClient

data class PopupModel(val dialogTitleId: Int? = null, val dialogTextId: Int? = null, val dialogText: String? = null, val showPopup: Boolean = false)
data class PopupLoadingModel(val isUserInfoLoading : Boolean = false, val isRefreshTokenLoading: Boolean = false, val isSampleApiLoading : Boolean = false)
class ServicesViewModel(
    private val authorizedHttpClient: AuthorizedHttpClient,
    private val eiamAuthStorage: AuthStorage
) {
    suspend fun loadUserInfo() : DiagnoseViewState<PopupModel, PopupLoadingModel>{
        val userInfoEndpoint =
            eiamAuthStorage.getCurrentAuthState().authorizationServiceConfiguration?.discoveryDoc?.userinfoEndpoint
                ?: return DiagnoseViewState.Error(Exception(), PopupModel(
                        R.string.error_title, R.string.error_title))
        return try {
            val result =
                authorizedHttpClient.userInfoService.getUserInfo(userInfoEndpoint.toString())
            DiagnoseViewState.Success(PopupModel(
                R.string.detail_popup_title
            , dialogText = result))
        } catch (t: Throwable) {
            DiagnoseViewState.Error(t, PopupModel(
                R.string.detail_popup_title, R.string.error_title))
        }
    }

    suspend fun loadApi() : DiagnoseViewState<PopupModel, PopupLoadingModel> {
        return try {
            val result = authorizedHttpClient.sampleApiService.callSample()
            DiagnoseViewState.Success(PopupModel(R.string.detail_popup_title, dialogText = result))
        } catch (t: Throwable) {
            DiagnoseViewState.Error(t, PopupModel(R.string.detail_popup_title, R.string.error_title))
        }
    }
}