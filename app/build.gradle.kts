plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.ksp)
	alias(libs.plugins.hilt)
	id("jacoco") // <--- OBRIGATÓRIO: Adiciona o plugin do Jacoco
}

android {
	namespace = "com.dexheimer.treeinspectorandroid"
	compileSdk = 36

	defaultConfig {
		applicationId = "com.dexheimer.treeinspectorandroid"
		minSdk = 24
		targetSdk = 36
		versionCode = 11
		versionName = "1.1"

		testInstrumentationRunner = "com.dexheimer.treeinspectorandroid.CustomTestRunner"
	}

	buildFeatures {
		buildConfig = true
	}

	buildTypes {
		debug {
			buildConfigField(
				type = "String",
				name = "API_BASE_URL",
				value = "\"http://10.0.2.2:3000/\""
			)
			// Habilita cobertura de código no modo debug
			enableUnitTestCoverage = true
			enableAndroidTestCoverage = true
		}
		release {
			buildConfigField("String", "API_BASE_URL", "\"https://www.treeinspector.com.br/\"")
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
	// --- UI/Core AndroidX ---
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.activity.ktx)
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
	implementation(libs.retrofit.core)
	implementation(libs.retrofit.converter.gson)
	implementation(libs.okhttp)
	implementation(libs.okhttp.logging.interceptor)
	implementation(libs.okhttp.urlconnection)
	implementation(libs.gson)

	// --- Mapas / Localização ---
	implementation(libs.osmdroid)
	implementation(libs.play.services.maps)
	implementation(libs.maps.utils.ktx)
	implementation(libs.play.services.location)

	// --- UI Utils ---
	implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

	// --- Testes ---
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)

	// Testes Unitários (Regras de Negócio)
	testImplementation("io.mockk:mockk:1.13.5")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

	// Testes de UI e Integração (Hilt + Espresso)
	androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
	kspAndroidTest("com.google.dagger:hilt-android-compiler:2.48")
	androidTestImplementation("androidx.test:runner:1.5.2")
}

// --- Configuração do Jacoco (Relatório de Cobertura) ---
tasks.register<JacocoReport>("jacocoTestReport") {
	// Garante que os testes rodam antes de gerar o relatório
	dependsOn("testDebugUnitTest")

	group = "Reporting"
	description = "Generate Jacoco coverage reports for the Debug build."

	reports {
		xml.required.set(true)
		html.required.set(true)
		// Define explicitamente onde o HTML vai ficar
		html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/html"))
	}

	val fileFilter = listOf(
		"**/R.class",
		"**/R\$*.class",
		"**/BuildConfig.*",
		"**/Manifest*.*",
		"**/*Test*.*",
		"android/**/*.*",
		"**/data/models/**",
		"**/di/**",
		"**/*_MembersInjector.class",
		"**/Dagger*Component.class",
		"**/Dagger*Component\$Builder.class",
		"**/*_Factory.*",
		"**/*_HiltModules*.*",
		"**/Hilt_*.*"
	)

	val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
		exclude(fileFilter)
	}

	val mainSrc = "${project.projectDir}/src/main/java"

	sourceDirectories.setFrom(files(mainSrc))
	classDirectories.setFrom(files(debugTree))

	// Procura tanto resultados de Unit Tests (.exec) quanto de Instrumentados (.ec)
	executionData.setFrom(fileTree(layout.buildDirectory.get()) {
		include(listOf("**/*.exec", "**/*.ec"))
	})

	// Imprime o link no final para você clicar
	doLast {
		val reportPath = layout.buildDirectory.dir("reports/jacoco/html/index.html").get().asFile
		println("----------------------------------------------------------")
		println("RELATÓRIO GERADO COM SUCESSO!")
		println("Copie e cole este caminho no seu navegador:")
		println("file:///$reportPath")
		println("----------------------------------------------------------")
	}
}