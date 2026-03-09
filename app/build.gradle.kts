plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.room)
    alias(libs.plugins.baselineprofile)
}

baselineProfile {
    warnings {
        maxAgpVersion = false
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.example.sysinfo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sysinfo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = false
        buildConfig = false
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.material)
    implementation(libs.swiperefreshlayout)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    // WorkManager
    implementation(libs.work.runtime.ktx)
    // 如果之前未引入 Room，现在也加上（用于存储 CellTower 等）
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // Baseline Profile：安装时预编译关键路径，提升启动与运行性能
    implementation(libs.profileinstaller)

    baselineProfile(project(":baseline-profile"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}