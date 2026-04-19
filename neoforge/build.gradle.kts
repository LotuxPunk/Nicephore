plugins {
    id("net.neoforged.moddev") version "2.0.141"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
}

val modId = project.property("mod_id") as String
val modName = project.property("mod_name") as String
val modLicense = project.property("mod_license") as String
val modVersion = project.version.toString()
val minecraftVersion = project.property("minecraft_version") as String
val minecraftVersionRange = project.property("minecraft_version_range") as String
val neoVersion = project.property("neo_version") as String

// Reference :common's SourceSetOutput via objects.fileCollection() rather than hardcoded
// paths. Direct SourceDirectorySet / SourceSetOutput references carry a back-link to
// :common's KotlinBuildScript instance which the configuration cache refuses to serialize.
val commonMainSourceSet = project(":common").sourceSets.getByName("main")
val commonResources = objects.fileCollection().from(commonMainSourceSet.resources.srcDirs)
val commonClasses = objects.fileCollection().from(commonMainSourceSet.output.classesDirs)

base {
    archivesName.set("$modId-neoforge")
}

sourceSets.named("main") {
    resources {
        srcDir("src/generated/resources")
        exclude("**/*.bbmodel")
        exclude("src/generated/**/.cache")
    }
}

neoForge {
    version = neoVersion

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
            programArgument("--nogui")
        }
        create("data") {
            clientData()
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath,
            )
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.getByName("main"))
            // Pull :common's compiled classes into NeoForge's TRANSFORMER classloader so
            // NativeImage and other Minecraft types resolve to the same Class object on
            // both sides of the ServiceLoader boundary (fixes ClassCastException on events).
            sourceSet(project(":common").sourceSets.getByName("main"))
        }
    }
}

val localRuntime by configurations.creating
configurations.named("runtimeClasspath") {
    extendsFrom(localRuntime)
}

dependencies {
    // Shared multi-loader module
    implementation(project(":common"))

    implementation("com.profesorfalken:jPowerShell:3.1.1")
    jarJar("com.profesorfalken:jPowerShell:[3.1.1,4.0.0)") {
        version {
            prefer("3.1.1")
        }
    }

    // Bundle Kotlin stdlib since we're not using KotlinForForge
    jarJar("org.jetbrains.kotlin:kotlin-stdlib:[2.3.20,2.4.0)") {
        version {
            prefer("2.3.20")
        }
    }

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    jarJar("org.jetbrains.kotlinx:kotlinx-coroutines-core:[1.10.2,1.11.0)") {
        version {
            prefer("1.10.2")
        }
    }

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
    jarJar("org.jetbrains.kotlinx:kotlinx-datetime:[0.7.1,0.8.0)") {
        version {
            prefer("0.7.1")
        }
    }

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    jarJar("org.jetbrains.kotlinx:kotlinx-serialization-json:[1.7.3,1.8.0)") {
        version {
            prefer("1.7.3")
        }
    }
    jarJar("org.jetbrains.kotlinx:kotlinx-serialization-core:[1.7.3,1.8.0)") {
        version {
            prefer("1.7.3")
        }
    }
}

val generateModMetadata by tasks.registering(ProcessResources::class) {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.named("main") {
    resources {
        srcDir(generateModMetadata)
    }
}
neoForge.ideSyncTask(generateModMetadata.get())

// Pull :common's resources (lang, pack.mcmeta, logo) into the NeoForge jar.
tasks.named<ProcessResources>("processResources") {
    from(commonResources)
}

// Bundle :common's compiled classes into the NeoForge jar. ModDev's
// mods { sourceSet(project(":common")...) } block makes :common visible on the
// dev runClient classpath, but the built jar distributed to end users only
// contains what the jar task writes. Without this, NicephoreNeoForge crashes
// at runtime with NoClassDefFoundError: com/vandendaelen/nicephore/config/NicephoreConfigHolder.
tasks.named<Jar>("jar") {
    dependsOn(":common:classes")
    from(commonClasses)
}
