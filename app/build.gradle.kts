plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.sozonext.inntouch"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.sozonext.inntouch"
        minSdk = 30 // Android 12
        targetSdk = 35
        versionCode = 1
        versionName = "v1.0.0-debug"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            // applicationIdSuffix = ".debug"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

android.applicationVariants.all {
    outputs.all {
        val baseVariantOutputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
        baseVariantOutputImpl.outputFileName = "inntouch-latest-${buildType.name}.apk"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.cronet.embedded)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // implementation(libs.google.services)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.barcode.scanning)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.okhttp)
    implementation(files("libs/portsip_voip_sdk_for_android_v19.4.7.jar"))
    // implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}