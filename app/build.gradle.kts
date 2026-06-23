// Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
// All rights reserved.
// This file is a part of LogiScanner application

import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    jacoco
}

android {
    namespace = "consulting.sw.logiscanner"
    compileSdk {
        version = release(36)
    }

    signingConfigs {
        create("release") {
            // file path provided by CI (or default local file name)
            val ksPath = System.getenv("SIGNING_STORE_FILE") ?: "release.keystore"
            storeFile = rootProject.file(ksPath)

            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "consulting.sw.logiscanner"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        val appVersionName: String by project
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // NOTE: SERVER_URL must include trailing slash
            buildConfigField("String", "SERVER_URL", "\"http://192.168.11.140:8080/api/\"")
        }
        release {
            // NOTE: SERVER_URL must include trailing slash
            buildConfigField("String", "SERVER_URL", "\"https://lb.gtc.express/api/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // IMPORTANT: make Gradle sign the release APK
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.datastore.preferences)
    implementation(libs.androidx.appcompat)
    implementation(libs.signalr)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val coverageExclusions = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*JsonAdapter.*",
    "**/*JsonAdapter$*.*"
)

val ciJacocoXmlReport = layout.buildDirectory.file("tmp/coverage/jacoco-debug-unit-test.xml")
val ciCoberturaReport = layout.buildDirectory.file("reports/coverage/cobertura.xml")
val jacocoToCoberturaScript = rootProject.layout.projectDirectory.file("tools/jacoco_to_cobertura.py")
val pythonExecutable = providers.gradleProperty("pythonExecutable")
    .orElse(
        providers.environmentVariable("PYTHON").orElse(
            if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
                "python"
            } else {
                "python3"
            }
        )
    )

val ciCoverageIntermediateReport = tasks.register<JacocoReport>("ciCoverageIntermediateReport") {
    description = "Generates a temporary JaCoCo XML report for CI coverage conversion."

    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        xml.outputLocation.set(ciJacocoXmlReport)
        html.required.set(false)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(
            fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
                exclude(coverageExclusions)
            },
            fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
                exclude(coverageExclusions)
            },
            fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
                exclude(coverageExclusions)
            }
        )
    )
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("jacoco/testDebugUnitTest.exec")
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        }
    )
}

val deleteCiCoverageIntermediateReport = tasks.register<Delete>("deleteCiCoverageIntermediateReport") {
    delete(ciJacocoXmlReport)
}

tasks.register<Exec>("ciCoberturaCoverage") {
    group = "verification"
    description = "Generates the Cobertura coverage report consumed by GitHub Code Quality."

    dependsOn(ciCoverageIntermediateReport)
    finalizedBy(deleteCiCoverageIntermediateReport)

    inputs.file(ciJacocoXmlReport)
    inputs.file(jacocoToCoberturaScript)
    outputs.file(ciCoberturaReport)

    workingDir(rootProject.projectDir)
    commandLine(
        pythonExecutable.get(),
        jacocoToCoberturaScript.asFile.absolutePath,
        ciJacocoXmlReport.get().asFile.absolutePath,
        ciCoberturaReport.get().asFile.absolutePath,
        "--source-root",
        project.file("src/main/java").absolutePath,
        "--project-root",
        rootProject.projectDir.absolutePath
    )
}
