import org.gradle.api.tasks.Sync

plugins {
    id("chessproblem.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Content cutover (KI-008): `content/puzzles/*.json` stays the single source of
// truth; a Sync task copies the production packs into a generated assets
// directory before every build. Static assets under src/main/assets were the
// v0.2.0 demo path and are no longer used.
val puzzlePacksDir = layout.buildDirectory.dir("generated/puzzlePacks")

val syncPuzzlePacks by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.dir("content/puzzles")) {
        include("*.json")
    }
    into(puzzlePacksDir.map { it.dir("content") })
}

android {
    namespace = "com.iilmchat.chessproblem.data"
}

androidComponents {
    onVariants { variant ->
        // AGP 9 forbids Provider assets.srcDir(...) on the legacy sourceSets API;
        // register the generated packs dir through the Variant API instead.
        variant.sources.assets?.addStaticSourceDirectory("build/generated/puzzlePacks")
    }
}

tasks.named("preBuild") {
    dependsOn(syncPuzzlePacks)
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
