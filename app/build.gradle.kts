plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Version de l'application = tag « vX.Y.Z » de CE dépôt (mobile), jamais celle du PC.
// La CI la passe avec -PappVersion=X.Y.Z ; en local, c'est le dernier tag de la branche.
val appVersion: String = (findProperty("appVersion") as String?)
    ?: runCatching {
        providers.exec { commandLine("git", "describe", "--tags", "--abbrev=0", "--match", "v[0-9]*") }
            .standardOutput.asText.get().trim().removePrefix("v")
    }.getOrNull()?.takeIf { it.isNotEmpty() }
    ?: "0.0.0"
require(Regex("""\d+\.\d+\.\d+""").matches(appVersion)) { "Version invalide : « $appVersion » (attendu X.Y.Z)" }
// X.Y.Z → X×10000 + Y×100 + Z (Y et Z < 100) ; toujours croissant d'une version à la suivante.
val appVersionCode = appVersion.split('.').map(String::toInt).let { (x, y, z) -> maxOf(1, x * 10000 + y * 100 + z) }

android {
    namespace = "fr.toupiearena.mobile"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "fr.toupiearena.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion
    }

    // Clé de publication fournie par la CI (secrets GitHub) ; sans elle, l'APK de publication
    // est signé avec la clé de débogage (essais locaux).
    val releaseKeystore = System.getenv("RELEASE_KEYSTORE")?.let(::file)?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKeystore != null) create("release") {
            storeFile = releaseKeystore
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Essai de cross-play avec la version PC (voir CrossPlayTest) : chemin du programme côté PC.
        unitTests.all { it.environment("TOUPIE_PC_INTEROP", System.getenv("TOUPIE_PC_INTEROP") ?: "") }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    // org.json est fourni par Android ; les tests JVM ont besoin de la vraie bibliothèque.
    testImplementation("org.json:json:20240303")
}
