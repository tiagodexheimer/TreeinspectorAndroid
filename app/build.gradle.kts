plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	id("com.google.devtools.ksp") version "1.9.21-1.0.15" // ou
	id("org.jetbrains.kotlin.kapt")
}

android {
	namespace = "com.dexheimer.treeinspectorandroid"
	compileSdk {
		version = release(36)
	}

	defaultConfig {
		applicationId = "com.dexheimer.treeinspectorandroid"
		minSdk = 24
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
	kotlinOptions {
		jvmTarget = "11"
	}
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.constraintlayout)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	implementation("com.android.volley:volley:1.2.1")
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("com.google.android.gms:play-services-maps:18.2.0")
	implementation("com.google.maps.android:maps-utils-ktx:3.4.0")
	implementation(libs.androidx.constraintlayout)
	implementation(libs.volley)
	implementation(libs.osmdroid)
	implementation(libs.gson)
	implementation("com.google.android.gms:play-services-location:21.2.0")

	val room_version = "2.6.1"

	implementation("androidx.room:room-runtime:$room_version")
	annotationProcessor("androidx.room:room-compiler:$room_version")

// Para Kotlin (KAPT)
	kapt("androidx.room:room-compiler:$room_version")

// Para coroutines (recomendado)
	implementation("androidx.room:room-ktx:$room_version")

}
