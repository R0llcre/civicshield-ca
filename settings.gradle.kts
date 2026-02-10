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
  }
}

rootProject.name = "RegShieldCA"

// Android app lives under apps/android/app but is exposed as :app from repo root.
include(":app")
project(":app").projectDir = file("apps/android/app")

