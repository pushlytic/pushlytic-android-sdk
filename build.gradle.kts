plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("org.jetbrains.dokka") version "1.8.10"
}

buildscript {
    dependencies {
        classpath(libs.protobuf.gradle.plugin)
        classpath(libs.dotenv.kotlin)
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}