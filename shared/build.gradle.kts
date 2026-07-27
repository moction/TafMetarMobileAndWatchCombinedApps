// Module Kotlin pur, partagé entre l'app mobile et l'app Wear OS.
// Contient uniquement des data classes et des constantes : aucune dépendance Android ici,
// pour qu'il compile aussi bien côté "mobile" que côté "wear" sans conflit.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}
