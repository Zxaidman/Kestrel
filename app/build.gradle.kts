// app/ is the Android assembly layer only (PROJECT_STRUCTURE.md §4): manifest, startup, wiring,
// navigation host, resources, APK configuration.
//
// Feature and domain logic must not accumulate here — see PROJECT_STRUCTURE.md §23.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.zxaidman.kestrel"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.zxaidman.kestrel"
        // Android 10 / API 29 is fixed by ADR-004. Do not raise without superseding that record.
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 44
        versionName = "0.0.44-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * One key for every build this repository produces.
     *
     * Android treats a signature as an application's identity, so two builds signed by different
     * keys are two different applications and the second cannot install over the first. Gradle's
     * default debug config generates a keystore on the machine that builds, and a CI runner is a
     * fresh machine every time — so every build was signed by a different key, and every install
     * was a reinstall that took the user's permissions and settings with it.
     *
     * This key is committed and its password is public. That is fine for builds people are testing
     * deliberately and unacceptable for builds people are trusting, so a release must use a key
     * that is not in this repository. `signing/README.md` carries the full reasoning and the steps.
     */
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("signing/kestrel-testing.p12")
            storePassword = "kestrel-testing"
            keyAlias = "kestrel-testing"
            keyPassword = "kestrel-testing"
        }
    }

    buildTypes {
        release {
            // Also the testing key for now: there is no release key, and an unsigned artifact
            // nobody can install is worse than a testing-signed one that says what it is.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        val javaVersion = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    }
}

dependencies {
    // The assembly layer depends on everything and nothing depends on it. Shizuku, storage and the
    // input engine arrive through :platform; the screens through :feature; the built-in layouts as
    // resources through :data. See settings.gradle.kts for the boundary each module enforces.
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":platform"))
    implementation(project(":feature"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
