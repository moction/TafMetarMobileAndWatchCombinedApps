plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.tafmetar.wear"
    compileSdk = 34

    defaultConfig {
        // Doit être STRICTEMENT identique à l'applicationId du module :mobile — voir le
        // commentaire détaillé dans mobile/build.gradle.kts. Les deux APK vivent sur des
        // appareils différents, il n'y a donc aucun conflit d'installation.
        applicationId = "com.example.tafmetar"
        minSdk = 30 // Wear OS 3+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":shared"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Compose spécifique Wear (remplace material3 classique : composants adaptés à l'écran rond)
    implementation("androidx.wear.compose:compose-material:1.3.1")
    implementation("androidx.wear.compose:compose-navigation:1.3.1")
    implementation("androidx.wear.compose:compose-foundation:1.3.1")
    implementation("com.google.android.gms:play-services-wearable:18.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Cache local minimal (dernière donnée connue, affichée au démarrage avant synchro)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Tile Wear OS
    implementation("androidx.wear.tiles:tiles:1.4.0")
    // Pas de tiles-material : il n'expose que l'ancien espace de noms androidx.wear.tiles.material,
    // incompatible avec setTileTimeline() qui attend des éléments androidx.wear.protolayout.
    // La tuile est construite avec protolayout seul.
    implementation("androidx.wear:wear-tooling-preview:1.0.0")
    implementation("com.google.guava:guava:33.2.1-android")

    // NOTE : plus AUCUNE dépendance Retrofit/OkHttp ici — c'est tout l'intérêt du changement.
}
