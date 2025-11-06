pluginManagement {
	repositories {
		google {
			content {
				includeGroupByRegex("com\\.android.*")
				includeGroupByRegex("com\\.google.*")
				includeGroupByRegex("androidx.*")
			}
		}
		mavenCentral()
		gradlePluginPortal()
	}
}
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
		maven {
			url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
			// Autenticação (necessária para o SDK de Navegação)
			// Você DEVE criar um secret token no Mapbox com o scope 'downloads:read'
			// e colocá-lo no seu arquivo 'gradle.properties' (NÃO AQUI)
			// No arquivo gradle.properties, adicione: MAPBOX_DOWNLOADS_TOKEN=SEU_TOKEN_AQUI
			credentials {
				username = "mapbox"

				// ==================== A CORREÇÃO ESTÁ AQUI ====================
				// Esta linha lê o token do seu arquivo gradle.properties
				password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").getOrElse("")
				// ==========================================================
			}
		}
	}
}

rootProject.name = "TreeInspectorAndroid"
include(":app")