// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
// 根目录 build.gradle.kts
buildscript {
    repositories {
        google()
        jcenter() // 或 mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
    }
}
