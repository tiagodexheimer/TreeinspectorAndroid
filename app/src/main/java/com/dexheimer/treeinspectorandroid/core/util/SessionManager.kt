package com.dexheimer.treeinspectorandroid.core.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

	private val prefs: SharedPreferences =
		context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

	companion object {
		const val IS_LOGGED_IN = "isLoggedIn"
		const val AUTH_COOKIE = "auth_cookie_string" // Nome da chave atualizado
	}

	fun setLoggedIn(isLoggedIn: Boolean) {
		prefs.edit().putBoolean(IS_LOGGED_IN, isLoggedIn).apply()
	}

	fun isLoggedIn(): Boolean {
		return prefs.getBoolean(IS_LOGGED_IN, false)
	}

	// --- MÉTODOS DE COOKIE (MANUAL) ---

	// Salva a string completa do cookie (ex: "authjs.session-token=xyz; next-auth...")
	fun saveCookieString(cookie: String) {
		prefs.edit().putString(AUTH_COOKIE, cookie).apply()
	}

	fun getCookieString(): String? {
		return prefs.getString(AUTH_COOKIE, null)
	}

	fun clearSession() {
		prefs.edit()
			.remove(IS_LOGGED_IN)
			.remove(AUTH_COOKIE)
			.apply()
	}
}