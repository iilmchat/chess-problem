import java.io.File
import java.util.Properties

plugins {
    id("chessproblem.android.application")
    id("chessproblem.android.compose")
    id("chessproblem.detekt")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.iilmchat.chessproblem"

    defaultConfig {
        applicationId = "com.iilmchat.chessproblem"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                listOf("storeFile", "storePassword", "keyAlias", "keyPassword").forEach { key ->
                    require(!keystoreProperties.getProperty(key).isNullOrBlank()) {
                        "keystore.properties is missing '$key'"
                    }
                }
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    buildTypes {
        release {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

// Публичный релиз: проверяем артефакты сразу после сборки AAB/APK.
val releaseMappingFile = layout.buildDirectory.file("outputs/mapping/release/mapping.txt")
val releaseManifestFile = layout.buildDirectory.file(
    "intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml",
)

val verifyReleaseArtifacts = tasks.register("verifyReleaseArtifacts") {
    group = "verification"
    description = "Проверяет mapping.txt и разрешения в merged manifest release-сборки"
    dependsOn("assembleRelease")

    // Пути фиксируются на конфигурации: configuration cache не умеет сериализовать
    // ссылки на объекты build-скрипта внутри action.
    val mappingPath = releaseMappingFile.get().asFile.absolutePath
    val manifestPath = releaseManifestFile.get().asFile.absolutePath

    doLast {
        val mapping = File(mappingPath)
        require(mapping.exists() && mapping.length() > 0L) {
            "mapping.txt не создан R8 при assembleRelease/bundleRelease: ${mapping.path}"
        }

        val manifest = File(manifestPath)
        require(manifest.exists()) { "Merged release-манифест не найден: ${manifest.path}" }
        val xml = manifest.readText()
        require(!xml.contains("android.permission.INTERNET", ignoreCase = true)) {
            "В release-манифесте присутствует INTERNET"
        }
        require(!xml.contains("AD_ID", ignoreCase = true)) {
            "В release-манифесте присутствует AD_ID"
        }
        require(!Regex("""android:debuggable\s*=\s*"true"""").containsMatchIn(xml)) {
            "В release-манифесте android:debuggable=\"true\""
        }
        require(Regex("""android:allowBackup\s*=\s*"false"""").containsMatchIn(xml)) {
            "В release-манифесте отсутствует android:allowBackup=\"false\""
        }

        logger.lifecycle("Release-артефакты корректны: mapping.txt и манифест без INTERNET/AD_ID/debuggable")
    }
}

// AGP 9 регистрирует bundle-задачи лениво, поэтому подключаемся через configureEach.
tasks.configureEach {
    if (name == "bundleRelease") {
        finalizedBy(verifyReleaseArtifacts)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
