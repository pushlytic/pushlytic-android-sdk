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
        maven("https://jitpack.io")
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.protobuf") {
                useModule("com.google.protobuf:protobuf-gradle-plugin:0.9.1")
            }
            if (requested.id.id == "org.gradle.toolchains.foojay-resolver-convention") {
                useModule("org.gradle.toolchains:foojay-resolver:0.7.0")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

gradle.beforeProject {
    val javaToolchainVersion = 11
    if (this != null) {
        extensions.findByType<JavaPluginExtension>()?.apply {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(javaToolchainVersion))
                vendor.set(JvmVendorSpec.ADOPTIUM)
            }
        }
    }
}

include(":example")
include(":sdk")