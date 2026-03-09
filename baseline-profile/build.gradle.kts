plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.example.sysinfo.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testApplicationId = "com.example.sysinfo"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

dependencies {
    implementation(libs.benchmark.macro.junit4)
}

baselineProfile {
    useConnectedDevices = true
}
