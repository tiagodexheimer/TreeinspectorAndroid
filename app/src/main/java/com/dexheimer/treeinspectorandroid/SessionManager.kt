// app/src/main/java/com/dexheimer/treeinspectorandroid/SessionManager.kt
package com.dexheimer.treeinspectorandroid

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

	private val prefs: SharedPreferences =
		context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

	companion object {
		const val IS_LOGGED_IN = "isLoggedIn"
	}

	// Salva o estado de login
	fun setLoggedIn(isLoggedIn: Boolean) {
		prefs.edit().putBoolean(IS_LOGGED_IN, isLoggedIn).apply()
	}

	// Verifica o estado de login
	fun isLoggedIn(): Boolean {
		return prefs.getBoolean(IS_LOGGED_IN, false)
	}
}