package ch.ubique.eiam.auth.storage

import kotlinx.coroutines.flow.Flow
import net.openid.appauth.AuthState

interface AuthStorage {
	/**
	 * @return The current [AuthState] of the user
	 */
	fun getCurrentAuthState(): AuthState

	/**
	 * @return A cold flow emitting the current [AuthState] whenever it changes
	 */
	fun getAuthState(): Flow<AuthState>

	/**
	 * Replace the current [AuthState] with [newAuthState] in the backing storage
	 */
	fun replaceAuthState(newAuthState: AuthState)
}