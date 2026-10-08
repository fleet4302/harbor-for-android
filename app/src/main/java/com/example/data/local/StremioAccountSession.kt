package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StremioSessionState(
    val isLoggedIn: Boolean = false,
    val authKey: String? = null,
    val email: String? = null,
    val userId: String? = null
)

class StremioAccountSession(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("harbor_stremio_account", Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow(loadSession())
    val sessionState: StateFlow<StremioSessionState> = _sessionState.asStateFlow()

    fun getAuthKey(): String? = _sessionState.value.authKey

    private fun loadSession(): StremioSessionState {
        val authKey = prefs.getString("auth_key", null)
        val email = prefs.getString("email", null)
        val userId = prefs.getString("user_id", null)
        val isLoggedIn = !authKey.isNullOrBlank()
        return StremioSessionState(
            isLoggedIn = isLoggedIn,
            authKey = authKey,
            email = email,
            userId = userId
        )
    }

    fun saveSession(authKey: String, email: String?, userId: String?) {
        prefs.edit()
            .putString("auth_key", authKey)
            .putString("email", email)
            .putString("user_id", userId)
            .apply()
        _sessionState.value = StremioSessionState(
            isLoggedIn = true,
            authKey = authKey,
            email = email,
            userId = userId
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _sessionState.value = StremioSessionState(
            isLoggedIn = false,
            authKey = null,
            email = null,
            userId = null
        )
    }
}
