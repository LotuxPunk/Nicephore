pluginManagement {
    val loomVersion = settings.providers.gradleProperty("loom_version").get()
    val forgeGradleVersion = settings.providers.gradleProperty("forge_gradle_version").get()
    plugins {
        id("net.fabricmc.fabric-loom") version loomVersion
        id("net.minecraftforge.gradle") version forgeGradleVersion
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
        maven {
            name = "MinecraftForge"
            url = uri("https://maven.minecraftforge.net/")
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
include(":forge")
