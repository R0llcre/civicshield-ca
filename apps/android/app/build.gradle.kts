plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.google.dagger.hilt.android")
  kotlin("kapt")
}

val regshieldStagingBaseUrl: String =
  (project.findProperty("REGSHIELD_STAGING_BASE_URL") as String?)
    ?: "https://ca-regshieldca-web-stg.blackhill-0c2251ca.westus2.azurecontainerapps.io"

val regshieldProdBaseUrl: String =
  (project.findProperty("REGSHIELD_PROD_BASE_URL") as String?)
    ?: "https://regshieldca.invalid"

val regshieldVersionCode: Int =
  (project.findProperty("REGSHIELD_VERSION_CODE") as String?)?.toIntOrNull()
    ?: 1

val regshieldVersionName: String =
  (project.findProperty("REGSHIELD_VERSION_NAME") as String?)
    ?: "0.1.0"

val markwonVersion = "4.6.2"

val internalKeystorePath = System.getenv("REGSHIELD_INTERNAL_KEYSTORE_PATH")
val internalKeystorePassword = System.getenv("REGSHIELD_INTERNAL_KEYSTORE_PASSWORD")
val internalKeyAlias = System.getenv("REGSHIELD_INTERNAL_KEY_ALIAS")
val internalKeyPassword = System.getenv("REGSHIELD_INTERNAL_KEY_PASSWORD")
val hasInternalSigning =
  !internalKeystorePath.isNullOrBlank() &&
    !internalKeystorePassword.isNullOrBlank() &&
    !internalKeyAlias.isNullOrBlank() &&
    !internalKeyPassword.isNullOrBlank()

android {
  namespace = "org.regshield.ca"
  compileSdk = 34

  defaultConfig {
    applicationId = "org.regshield.ca"
    minSdk = 24
    targetSdk = 34
    versionCode = regshieldVersionCode
    versionName = regshieldVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables.useSupportLibrary = true
  }

  flavorDimensions += "env"
  productFlavors {
    create("dev") {
      dimension = "env"
      applicationIdSuffix = ".dev"
      versionNameSuffix = "-dev"
      buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000\"")
    }
    create("staging") {
      dimension = "env"
      applicationIdSuffix = ".staging"
      versionNameSuffix = "-staging"
      buildConfigField("String", "API_BASE_URL", "\"$regshieldStagingBaseUrl\"")
    }
    create("prod") {
      dimension = "env"
      buildConfigField("String", "API_BASE_URL", "\"$regshieldProdBaseUrl\"")
    }
  }

  signingConfigs {
    create("internal") {
      if (hasInternalSigning) {
        storeFile = file(internalKeystorePath!!)
        storePassword = internalKeystorePassword
        keyAlias = internalKeyAlias
        keyPassword = internalKeyPassword
      }
    }
  }

  buildTypes {
    debug {
      isMinifyEnabled = false
    }
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Default to debug signing for local builds. CI/internal releases inject a stable signing key via env.
      signingConfig =
        if (hasInternalSigning) signingConfigs.getByName("internal")
        else signingConfigs.getByName("debug")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.8"
  }

  packaging {
    resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
  }
}

kapt {
  correctErrorTypes = true
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2024.02.02"))
  androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.02"))

  implementation("androidx.core:core-ktx:1.12.0")
  implementation("androidx.activity:activity-compose:1.8.2")

  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material3:material3-window-size-class")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.compose.animation:animation")
  implementation("com.google.android.material:material:1.11.0")

  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")

  implementation("androidx.navigation:navigation-compose:2.7.6")

  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

  // Networking (no logging; avoid PII leaks)
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
  implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")

  // DI
  implementation("com.google.dagger:hilt-android:2.51.1")
  kapt("com.google.dagger:hilt-compiler:2.51.1")
  implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

  // Local vault storage
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  kapt("androidx.room:room-compiler:2.6.1")

  // Local preferences (Shield)
  implementation("androidx.datastore:datastore-preferences:1.1.1")

  // Markdown rendering (Ask)
  implementation("io.noties.markwon:core:$markwonVersion")
  implementation("io.noties.markwon:ext-strikethrough:$markwonVersion")
  implementation("io.noties.markwon:ext-tasklist:$markwonVersion")
  implementation("io.noties.markwon:ext-tables:$markwonVersion")

  // Tests
  testImplementation("junit:junit:4.13.2")
  testImplementation("com.google.truth:truth:1.4.2")
  testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

  androidTestImplementation("androidx.compose.ui:ui-test-junit4-android")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test:runner:1.5.2")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
