plugins {
    id("com.android.application")
}

// scripts/build-android.sh passes SDL's Java sources (org.libsdl.app, fetched
// with the SDL3 the native build uses, so Java and native code match).
val sdlJava: String? = providers.gradleProperty("sdlJava").orNull

android {
    namespace = "com.mh3.wii"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mh3.wii"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        ndk { abiFilters += "arm64-v8a" }
    }

    sourceSets["main"].java.srcDir("src/main/java")
    if (sdlJava != null) sourceSets["main"].java.srcDir(sdlJava)

    packaging {
        // The 4 GB disc image is copied at run time, never packaged; the
        // libraries are loaded straight from the APK where the platform allows.
        jniLibs.useLegacyPackaging = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
