package com.dexheimer.treeinspectorandroid.presentation.login

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoginTest {

	@get:Rule
	var hiltRule = HiltAndroidRule(this)

	@Inject
	lateinit var sessionManager: SessionManager

	@Before
	fun init() {
		hiltRule.inject()
		// Garante que começamos limpos
		sessionManager.clearSession()
	}

	@Test
	fun loginFlow_verificaElementosVisiveis() {
		// O 'use' garante que a Activity é fechada corretamente no fim do teste
		ActivityScenario.launch(MainActivity::class.java).use {
			onView(withId(R.id.editTextEmail)).check(matches(isDisplayed()))
			onView(withId(R.id.editTextPassword)).check(matches(isDisplayed()))
			onView(withId(R.id.buttonLogin)).check(matches(isDisplayed()))
		}
	}

	@Test
	fun loginFlow_tentaLogar() {
		ActivityScenario.launch(MainActivity::class.java).use {
			// Digita e fecha o teclado imediatamente para evitar roubo de foco
			onView(withId(R.id.editTextEmail))
				.perform(typeText("admin@treeinspector.com"), closeSoftKeyboard())

			onView(withId(R.id.editTextPassword))
				.perform(typeText("senha123"), closeSoftKeyboard())

			onView(withId(R.id.buttonLogin))
				.perform(click())
		}
	}
}