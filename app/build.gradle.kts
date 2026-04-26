plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.danzucker.networklocationtracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.danzucker.networklocationtracker"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    lint {
        // NonNullableMutableLiveDataDetector crashes with Kotlin 2.0 analysis API on AGP 8.7.x.
        // Safe to disable — we don't use LiveData anywhere in this project.
        disable += "NullSafeMutableLiveData"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.google.android.gms.play.services.location)

    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
    implementation(libs.androidx.room.runtime.android)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.coroutines)
    coreLibraryDesugaring(libs.desugar.jdk.libs)


    testImplementation(libs.bundles.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.bundles.coroutines.test)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit5)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}