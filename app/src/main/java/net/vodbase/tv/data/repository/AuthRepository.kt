package net.vodbase.tv.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.vodbase.tv.data.api.VodBaseApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val api: VodBaseApi
) {
    companion object {
        val DEVICE_TOKEN_KEY = stringPreferencesKey("device_token")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    }

    val isAuthenticated: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DEVICE_TOKEN_KEY] != null
    }

    val userEmail: Flow<String?> = dataStore.data.map { prefs ->
        prefs[USER_EMAIL_KEY]
    }

    suspend fun getDeviceToken(): String? {
        return dataStore.data.first()[DEVICE_TOKEN_KEY]
    }

    suspend fun createQrSession() = api.createQrSession()

    suspend fun pollQrSession(token: String) = api.pollQrSession(token)

    suspend fun saveAuth(deviceToken: String, email: String) {
        dataStore.edit { prefs ->
            prefs[DEVICE_TOKEN_KEY] = deviceToken
            prefs[USER_EMAIL_KEY] = email
        }
    }

    suspend fun logout() {
        dataStore.edit { prefs ->
            prefs.remove(DEVICE_TOKEN_KEY)
            prefs.remove(USER_EMAIL_KEY)
        }
    }

    suspend fun validateToken(): Boolean {
        val token = getDeviceToken() ?: return false
        return try {
            val response = api.getProgress("jerma", token)
            true // if we get a response, the token is valid
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                logout() // clear invalid token
                false
            } else {
                true // network error, assume valid
            }
        } catch (e: Exception) {
            true // network error, assume valid
        }
    }
}
