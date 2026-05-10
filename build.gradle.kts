plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.compose") version "1.6.10"
}

group = "com.jdisktree"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/interactive")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "J-DiskTree"
            packageVersion = "1.0.0"
            vendor = "J-DiskTree Team"
            description = "High-performance disk space analyzer"
            
            windows {
                shortcut = true // Create desktop shortcut
                menu = true     // Create Start Menu entry
                menuGroup = "J-DiskTree"
                upgradeUuid = "8e9f5b61-4d3e-4f5c-8b2a-1c5d6e7f89ab" // Valid hexadecimal UUID
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
