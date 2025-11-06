import java.io.FileInputStream
import java.util.Properties
 
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.whatsappbulk.sender"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.whatsappbulk.sender"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // BuildConfig fields from app/android/local.properties
        val localProps = Properties()
        val localPropsFile = file("../local.properties")
        if (localPropsFile.exists()) {
            val fis = FileInputStream(localPropsFile)
            localProps.load(fis)
            fis.close()
        }

        val apiBaseUrlProp = (localProps.getProperty("api.base.url") ?: "http://127.0.0.1:3001")
        val apiBaseUrl = if (apiBaseUrlProp.endsWith("/")) apiBaseUrlProp else "$apiBaseUrlProp/"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            // Restore standard release (minified)
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }

        // Additional build type for testing without minification
        create("releaseNoMinify") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false
            // Keep same signing for convenience
            signingConfig = signingConfigs.getByName("debug")
            // Ensure tasks resolve similarly to release
            matchingFallbacks += listOf("release")
        }

        // Additional build type for testing WITH minification isolated (baseline)
        create("releaseMinify") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }

        // Stage 0: Minify enabled but no shrink/opt/obfuscation (via rules)
        create("releaseMinify0") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
                "proguard-minify0.pro"
            )
        }

        // Stage 1: Shrink only
        create("releaseMinify1") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
                "proguard-minify1.pro"
            )
        }

        // Stage 2: Shrink + optimize (no obfuscation)
        create("releaseMinify2") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-minify2.pro"
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
        compose = true
        buildConfig = true
    }

    composeOptions {
        // Align Compose Compiler with Kotlin 1.9.22
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM (Bill of Materials) - Maneja versiones de Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material:material-icons-core")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.8.2")

    // Core KTX
    implementation("androidx.core:core-ktx:1.12.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit (Networking)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room (Local Database)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // WorkManager (Background tasks)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Security Crypto (Encrypted SharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Gson (JSON parsing)
    implementation("com.google.code.gson:gson:2.10.1")

    // Accompanist (Compose utilities)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.9")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

// Copy custom launcher icons from app/iconos into res mipmap folders
tasks.register<Copy>("copyCustomLauncherIcons") {
    // Module dir is app/android/app; custom icons live in app/iconos
    val base = project.file("../../iconos")
    into("src/main/res")

    val dpis = listOf("mdpi","hdpi","xhdpi","xxhdpi","xxxhdpi")
    dpis.forEach { dpi ->
        // Remove default webp variants to avoid duplicate resources
        doFirst {
            delete(
                "src/main/res/mipmap-$dpi/ic_launcher.webp",
                "src/main/res/mipmap-$dpi/ic_launcher_round.webp"
            )
        }
        // ic_launcher.png -> ic_launcher.png
        from(file("$base/mipmap-$dpi/ic_launcher.png")) {
            into("mipmap-$dpi")
            rename { "ic_launcher.png" }
        }
        // duplicate as foreground for adaptive icon
        from(file("$base/mipmap-$dpi/ic_launcher.png")) {
            into("mipmap-$dpi")
            rename { "ic_launcher_foreground.png" }
        }
        // duplicate as round icon too (fallback)
        from(file("$base/mipmap-$dpi/ic_launcher.png")) {
            into("mipmap-$dpi")
            rename { "ic_launcher_round.png" }
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn("copyCustomLauncherIcons")
}
