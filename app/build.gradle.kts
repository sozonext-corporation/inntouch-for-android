import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.sozonext.inntouch"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.sozonext.inntouch"
        minSdk = 24 // Android 7 (Release: 2016/08)
        targetSdk = 36
        versionCode = 25082201
        versionName = "v1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.cronet.embedded)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.barcode.scanning)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.okhttp)
    implementation(platform(libs.firebase.bom))
    implementation(files("libs/portsip_voip_sdk_for_android_v19.5.0.jar"))
}
