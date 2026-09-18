plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.unmango.slip"
    compileSdk = 35

    // Pinned because AGP otherwise asks for whichever build tools it was built
    // against and tries to install them, which fails against a read-only SDK
    // out of the nix store. The flake composes this version.
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "dev.unmango.slip"

        // Matches the -androidapi the flake pins for gomobile. The .aar is
        // compiled against that NDK sysroot, so a lower minSdk here would link
        // against symbols the device does not have.
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // One APK per ABI. The bulk of this app is libgojni.so, so a universal
    // build is four copies of the Go runtime and three of them can never run.
    // Splitting costs nothing but extra outputs, and installDebug still picks
    // the right one for whatever is plugged in.
    //
    // 32-bit x86 is left out: no phone ships it and no current emulator image
    // needs it. x86_64 stays, because the emulator does.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
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
    }
}

dependencies {
    // Built by `make bind` at the repository root. It is not checked in: it is
    // 37M of compiled Go, and it is reproduced from the source beside it.
    implementation(files(rootProject.file("../mobile/slip.aar")))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime.ktx)
}
