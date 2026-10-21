plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.example.calification"
  compileSdk {
    version = release(37)
  }

  defaultConfig {
    applicationId = "com.example.calification"
    minSdk = 27
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      optimization {
        enable = true
        packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    viewBinding = true
  }
}

dependencies {
  implementation(platform(libs.supabase.bom))
  implementation(libs.supabase.postgrest)
  implementation(libs.ktor.client.android)
  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.core.ktx)
  implementation(libs.material)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
}
