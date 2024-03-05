package ch.ubique.eiam.auth

import com.auth0.android.jwt.DecodeException
import com.auth0.android.jwt.JWT
import net.openid.appauth.AuthState

fun AuthState.getProfileData(): ProfileData? {
    return try {
        idToken?.let {
            val claims = JWT(it).claims
            val firstName = claims["given_name"]?.asString()
            val lastName = claims["family_name"]?.asString()
            val email = claims["email"]?.asString()
            ProfileData(firstName, lastName, email)
        }
    } catch (e: DecodeException) {
        null
    }
}