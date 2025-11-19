plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
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

	buildFeatures {
		buildConfig = true
	}

	buildTypes {
		debug {
			// "API_BASE_URL" será o nome da sua variável
			// "http://10.0.2.2:3000/api" é o valor (note as aspas escapadas \"\")
			// O /api no final é porque suas duas activities usam isso como base
			buildConfigField(
				type = "String",
				name = "API_BASE_URL",
				value = "\"http://10.0.2.2:3000/\""
			)
		}
		release {
			// ...
			// ISTO DEVE ESTAR PERFEITO:
			buildConfigField("String", "API_BASE_URL", "\"https://www.treeinspector.com.br/\"")
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

	implementation(libs.retrofit.core)
	implementation(libs.retrofit.converter.gson)
	implementation(libs.okhttp)
	implementation(libs.okhttp.logging.interceptor)
	implementation(libs.okhttp.urlconnection)

	val room_version = "2.6.1"

	implementation("androidx.room:room-runtime:$room_version")

// Para Kotlin (KAPT)
	ksp("androidx.room:room-compiler:2.6.1")

// Para coroutines (recomendado)
	implementation("androidx.room:room-ktx:$room_version")

	implementation("androidx.work:work-runtime-ktx:2.9.0")
	implementation("androidx.activity:activity-ktx:1.9.0")
	implementation(libs.hilt.android)
	ksp(libs.hilt.compiler)

}
