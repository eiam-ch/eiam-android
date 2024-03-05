package ch.ubique.eiam.auth

import android.content.Intent

data class LoginState(
    val authRequestIntent: Intent? = null,
    val isLoggedIn: Boolean = false,
    val configReloaded : Boolean = false
)