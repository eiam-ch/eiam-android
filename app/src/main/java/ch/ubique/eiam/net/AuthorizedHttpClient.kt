package ch.ubique.eiam.net

import ch.ubique.auth.storage.PreferencesAuthStorage
import ch.ubique.eiam.auth.AuthRepository
import ch.ubique.eiam.auth.network.AuthorizationHeaderInterceptor
import ch.ubique.eiam.BuildConfig
import ch.ubique.eiam.common.EiamEnvironment
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class AuthorizedHttpClient(
    authRepository: AuthRepository,
    authService: AuthorizationService,
    private val authStorage:  PreferencesAuthStorage,
    private val baseUrlUserInfo: (AuthState) -> String,
    private val baseUrlApi: (EiamEnvironment) -> String
)  {
    val authorizationHeaderInterceptor: AuthorizationHeaderInterceptor =
        AuthorizationHeaderInterceptor(
            authStorage,
            authRepository,
            authService,
            authorizedBaseUrls = setOf(
                BuildConfig.IDP_REF_DISCOVERY_URL,
                BuildConfig.IDP_ABN_DISCOVERY_URL,
                BuildConfig.IDP_PROD_DISCOVERY_URL,
                BuildConfig.DEMO_REF_BACKEND,
                BuildConfig.DEMO_ABN_BACKEND,
                BuildConfig.DEMO_PROD_BACKEND
            )
        )
    private val okHttpClient: OkHttpClient =
        OkHttpClient.Builder().apply { addInterceptor(authorizationHeaderInterceptor) }
            .build()

    var sampleApiService : SampleApiService = sampleClient(authStorage.getCurrentEnvState())
        private set

    var userInfoService : UserInfoService = userInfoClient(authStorage.getCurrentAuthState())
        private set

    fun switchConfig() {
        userInfoService = userInfoClient(authStorage.getCurrentAuthState())
        sampleApiService = sampleClient(authStorage.getCurrentEnvState())
    }
    private fun userInfoClient(authState: AuthState): UserInfoService {
        val currentBaseUrl = baseUrlUserInfo(authState)
        val retrofitUserInfo: Retrofit =
            Retrofit.Builder().baseUrl(currentBaseUrl).addConverterFactory(
                ScalarsConverterFactory.create()
            ).client(okHttpClient).build()
        return retrofitUserInfo.create(UserInfoService::class.java)
    }

    private fun sampleClient(env: EiamEnvironment): SampleApiService {
        val retrofitApi: Retrofit = Retrofit.Builder().baseUrl(baseUrlApi(env)).addConverterFactory(
            ScalarsConverterFactory.create()
        ).client(okHttpClient).build()
        return retrofitApi.create(SampleApiService::class.java)
    }
}