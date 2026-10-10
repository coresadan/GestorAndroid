plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.compose")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.sqlite.jdbc)
    implementation(libs.org.json)
}

compose.desktop {
    application {
        mainClass = "com.example.gestorstock.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName = "GestorStock"
            packageVersion = "1.0.0"
            description = "GestorStock - Control de Inventario y Ventas"
            copyright = "© 2026 GestorStock"
            modules("java.instrument", "java.sql", "jdk.unsupported")
            windows {
                menuGroup = "GestorStock"
                shortcut = true
            }
        }
    }
}
