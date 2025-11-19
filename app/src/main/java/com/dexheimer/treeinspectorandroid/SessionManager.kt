package com.dexheimer.treeinspectorandroid

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

	private val prefs: SharedPreferences =
		context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

	companion object {
		const val IS_LOGGED_IN = "isLoggedIn"
		const val AUTH_COOKIE = "auth_cookie" // [NOVO]
	}

	fun setLoggedIn(isLoggedIn: Boolean) {
		prefs.edit().putBoolean(IS_LOGGED_IN, isLoggedIn).apply()
	}

	fun isLoggedIn(): Boolean {
		return prefs.getBoolean(IS_LOGGED_IN, false)
	}

	// [NOVO] Salva o cookie completo
	fun saveAuthToken(token: String) {
		prefs.edit().putString(AUTH_COOKIE, token).apply()
	}

	// [NOVO] Recupera o cookie
	fun getAuthToken(): String? {
		return prefs.getString(AUTH_COOKIE, null)
	}
}