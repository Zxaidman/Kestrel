// data/ is packaged configuration (PROJECT_STRUCTURE.md §10): the built-in layouts today, schemas and
// migrations as they arrive. Data, never code — there are no sources in this module, by design.
//
// A plain Java library so its resources land on the classpath exactly as they did inside :core,
// which is where `BuiltInLayouts` reads them from. :core does not depend on this module to compile —
// only its tests do, to check the shipped layouts — so the domain still cannot reach a file it does
// not own.
plugins {
    `java-library`
}

java {
    val javaVersion = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}
