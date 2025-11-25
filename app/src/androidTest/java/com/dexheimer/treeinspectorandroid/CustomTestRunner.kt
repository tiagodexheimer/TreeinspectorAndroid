package com.dexheimer.treeinspectorandroid

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

// Este runner permite usar Hilt nos testes instrumentados
class CustomTestRunner : AndroidJUnitRunner() {
	override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
		return super.newApplication(cl, HiltTestApplication::class.java.name, context)
	}
}