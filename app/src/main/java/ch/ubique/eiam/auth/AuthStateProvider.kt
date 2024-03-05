package ch.ubique.eiam.auth

import kotlinx.coroutines.flow.Flow

interface AuthStateProvider {
	/**
	 * @return True if the currently stored [net.openid.appauth.AuthState] is authorized
	 */
	fun isAuthorized(): Boolean

	/**
	 * @return A flow that emits true or false whenever the authorization state of the stored [net.openid.appauth.AuthState] changes
	 */
	fun getAuthorizedState(): Flow<Boolean>

	/**
	 * @return A flow that emits true or false whenever a session expiration is detected
	 */
	fun getSessionExpiration(): Flow<Boolean?>
}