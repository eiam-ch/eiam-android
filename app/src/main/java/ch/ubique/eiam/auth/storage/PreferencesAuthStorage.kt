package ch.ubique.auth.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import ch.ubique.eiam.auth.storage.AuthStorage
import ch.ubique.eiam.auth.storage.EncryptedSharedPreferencesUtils
import ch.ubique.eiam.common.EiamEnvironment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthState

/**
 * Default implementation of an [AuthStorage] backed by a regular shared preferences file.
 * The preferences can either be provided by calling the primary constructor with an existing instance or let the storage create
 * a new (encrypted) preferences file by calling the secondary constructor and passing a name
 */
class PreferencesAuthStorage(
	private val preferences: SharedPreferences,
	private val authStatePreferencesKey: String,
	private val envStatePreferencesKey: String
) : AuthStorage {

	companion object {
		private const val DEFAULT_PREFERENCES_NAME = "auth_preferences"
		private const val KEY_AUTH_STATE = "KEY_AUTH_STATE"
		private const val KEY_ENV_STATE = "KEY_ENV_STATE"
	}

	constructor(
		context: Context,
		name: String = DEFAULT_PREFERENCES_NAME,
	) : this(EncryptedSharedPreferencesUtils.initializeSharedPreferences(context, name), KEY_AUTH_STATE, KEY_ENV_STATE)

	override fun getCurrentAuthState(): AuthState {
		return preferences.getString(authStatePreferencesKey, null)?.let { AuthState.jsonDeserialize(it) } ?: AuthState()
	}

	override fun getAuthState(): Flow<AuthState> {
		return callbackFlow {
			val callback = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
				if (key == authStatePreferencesKey) {
					trySend(getCurrentAuthState())
				}
			}

			// Send initial value
			send(getCurrentAuthState())

			preferences.registerOnSharedPreferenceChangeListener(callback)
			awaitClose {
				preferences.unregisterOnSharedPreferenceChangeListener(callback)
			}
		}
	}

	override fun replaceAuthState(newAuthState: AuthState) {
		preferences.edit { putString(authStatePreferencesKey, newAuthState.jsonSerializeString()) }
	}

	fun getCurrentEnvState() = runBlocking {
		preferences.getString(envStatePreferencesKey, null)?.let { EiamEnvironment.valueOf(it) } ?: EiamEnvironment.REF
	}

	fun getEnvState(): Flow<EiamEnvironment> {
		return callbackFlow {
			val callback = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
				if (key == envStatePreferencesKey) {
					trySend(getCurrentEnvState())
				}
			}

			// Send initial value
			send(getCurrentEnvState())

			preferences.registerOnSharedPreferenceChangeListener(callback)
			awaitClose {
				preferences.unregisterOnSharedPreferenceChangeListener(callback)
			}
		}
	}

	fun replaceEnvState(newEnvState: EiamEnvironment) {
		preferences.edit { putString(envStatePreferencesKey, newEnvState.name) }
	}
}