import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.proto
import org.gradle.api.tasks.PathSensitivity

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
}

private val androidAbis = listOf("armeabi-v7a", "arm64-v8a", "x86_64")

android {
    namespace = "com.ivanovsky.passnotes.keepassrs"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    kotlin {
        compilerOptions {
            jvmToolchain(21)
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDir("src/main/kotlin")
            jniLibs.srcDir("src/main/jniLibs")
            proto {
                srcDir("../keepass-rs/proto")
            }
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }
                id("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

fun registerBuildRustJniTask(taskName: String, profile: String): TaskProvider<Exec> {
    val outputDirectory = layout.projectDirectory.dir("src/main/jniLibs")
    val rustInputs = rootProject.fileTree("keepass-rs") {
        include("Cargo.lock")
        include("Cargo.toml")
        include("build.rs")
        include("proto/**/*.proto")
        include("src/**/*.rs")
    }

    return tasks.register<Exec>(taskName) {
        group = "rust"
        description = "Builds $profile Android JNI binaries for keepass-rs."

        inputs.file(rootProject.file("keepass-rs-android/build-native-libraries.sh"))
        inputs.files(rustInputs)
            .withPropertyName("rustInputs")
            .withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.files(androidAbis.map { abi -> outputDirectory.file("$abi/libkeepass_rs.so") })

        if (profile == "debug") {
            commandLine(rootProject.file("keepass-rs-android/build-native-libraries.sh"), "--debug")
        } else {
            commandLine(rootProject.file("keepass-rs-android/build-native-libraries.sh"))
        }
    }
}

val buildDebugRustJni = registerBuildRustJniTask("buildDebugRustJni", "debug")
val buildReleaseRustJni = registerBuildRustJniTask("buildReleaseRustJni", "release")

tasks.register("buildRustJni") {
    group = "rust"
    description = "Builds Android JNI binaries for keepass-rs. Use -PrustProfile=debug for debug."

    val profile = providers.gradleProperty("rustProfile").orElse("release").get()
    dependsOn(if (profile == "debug") buildDebugRustJni else buildReleaseRustJni)

    doLast {
        logger.lifecycle("Built keepass-rs Android JNI binaries in $profile mode.")
    }
}

tasks.matching { it.name == "mergeDebugJniLibFolders" }.configureEach {
    dependsOn(buildDebugRustJni)
}

tasks.matching { it.name == "mergeReleaseJniLibFolders" }.configureEach {
    dependsOn(buildReleaseRustJni)
}

dependencies {
    api(libs.protobuf.javalite)
    api(libs.protobuf.kotlin.lite)

    // Arrow
    implementation(libs.arrowCore)
    implementation(libs.arrowCoroutines)
}
