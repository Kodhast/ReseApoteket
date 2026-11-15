pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Komponentbiblioteket android-ui (skill android:android-gradle-logic → GitHub Packages).
        maven("https://maven.pkg.github.com/Kodhast/android-ui") {
            name = "androidUi"
            credentials {
                username = providers.environmentVariable("GPR_USER")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.environmentVariable("GPR_TOKEN")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
            content { includeGroup("se.kodhast.ui") }
        }
    }
}

rootProject.name = "ReseApoteket"
include(":core", ":app")
