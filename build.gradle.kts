import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("idea")
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
}

/**
 * Computes the mod version from git tags, strict exact-tag mode.
 * Falls back to "0.0.1-SNAPSHOT" when off-tag, outside a git checkout, or git is missing.
 */
fun computeModVersion(p: Project): String {
    try {
        val result = p.providers.exec {
            commandLine("git", "describe", "--tags", "--exact-match", "HEAD")
            workingDir = p.rootDir
            isIgnoreExitValue = true
        }
        if (result.result.get().exitValue == 0) {
            val tag = result.standardOutput.asText.get().trim()
            return if (tag.startsWith("v")) tag.substring(1) else tag
        }
    } catch (_: Throwable) {
    }
    return "0.0.1-SNAPSHOT"
}

allprojects {
    version = computeModVersion(this)
    group = project.property("mod_group_id") as String
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    repositories {
        mavenCentral()
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "MinecraftForge"
            url = uri("https://maven.minecraftforge.net/")
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

// Aggregate all loader jars into a single location for easy distribution.
// Usage: ./gradlew dist
// Output: build/dist/nicephore-{fabric,neoforge,forge}-<version>.jar
tasks.register<Sync>("dist") {
    group = "build"
    description = "Builds all loader jars and collects them into build/dist/."
    dependsOn(":fabric:build", ":neoforge:build", ":forge:build")
    // Capture the version string at configuration time — using project.version inside
    // the closure would break the configuration cache.
    val currentVersion = project.version.toString()
    from(project(":fabric").layout.buildDirectory.dir("libs"))
    from(project(":neoforge").layout.buildDirectory.dir("libs"))
    from(project(":forge").layout.buildDirectory.dir("libs"))
    into(layout.buildDirectory.dir("dist"))
    // Only pick up the current version's jars; loader build plugins don't clean
    // their own libs/ directory when the version string changes, so stale older
    // jars would otherwise pile up in build/dist/.
    include("*-$currentVersion.jar")
    exclude("*-sources.jar")
    exclude("*-javadoc.jar")
    exclude("*-dev.jar")
}
