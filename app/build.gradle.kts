import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val ciKeystore = layout.buildDirectory.file("ci/siya-debug.keystore").get().asFile
if (!ciKeystore.exists()) {
    val encoded = file("ci/siya-debug.keystore.b64").readText().trim()
    ciKeystore.parentFile.mkdirs()
    ciKeystore.writeBytes(Base64.getDecoder().decode(encoded))
}

android {
    namespace = "com.siya.ai"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.siya.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0"
        vectorDrawables.useSupportLibrary = true
    }
    signingConfigs {
        create("ciStableDebug") {
            storeFile = ciKeystore
            storePassword = "siya1234"
            keyAlias = "siya-debug"
            keyPassword = "siya1234"
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("ciStableDebug") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        jniLibs {
            pickFirsts += "lib/arm64-v8a/libonnxruntime.so"
            pickFirsts += "lib/armeabi-v7a/libonnxruntime.so"
            pickFirsts += "lib/x86/libonnxruntime.so"
            pickFirsts += "lib/x86_64/libonnxruntime.so"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.28.2")
    implementation("com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.8")
    implementation("dev.ffmpegkit-maintained:llama-android:0.1.1")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
