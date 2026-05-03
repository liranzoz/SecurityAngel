import java.io.FileInputStream
import java.util.Properties


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.securityangel"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.securityangel"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        val geminiKey = localProperties.getProperty("GEMINI_API_KEY") ?: "".replace("\"", "")
        val virusTotalKey = localProperties.getProperty("VIRUSTOTAL_API_KEY") ?: "".replace("\"", "")
        val secretKey = localProperties.getProperty("SECRET_KEY") ?: "".replace("\"", "")
        val algorithm = localProperties.getProperty("ALGORITHM") ?: "AES/CBC/PKCS5Padding".replace("\"", "")

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
        buildConfigField("String", "VIRUSTOTAL_API_KEY", "\"$virusTotalKey\"")
        buildConfigField("String", "SECRET_KEY", "\"$secretKey\"")
        buildConfigField("String", "ALGORITHM", "\"$algorithm\"")


    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
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
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("com.google.firebase:firebase-messaging:23.4.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.credentials:credentials:1.2.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.0")
    implementation("androidx.autofill:autofill:1.1.0")
}