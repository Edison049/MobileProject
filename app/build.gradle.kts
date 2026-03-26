plugins {
    alias(libs.plugins.android.application)
    // id("com.google.gms.google-services") // 暂时注释，等待google-services.json文件
}

android {
    namespace = "com.innovationai.myapplication"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.innovationai.myapplication"
        minSdk = 34
        targetSdk = 36
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
    }
    
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test:core:1.6.1")
    
    // Firebase BoM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")
    
    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore")
    
    // Firebase Storage
    implementation("com.google.firebase:firebase-storage")
    
    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    // ExoPlayer for video playback (Media3)
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-common:1.2.0")
    
    // RecyclerView and CardView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Material Design Components
    implementation("com.google.android.material:material:1.12.0")
    
    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
}
