plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.roleplayai.chatbot"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.roleplayai.chatbot"
        minSdk = 24
        targetSdk = 34
        versionCode = 56
        versionName = "3.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // NDK désactivé temporairement
        // ndk {
        //     // Compiler seulement pour ARM 64-bit (téléphones modernes)
        //     abiFilters += listOf("arm64-v8a")
        // }
        //
        // externalNativeBuild {
        //     cmake {
        //         arguments += listOf(
        //             "-DANDROID_STL=c++_shared",
        //             "-DANDROID_PLATFORM=android-24"
        //         )
        //         cppFlags += listOf("-std=c++17", "-frtti", "-fexceptions")
        //     }
        // }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    // NDK configuration for llama.cpp (désactivé temporairement - wrapper JNI prêt)
    // TODO: Activer après intégration complète de llama.cpp
    // ndkVersion = "26.1.10909125"
    //
    // externalNativeBuild {
    //     cmake {
    //         path = file("../CMakeLists.txt")
    //         version = "3.22.1"
    //     }
    // }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Gemini Nano (AI on-device)
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Room Database (désactivé, on utilise SharedPreferences)
    // implementation("androidx.room:room-runtime:2.6.1")
    // implementation("androidx.room:room-ktx:2.6.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
