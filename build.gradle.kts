import org.gradle.api.tasks.bundling.Zip

plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.compose") version "1.6.10"
}

group = "com.jdisktree"
version = "1.2.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/interactive")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.formdev:flatlaf:3.4.1")
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "J-DiskTree"
            packageVersion = "1.2.0"
            vendor = "J-DiskTree Team"
            description = "High-performance disk space analyzer"
            
            windows {
                shortcut = true // Create desktop shortcut
                menu = true     // Create Start Menu entry
                menuGroup = "J-DiskTree"
                upgradeUuid = "8e9f5b61-4d3e-4f5c-8b2a-1c5d6e7f89ab" // Valid hexadecimal UUID
            }

            buildTypes.release.proguard {
                isEnabled.set(false)
            }
        }
    }
}

tasks.register<Zip>("packagePortableZip") {
    group = "compose desktop"
    description = "Packs the release distributable into a portable ZIP archive"

    // Ensure we create the distributable first
    dependsOn("createReleaseDistributable")

    val appName = "J-DiskTree" 
    
    // Pick everything from the App Image folder
    from(layout.buildDirectory.dir("compose/binaries/main-release/app/$appName"))

    // Set the archive name and destination
    archiveFileName.set("${appName}-${project.version}-portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
