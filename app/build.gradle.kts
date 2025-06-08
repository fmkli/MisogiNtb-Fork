plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.misogintb"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.misogintb"
        minSdk = 28
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

}
val roomVersion = "2.6.1"
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose) // 使用 libs.versions.toml 中的版本

    implementation(platform(libs.androidx.compose.bom)) // 使用 BOM 别名
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.runtime) // 确保在 libs.versions.toml 中定义了 runtime (不带版本，由BOM控制)
    // 例如: androidx-compose-runtime = { group = "androidx.compose.runtime", name = "runtime" }
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.foundation) // 确保在 libs.versions.toml 中定义了 foundation
    implementation(libs.androidx.material3)         // 确保在 libs.versions.toml 中定义了 material3

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // 如果这个不在 libs 中，可以保留

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    implementation("androidx.room:room-runtime:$roomVersion")
    // 注释或删除 kapt 依赖
    // kapt("androidx.room:room-compiler:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion") // 使用 ksp 替代 kapt

    implementation("androidx.room:room-ktx:$roomVersion")

}

