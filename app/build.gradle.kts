import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// [+] Секреты подписи читаются из ВНЕШНЕГО файла keystore.properties (папка keys/,
//     которая НЕ должна попадать в архивы проекта).
//     Путь можно переопределить: -PKEYSTORE_PROPERTIES=... или env KEYSTORE_PROPERTIES.
val keystoreProps: Properties? = run {
    val path = (project.findProperty("KEYSTORE_PROPERTIES") as String?)
        ?: System.getenv("KEYSTORE_PROPERTIES")
        ?: "/storage/emulated/0/AndroidCSProjects/MagiskNext/keys/keystore.properties"
    val f = file(path)
    if (f.exists()) Properties().apply { f.inputStream().use { load(it) } } else null
}

android {
    namespace = "com.magisk.next"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.magisk.next"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            // [*] Конфиг создаётся только если keystore.properties найден
            //     и указанный в нём keystore реально существует —
            //     иначе debug-сборка и configure-фаза не падают.
            val props = keystoreProps
            val storePathProp = props?.getProperty("RELEASE_STORE_FILE")
            if (storePathProp != null && file(storePathProp).exists()) {
                storeFile = file(storePathProp)
                storePassword = props.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = props.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = props.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            // [*] подписываем release-ключом только если keystore сконфигурирован
            val rc = signingConfigs.getByName("release")
            if (rc.storeFile != null) {
                signingConfig = rc
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// [*] Замена устаревшего kotlinOptions (Kotlin 2.0 DSL)
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.17.0")
        force("androidx.core:core-ktx:1.17.0")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2025.04.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // libsu — root shell для Android от автора Magisk
    implementation("com.github.topjohnwu.libsu:core:5.3.0")
    implementation("androidx.compose.material:material-icons-core") {
        exclude(group = "androidx.core")
    }
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}