plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Número da build do GitHub Actions vira versionCode, para que cada APK novo
// possa ser instalado por cima do anterior (adb install -r / Downloader).
val ciBuild = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.darkglass.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.darkglass.launcher"
        minSdk = 24
        targetSdk = 35
        versionCode = ciBuild
        versionName = "1.0.$ciBuild"
    }

    signingConfigs {
        // Chave fixa APENAS para testes (versionada no repositório de propósito):
        // mantém a mesma assinatura entre builds e permite atualizar sem desinstalar.
        create("test") {
            storeFile = file("signing/darkglass-test.jks")
            storePassword = "darkglass"
            keyAlias = "darkglass"
            keyPassword = "darkglass"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("test")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("test")
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
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    testImplementation("junit:junit:4.13.2")
}
