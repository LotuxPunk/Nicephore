pluginManagement {
    val loomVersion = settings.providers.gradleProperty("loom_version").get()
    plugins {
        id("net.fabricmc.fabric-loom") version loomVersion
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "nicephore"

include(":common")
include(":fabric")
include(":neoforge")
