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
    }
}

rootProject.name = "Kestrel"

// Gradle module count is kept deliberately small (PROJECT_STRUCTURE.md §24).
//
// :core is a plain Kotlin/JVM module with no Android dependency on its classpath. That is what
// enforces the rule in PROJECT_STRUCTURE.md §21 — core cannot depend on Compose, Android UI, or
// Shizuku, because none of them can resolve there. The boundary is checked by the compiler rather
// than by review.
//
// The five modules PROJECT_STRUCTURE.md §26 recommends to start with, and no more (§24). Each one is a
// boundary the compiler checks rather than a folder:
//
// - :core     — pure domain logic, Kotlin/JVM. Cannot see Android, Compose or Shizuku.
// - :data     — packaged configuration. Resources only, no code.
// - :platform — every Android-specific implementation. Cannot see a screen; Shizuku stops here.
// - :feature  — the screens. Sees :platform's services, never Shizuku.
// - :app      — the activity, the navigation and the manifest that assemble them.
include(":app")
include(":core")
include(":data")
include(":platform")
include(":feature")

// EXPERIMENTAL — Phase 0 feasibility harness. Separate APK, not a dependency of :app.
// Remove this module once Phase 0 concludes and ADR-INPUT-001 is decided.
include(":tools:phase0")
