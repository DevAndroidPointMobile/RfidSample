plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "device.apps.rfidsamplev2"
    compileSdk = 34

    defaultConfig {
        applicationId = "device.apps.rfidsamplev2"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to "device.sdk.jar")))
    implementation(fileTree(mapOf("dir" to "libs", "include" to "ex.dev.sdk.rf88.jar")))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}