import org.gradle.api.JavaVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.repp.aquaticremineztools"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.repp.aquaticremineztools"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

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
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // ⚠️ HANYA 1 KALI (yang duplikat udah dihapus)
    androidResources {
        noCompress += "mp4"
    }
}

dependencies {

    // CORE ANDROID
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)

    // LIFECYCLE & VIEWMODEL
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // NAVIGATION
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // RECYCLERVIEW & VIEWPAGER
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)

    // ROOM DATABASE
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DATASTORE
    implementation(libs.androidx.datastore.preferences)

    // NETWORKING
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // IMAGE LOADING
    implementation(libs.coil)
    implementation(libs.coil.gif)

    // VIDEO PLAYER
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)

    // HILT
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // COROUTINES
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // QR CODE
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)

    // CHART
    implementation(libs.mpandroidchart)

    // PERMISSIONS
    implementation(libs.permissionx)

    // UI
    implementation(libs.shimmer)
    implementation(libs.lottie)
    implementation(libs.gif.drawable)

    // ⬇️ TAMBAHAN BARU: Google Play Services Location (GPS)
    implementation("com.google.android.gms:play-services-location:21.2.0")

    implementation(libs.androidx.security.crypto)

    // TESTING
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}