package ch.ubique.eiam.auth.extensions

import net.openid.appauth.AuthState

/**
 * Because the AppAuth update methods directly change the AuthState instance, a StateFlow would not emit the value again because the
 * underlying value already changed inside the flow. This extension function helps by copying an AuthState via JSON serialization
 * and deserialization to create a new instance.
 */
internal fun AuthState.copy(): AuthState {
	val serialized = jsonSerialize()
	return AuthState.jsonDeserialize(serialized)
}