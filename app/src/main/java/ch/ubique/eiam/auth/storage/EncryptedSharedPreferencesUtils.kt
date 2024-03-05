package ch.ubique.eiam.auth.storage

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.KeyStoreException

object EncryptedSharedPreferencesUtils {

	private val TAG = EncryptedSharedPreferencesUtils::class.java.simpleName

	@Synchronized
	fun initializeSharedPreferences(context: Context, preferencesName: String): SharedPreferences {
		return try {
			createEncryptedSharedPreferences(context, preferencesName)
		} catch (e: Exception) {
			// Try to recreate the shared preferences. This will cause any previous data to be lost.
			return try {
				if (e is KeyStoreException) {
					// Issues with the KeyStore, try to reset the master key
					tryToDeleteMasterKey()
				}

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					// Try to delete the shared preferences file via the API
					context.deleteSharedPreferences(preferencesName)
				} else {
					// Try to manually delete the shared preferences file
					tryToDeleteSharedPreferencesFile(context, preferencesName)
				}
				createEncryptedSharedPreferences(context, preferencesName)
			} catch (e2: Exception) {
				// Tried to delete and recreate shared preferences, cannot recover
				throw RuntimeException(e2)
			}
		}
	}

	/**
	 * Create or obtain an encrypted SharedPreferences instance. Note that this method is synchronized because the AndroidX
	 * Security library is not thread-safe.
	 * @see [https://developer.android.com/topic/security/data](https://developer.android.com/topic/security/data)
	 */
	@Synchronized
	@Throws(GeneralSecurityException::class, IOException::class)
	private fun createEncryptedSharedPreferences(context: Context, preferencesName: String): SharedPreferences {
		val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
			.setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
			.build()

		return EncryptedSharedPreferences.create(
			context,
			preferencesName,
			masterKey,
			EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
			EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
		)
	}

	private fun tryToDeleteSharedPreferencesFile(context: Context, preferencesName: String) {
		val sharedPreferencesFile = File(context.applicationInfo.dataDir + "/shared_prefs/" + preferencesName + ".xml")
		if (sharedPreferencesFile.exists()) {
			if (!sharedPreferencesFile.delete()) {
				Log.e(TAG, "Failed to delete $sharedPreferencesFile")
			}
		}
	}

	private fun tryToDeleteMasterKey() {
		val keyStore = KeyStore.getInstance("AndroidKeyStore")
		keyStore.load(null)
		keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
	}
}
