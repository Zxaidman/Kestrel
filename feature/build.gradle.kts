// feature/ is the user-facing functionality (PROJECT_STRUCTURE.md §6): the screens, the editor, the
// test ground, the diagnostics, and the theme they share.
//
// It depends on :core and on :platform, which is the direction PROJECT_STRUCTURE.md §20 allows —
// screens use platform services, platform services never use screens. Shizuku is not on this
// module's classpath at all (see platform/build.gradle.kts), so no screen can call it.
//
// One module for every feature, deliberately (PROJECT_STRUCTURE.md §24): splitting each screen into
// its own module is worth doing when the features stop sharing the theme and the build time says so.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.zxaidman.kestrel.feature"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
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
    implementation(project(":core"))
    implementation(project(":platform"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
