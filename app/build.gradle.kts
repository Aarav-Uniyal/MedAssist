plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Reads app/google-services.json (which you download from the Firebase console; it is git-ignored).
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.medassist"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.medassist"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Left off for the college build. Turn on (with ProGuard rules) before any real release.
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Lets plain JVM tests touch Android classes without crashing (they return defaults).
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Firebase. The BoM picks matching versions for every Firebase library, so none is listed below.
    // Kotlin projects use the main modules (the old "-ktx" modules are no longer released).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    // Phase 4: Gemini through Firebase AI Logic (free "Gemini Developer API" tier, Spark plan),
    // Remote Config (free) to change the model without a new app release, and App Check.
    implementation(libs.firebase.ai)
    implementation(libs.firebase.config)
    implementation(libs.firebase.appcheck)
    // Phase 5: local PDF text extraction; no OCR service, Cloud Function, or paid storage needed.
    implementation(libs.pdfbox.android)
    // Debug builds prove themselves with a debug token; release builds with Play Integrity.
    // Each build type has its own AppCheckInstaller.kt (src/debug and src/release).
    debugImplementation(libs.firebase.appcheck.debug)
    releaseImplementation(libs.firebase.appcheck.playintegrity)
    // Adds Task.await() so Firebase calls can be used from coroutines.
    implementation(libs.kotlinx.coroutines.play.services)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
