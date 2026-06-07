import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileWriter
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.play.publisher)
    alias(libs.plugins.spotless)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.room)
}

val versionMajor = 1
val versionMinor = 17
val versionPatch = 0

val formattedVersionCode = (versionMajor * 10000 + versionMinor * 100 + versionPatch).toString()
val formattedVersionName = "$versionMajor.$versionMinor.$versionPatch"

val devPropertiesFile = file("../project.properties")
val ciPropertiesFile = file("../ci.properties")

val propertyKeystorePassword = "KEYSTORE_PASSWORD"
val propertyKeystoreAlias = "KEYSTORE_ALIAS"

val debugKeyPath = "../keys/debug.keystore"
val releaseKeyPath = "../keys/release.keystore"
val publicKeyPath = "../keys/public.keystore"
val googleCloudKeyPath = "../keys/google-cloud-key.json"

val publicKeyPassword = "abc123"
val publicKeyAlias = "testalias"

val customProps = Properties().apply {
    if (devPropertiesFile.exists()) {
        FileInputStream(devPropertiesFile).use(::load)
    }
    if (ciPropertiesFile.exists()) {
        FileInputStream(ciPropertiesFile).use(::load)
    }
}

fun getCustomProperty(key: String): String {
    return customProps.getProperty(key) ?: System.getenv(key).orEmpty()
}

val generateVersionPropertiesFile by tasks.registering {
    description = "Generates version.properties for F-Droid."

    doLast {
        val propertyVersionCode = "versionCode"
        val propertyVersionName = "versionName"
        val versionPropertiesFile = file("../version.properties")

        val versions = Properties()
        if (versionPropertiesFile.exists()) {
            FileInputStream(versionPropertiesFile).use(versions::load)
        }

        if (
            versions[propertyVersionCode] != formattedVersionCode ||
            versions[propertyVersionName] != formattedVersionName
        ) {
            project.logger.lifecycle("Updating file: version.properties")

            versions.setProperty(propertyVersionCode, formattedVersionCode)
            versions.setProperty(propertyVersionName, formattedVersionName)

            BufferedWriter(FileWriter(versionPropertiesFile)).use {
                versions.store(it, "The content of this file is generated automatically for F-Droid")
            }
        }
    }
}

android {
    namespace = "com.ivanovsky.passnotes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ivanovsky.passnotes"
        minSdk = 26
        targetSdk = 36
        versionCode = formattedVersionCode.toInt()
        versionName = formattedVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        project.logger.lifecycle("VersionName: $versionName")
        project.logger.lifecycle("VersionCode: $versionCode")
    }

    buildFeatures {
        compose = true
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }

    kotlin {
        compilerOptions {
            jvmToolchain(21)
        }
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    sourceSets {
        getByName("main") {
            java.srcDir("src/main/kotlin")
        }
        getByName("androidTest") {
            java.srcDir("src/androidTest/kotlin")
            assets.srcDir("schemas")
        }
        getByName("test") {
            java.srcDir("src/test/kotlin")
        }
    }

    signingConfigs {
        getByName("debug") {
            if (file(debugKeyPath).exists()) {
                storeFile = file(debugKeyPath)
            }
        }
        create("release") {
            if (file(releaseKeyPath).exists()) {
                storeFile = file(releaseKeyPath)
                storePassword = getCustomProperty(propertyKeystorePassword)
                keyAlias = getCustomProperty(propertyKeystoreAlias)
                keyPassword = getCustomProperty(propertyKeystorePassword)
            } else {
                // This signing config is used for CI builds on forked repos.
                storeFile = file(publicKeyPath)
                storePassword = publicKeyPassword
                keyAlias = publicKeyAlias
                keyPassword = publicKeyPassword
                project.logger.lifecycle("Configure release build with public keystore")
            }
        }
    }

    flavorDimensions += "default"
    productFlavors {
        create("fdroid") {
            dimension = "default"
        }
        create("gplay") {
            dimension = "default"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            versionNameSuffix = "d"
            signingConfig = signingConfigs.getByName("debug")
        }
        create("automation") {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".automation"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "plugin.properties"
        }
    }
}

tasks.named("preBuild") {
    dependsOn(generateVersionPropertiesFile)
}

play {
    serviceAccountCredentials.set(file(googleCloudKeyPath))
    track.set("internal")
    releaseStatus.set(ReleaseStatus.DRAFT)
    defaultToAppBundles.set(true)
    enabled.set(true)
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint(libs.versions.ktlint.get()).setEditorConfigPath("$projectDir/../.editorconfig")
        indentWithSpaces()
        trimTrailingWhitespace()
    }

    java {
        target("**/*.java")
        googleJavaFormat().aosp()
        removeUnusedImports()
        trimTrailingWhitespace()
        indentWithSpaces()
    }

    format("misc") {
        target("**/*.gradle", "**/*.gradle.kts", "**/*.md", "**/.gitignore")
        indentWithSpaces()
        trimTrailingWhitespace()
    }

    format("xml") {
        target("**/*.xml")
        targetExclude("**/strings.xml")
        indentWithSpaces()
        trimTrailingWhitespace()
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlin.reflect)

    // DI
    implementation(libs.koin.android)
    implementation(libs.koin.core)

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    androidTestImplementation(libs.room.testing)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.preference.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.apache.commons.lang3) // TODO: Remove
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.android)

    // Network
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.sardine.android)

    // Git
    implementation(libs.jgit)
    implementation(libs.jgit.ssh.jsch)

    // Routing
    implementation(libs.cicerone)

    // Logging
    implementation(libs.timber)
    implementation(libs.treessence)

    // KeePass
    implementation(libs.kotpass)
    implementation(libs.keepass.tree.diff)
    implementation(libs.keepass.tree.builder)

    // Arrow
    implementation(libs.arrowCore)
    implementation(libs.arrowCoroutines)

    // Other
    implementation(libs.fzf4j)
    implementation(libs.totp.kt)
}
