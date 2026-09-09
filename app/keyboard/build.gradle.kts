plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.zakhrafa.keyboard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.keyboard.calligraphy"
        minSdk = 24
        targetSdk = 36
        versionCode = 58
        versionName = "5.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootProject.projectDir}/release/zakhrafa-upload-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "ZakhrafaUpload2026!"
            keyAlias = System.getenv("KEY_ALIAS") ?: "zakhrafa_upload"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "ZakhrafaUpload2026!"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":engine"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.play.services.ads)
    testImplementation(libs.junit)
}
