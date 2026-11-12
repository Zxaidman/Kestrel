// platform/ holds every Android-specific implementation (PROJECT_STRUCTURE.md §7–§9): the overlay,
// the session service, the input engine and its backend, Shizuku, storage, settings, display.
//
// It is an Android library and it depends on :core and nothing above it. Two walls are what make it
// a module rather than a folder, and both are compile errors rather than review comments:
//
// - **Nothing here can reach a screen.** :feature depends on this module, never the reverse, so a
//   service or an overlay that tried to import a Composable would not compile.
// - **Shizuku stops here.** It is an `implementation` dependency, so it is on this module's
//   classpath and on no screen's. A Composable that called Shizuku directly — which CLAUDE.md §4
//   forbids — would fail to resolve rather than slip through review.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.zxaidman.kestrel.platform"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        val javaVersion = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    testOptions {
        // The engine is tested on the JVM with a fake shell. The binder interface it implements
        // comes from the Android stub jar, whose methods throw unless told to return defaults.
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        // The privileged shell's binder interface is generated from AIDL, and it lives beside the
        // Shizuku code that serves it.
        aidl = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    }
}

dependencies {
    implementation(project(":core"))

    // Shizuku — the privilege ADR-INPUT-001's backend needs. Licence: Apache-2.0. Within the API 29
    // baseline. Optional at runtime by ADR-003. `implementation`, not `api`: it must never be
    // visible to :feature or :core (CLAUDE.md §4).
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // The folder the user chose, through the Storage Access Framework. Licence: Apache-2.0.
    // The full reasoning is in git history at app/build.gradle.kts, where this was first added.
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.core.ktx)

    // Settings and session state are observable by the screens that show them. Only the runtime —
    // state holders, no UI — so this module still cannot draw anything.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
