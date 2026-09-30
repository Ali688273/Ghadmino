plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ir.ghadmino.stepcounter"

    compileSdk = 36

    defaultConfig {
        applicationId = "ir.ghadmino.stepcounter"

        minSdk = 26
        targetSdk = 34

        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("ghadmino-release.jks")
            storePassword = System.getenv("GHADMINO_KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("GHADMINO_KEY_ALIAS") ?: "ghadmino"
            keyPassword = System.getenv("GHADMINO_KEY_PASSWORD")
                ?: System.getenv("GHADMINO_KEYSTORE_PASSWORD")
                ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
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
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

configurations.all {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.22")
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22")
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")

    implementation(
        "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
    )

    implementation(
        "androidx.activity:activity-compose:1.8.2"
    )

    implementation(
        platform("androidx.compose:compose-bom:2023.10.01")
    )

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation(
        "androidx.compose.material:material-icons-extended"
    )

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    implementation(
        "androidx.datastore:datastore-preferences:1.1.0"
    )

    implementation(
        "androidx.health.connect:connect-client:1.1.0"
    )

    // Production ad SDKs
    implementation(
        "ir.tapsell.plus:tapsell-plus-sdk-android:2.3.3"
    )

    implementation(
        "com.adivery:sdk:4.9.0"
    )

    implementation(
        "com.google.android.gms:play-services-ads-identifier:18.0.1"
    )
}
