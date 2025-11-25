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
	// --- UI/Core AndroidX ---
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.activity.ktx) // Usando alias KTX
	implementation(libs.androidx.constraintlayout)

	// --- DI/Hilt ---
	implementation(libs.hilt.android)
	ksp(libs.hilt.compiler)
	implementation(libs.hilt.work)
	ksp(libs.hilt.work.compiler)

	// --- Banco de Dados (Room) ---
	implementation(libs.room.runtime)
	implementation(libs.room.ktx)
	ksp(libs.room.compiler)

	// --- Background (WorkManager) ---
	implementation(libs.work.runtime.ktx)

	// --- Rede / Serialização ---
	// Volley e GSON explícitos foram substituídos por aliases
	implementation(libs.retrofit.core)
	implementation(libs.retrofit.converter.gson)
	implementation(libs.okhttp)
	implementation(libs.okhttp.logging.interceptor)
	implementation(libs.okhttp.urlconnection)
	implementation(libs.gson)

	// --- Mapas / Localização ---
	// Removidas declarações explícitas e duplicadas
	implementation(libs.osmdroid)
	implementation(libs.play.services.maps)
	implementation(libs.maps.utils.ktx)
	implementation(libs.play.services.location)
	// Note: Volley foi removido por ser obsoleto após a refatoração do Login

	// --- Testes ---
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)

	implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}