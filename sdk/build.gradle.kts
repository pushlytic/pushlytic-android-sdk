import io.github.cdimascio.dotenv.Dotenv

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.protobuf")
    id("maven-publish")
    id("org.jetbrains.dokka")
    id("signing")
}

val pushlyticVersion = "0.1.2"
val dotenv = Dotenv.configure().ignoreIfMissing().load()

if (dotenv["OSSRH_USERNAME"].isNullOrEmpty() || dotenv["OSSRH_PASSWORD"].isNullOrEmpty()) {
    throw GradleException("OSSRH_USERNAME and OSSRH_PASSWORD must be set in the .env file.")
}
if (dotenv["GPG_KEY_ID"].isNullOrEmpty() || dotenv["GPG_PASSPHRASE"].isNullOrEmpty()) {
    throw GradleException("GPG_KEY_ID and GPG_PASSPHRASE must be set in the .env file.")
}

android {
    namespace = "com.pushlytic.sdk"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "VERSION_NAME", "\"$pushlyticVersion\"")
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    buildTypes {
        debug {
            buildConfigField("boolean", "DEBUG", "true")
        }
        release {
            buildConfigField("boolean", "DEBUG", "false")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    lint {
        targetSdk = 35
        warningsAsErrors = true
    }
    packaging {
        resources {
            excludes += "google/protobuf/*.proto"
            pickFirsts += "google/protobuf/field_mask.proto"
            pickFirsts += "META-INF/INDEX.LIST"
            pickFirsts += "META-INF/io.netty.versions.properties"
            pickFirsts += "META-INF/DEPENDENCIES"
            pickFirsts += "META-INF/gradle/incremental.annotation.processors"
            pickFirsts += "META-INF/NOTICE"
            pickFirsts += "META-INF/LICENSE"
            pickFirsts += "META-INF/AL2.0"
            pickFirsts += "META-INF/LGPL2.1"
        }
    }
    sourceSets {
        getByName("main") {
            resources.srcDirs("src/main/proto")
            java.srcDirs(
                layout.buildDirectory.dir("generated/source/proto/main/java"),
                layout.buildDirectory.dir("generated/source/proto/main/grpc"),
                layout.buildDirectory.dir("generated/source/proto/main/kotlin"),
                layout.buildDirectory.dir("generated/source/proto/main/grpckt")
            )
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Compose dependencies
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    // Protobuf and gRPC dependencies
    implementation("io.grpc:grpc-kotlin-stub:1.3.0") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    implementation("io.grpc:grpc-protobuf-lite:1.58.0") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    implementation("io.grpc:grpc-okhttp:1.58.0") {
        exclude(group = "org.json", module = "json")
    }
    implementation(libs.grpc.android)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.gson)

    // Testing dependencies
    testImplementation(libs.androidx.runner)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric.v4141)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.core)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.0"
    }

    generatedFilesBaseDir = layout.buildDirectory.dir("generated/source/proto").get().asFile.path

    plugins {
        create("java") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.58.0"
        }

        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.58.0"
        }

        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.3.0:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
            task.plugins {
                create("grpc") {
                    option("lite")
                }
                create("grpckt") {
                    option("lite")
                }
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn("generateReleaseProto")
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn("generateReleaseProto")
}

tasks.register("generateProto") {
    dependsOn("generateReleaseProto")
}

tasks.named("build") {
    dependsOn("generateProto")
}

tasks.register<Jar>("androidSourcesJar") {
    group = "publishing"
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml") {
    outputDirectory.set(layout.buildDirectory.dir("docs").get().asFile)
    dokkaSourceSets {
        named("main") {
            noAndroidSdkLink.set(false)
        }
    }
}

tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaJavadoc") {
    outputDirectory.set(layout.buildDirectory.dir("javadocs").get().asFile)
    dokkaSourceSets {
        named("main") {
            noAndroidSdkLink.set(false)
        }
    }
}

tasks.register<Jar>("androidJavadocsJar") {
    dependsOn("dokkaJavadoc")
    group = "publishing"
    archiveClassifier.set("javadoc")
    from(layout.buildDirectory.dir("javadocs").get().asFile)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.pushlytic"
                artifactId = "sdk"
                version = pushlyticVersion

                artifact(tasks.named("androidJavadocsJar").get()) {
                    classifier = "javadoc"
                }

                pom {
                    packaging = "aar"
                    name.set("Pushlytic")
                    description.set("Android SDK for real-time communication using Pushlytic.")
                    url.set("https://github.com/pushlytic/pushlytic-android-sdk")
                    inceptionYear.set("2024")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("pushlytic")
                            name.set("Pushlytic Team")
                            email.set("support@pushlytic.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/pushlytic/pushlytic-android-sdk.git")
                        developerConnection.set("scm:git:ssh://github.com/pushlytic/pushlytic-android-sdk.git")
                        url.set("https://github.com/pushlytic/pushlytic-android-sdk")
                    }
                }
            }
        }
        repositories {
            maven {
                url = if (dotenv["IS_SNAPSHOT"] == "true") {
                    uri("https://oss.sonatype.org/content/repositories/snapshots/")
                } else {
                    uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                }
                credentials {
                    username = dotenv["OSSRH_USERNAME"] ?: ""
                    password = dotenv["OSSRH_PASSWORD"] ?: ""
                }
            }
        }
    }

    signing {
        val keyId = dotenv["GPG_KEY_ID"] ?: throw GradleException("GPG_KEY_ID is missing in .env")
        val privateKey = dotenv["GPG_SIGNING_KEY"] ?: throw GradleException("GPG_SIGNING_KEY is missing in .env")
        val password = dotenv["GPG_PASSPHRASE"] ?: throw GradleException("GPG_PASSPHRASE is missing in .env")

        useInMemoryPgpKeys(keyId, privateKey.replace("\\n", "\n"), password)
        sign(publishing.publications["release"])
    }

    tasks.named("generateMetadataFileForReleasePublication") {
        dependsOn(tasks.named("androidSourcesJar"))
        dependsOn(tasks.named("androidJavadocsJar"))
    }
}

configurations {
    all {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
        exclude(group = "org.json", module = "json")
        resolutionStrategy {
            force("com.google.protobuf:protobuf-javalite:3.24.0")
            force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            force("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
        }
    }
}