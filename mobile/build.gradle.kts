plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.tafmetar.mobile"
    compileSdk = 34

    defaultConfig {
        // IMPORTANT : le Wearable Data Layer route les DataItem/Message par
        // AppKey = (applicationId, signature). L'app mobile et l'app Wear DOIVENT donc partager
        // exactement le même applicationId et la même clé de signature, sinon Play Services
        // transporte bien les octets mais ne trouve aucun destinataire sur le nœud pair et
        // abandonne silencieusement ("WearableService: Failed to deliver message").
        // Le `namespace` ci-dessus, lui, reste distinct : il ne sert qu'aux classes générées.
        applicationId = "com.example.tafmetar"
        minSdk = 26
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
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Communication avec la montre (Data Layer API)
    implementation("com.google.android.gms:play-services-wearable:18.2.0")

    // Réseau
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Tâche de fond périodique
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Favoris persistants
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
