package com.iilmchat.chessproblem.buildlogic

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

/**
 * Единая настройка detekt и detekt-formatting (ktlint) для всех модулей.
 * Конфигурация и baseline лежат в корне, чтобы правила были одинаковыми везде.
 */
class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.gitlab.arturbosch.detekt")

            val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
            val formatting = catalog.findLibrary("detekt-formatting").orElseThrow {
                IllegalStateException("detekt-formatting is missing from the version catalog")
            }
            dependencies.add("detektPlugins", formatting.get())

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig = true
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                baseline = rootProject.file("config/detekt/detekt-baseline.xml")
                parallel = true
            }
        }
    }
}
