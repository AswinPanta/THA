plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

import java.util.Properties
import java.io.FileInputStream

android {
    namespace = "com.treasurehuntadventure.tha"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
    }

    defaultConfig {
        applicationId = "com.treasurehuntadventure.tha"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // buildConfigField("String", "PEXELS_API_KEY", "\"${localProperties.getProperty("PEXELS_API_KEY")}\"")
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
    
    lint {
        baseline = file("lint-baseline.xml")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    
}

dependencies {
    // Firebase BOM - Keep existing stable version
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-database-ktx") // Realtime Database
    implementation("com.google.firebase:firebase-functions-ktx") // Cloud Functions

    // Core Android libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Location and Maps
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // Image loading and caching
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    // Networking
    implementation("com.android.volley:volley:1.2.1")
    
    // JSON and data handling
    implementation("com.google.code.gson:gson:2.11.0")
    
    // UI enhancements
    implementation("com.airbnb.android:lottie:6.6.2") // Animations
    implementation("de.hdodenhof:circleimageview:3.1.0") // Circular images
    
    // Performance and monitoring
    implementation("com.jakewharton.timber:timber:5.0.1") // Better logging
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14") // Memory leak detection (debug only)
    
    // Utilities
    implementation("org.apache.commons:commons-lang3:3.17.0")
    
    // AR dependencies
    implementation("com.google.ar:core:1.42.0")
    implementation("io.github.sceneview:arsceneview:1.2.1")

    // ML Kit for AI-based object recognition
    implementation("com.google.mlkit:object-detection:17.0.1")
    implementation("com.google.mlkit:image-labeling:17.0.8")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:camera:16.2.6")

    // ML Kit for chat features
    implementation("com.google.mlkit:language-id:17.0.5")
    implementation("com.google.mlkit:translate:17.0.2")

    // Google Cloud Translation API
    implementation("com.google.cloud:google-cloud-translate:2.20.0")

    // Content moderation and toxicity detection
    implementation("com.google.cloud:google-cloud-dlp:3.20.1")
    implementation("org.tensorflow:tensorflow-lite-task-text:0.4.4")

    // TensorFlow Lite for custom models
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")

    // Firebase ML and AI services for personalized challenges
    implementation("com.google.firebase:firebase-ml-common:22.1.2")
    implementation("com.google.firebase:firebase-appcheck-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")

    // Work Manager for background AI processing
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Advanced RecyclerView and UI components for challenges
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") // For analytics charts

    // Testing
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
}
