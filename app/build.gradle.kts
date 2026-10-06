plugins { id("com.android.application") }

android {
    namespace = "io.github.silent07137.mizuwidget"
    compileSdk { version = release(37) { minorApiLevel = 0 } }
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "io.github.silent07137.mizuwidget"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "io.github.silent07137.mizuwidget.WidgetTestRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { buildConfig = true }
    signingConfigs {
        create("ciRelease") {
            val path = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
            if (providers.environmentVariable("GITHUB_ACTIONS").orNull == "true" && !path.isNullOrBlank()) {
                storeFile = file(path)
                storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            if (providers.environmentVariable("GITHUB_ACTIONS").orNull == "true" &&
                !providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("ciRelease")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    lint { abortOnError = true; checkReleaseBuilds = true }
}
