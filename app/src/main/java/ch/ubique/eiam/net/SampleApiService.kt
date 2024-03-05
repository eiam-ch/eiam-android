package ch.ubique.eiam.net

import retrofit2.http.GET

interface SampleApiService {
    @GET("hello")
    suspend fun callSample() : String
}