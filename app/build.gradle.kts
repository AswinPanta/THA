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
    
    // Testing
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
}
