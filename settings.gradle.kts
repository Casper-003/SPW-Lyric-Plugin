pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google {
            content {
                includeGroupByRegex("androidx\\..*")
            }
        }
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.Moriafly")
                includeGroup("com.github.Moriafly.spw-workshop-api")
            }
        }
    }
}

rootProject.name = "SPW-Lyric-Plugin"
