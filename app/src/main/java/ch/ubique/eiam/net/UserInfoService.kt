package ch.ubique.eiam.net

import retrofit2.http.GET
import retrofit2.http.Url

interface UserInfoService {
    @GET
    suspend fun getUserInfo(@Url url : String) : String
}