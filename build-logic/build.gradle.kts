plugins {
    `kotlin-dsl`
}

group = "com.iilmchat.chessproblem.buildlogic"

dependencies {
    implementation("com.android.tools.build:gradle:9.4.1")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.10")
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "chessproblem.android.application"
            implementationClass = "com.iilmchat.chessproblem.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "chessproblem.android.library"
            implementationClass = "com.iilmchat.chessproblem.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "chessproblem.android.compose"
            implementationClass = "com.iilmchat.chessproblem.buildlogic.AndroidComposeConventionPlugin"
        }
    }
}
