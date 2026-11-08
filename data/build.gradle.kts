plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.mmomo.dailyjournal.data"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Robolectric needs merged resources and assets (prompts.json).
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
                // Robolectric reads FileDescriptor internals, which recent JDKs only allow when exported and opened.
                it.jvmArgs(
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                )
            }
        }
    }
}

room {
    // Exported schemas are committed so future migrations can be tested (DATA-REL-4).
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":domain"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)

    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.room.testing)
    // Robolectric needs its JUnit 4 runner; JUnit 4 tests run on the JUnit Platform through the Vintage engine.
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
}
