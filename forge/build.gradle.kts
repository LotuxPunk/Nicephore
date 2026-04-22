plugins {
    // Version centralised in settings.gradle.kts pluginManagement (reads forge_gradle_version from gradle.properties).
    id("net.minecraftforge.gradle")
    // Jar-in-Jar is a separate plugin in ForgeGradle 7.
    id("net.minecraftforge.jarjar") version "0.2.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
}

val modId = project.property("mod_id") as String
val modName = project.property("mod_name") as String
val modLicense = project.property("mod_license") as String
val modVersion = project.version.toString()
val minecraftVersion = project.property("minecraft_version") as String
val minecraftVersionRange = project.property("minecraft_version_range") as String
val forgeVersion = project.property("forge_version") as String
val forgeLoaderVersionRange = project.property("forge_loader_version_range") as String

// Reference :common's SourceSetOutput via objects.fileCollection() rather than hardcoded
// paths. Direct SourceDirectorySet / SourceSetOutput references carry a back-link to
// :common's KotlinBuildScript instance which the configuration cache refuses to serialize.
val commonMainSourceSet = project(":common").sourceSets.getByName("main")
val commonResources = objects.fileCollection().from(commonMainSourceSet.resources.srcDirs)
val commonClasses = objects.fileCollection().from(commonMainSourceSet.output.classesDirs)

base {
    archivesName.set("$modId-forge")
}

sourceSets.named("main") {
    resources {
        srcDir("src/generated/resources")
        exclude("**/*.bbmodel")
        exclude("src/generated/**/.cache")
    }
}

minecraft {
    mappings("official", minecraftVersion)

    runs {
        configureEach {
            workingDir.convention(layout.projectDirectory.dir("run"))
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")
        }

        register("client")

        register("server") {
            args("--nogui")
        }

        register("data") {
            workingDir.set(layout.projectDirectory.dir("run-data"))
            args(
                "--mod", modId,
                "--all",
                "--output", layout.projectDirectory.dir("src/generated/resources").asFile.absolutePath,
                "--existing", layout.projectDirectory.dir("src/main/resources").asFile.absolutePath,
            )
        }
    }
}

/**
 * Declares a library bundled into the Forge mod jar via jarJar.
 * Adds it to implementation (compile+runtime) unless [includeAsImplementation] is false —
 * set false only for libs the Kotlin plugin brings in automatically (kotlin-stdlib) or
 * transitive-only libs (kotlinx-serialization-core is pulled in by serialization-json).
 *
 * FG7's JarJar plugin auto-generates a `[$version,)` constraint from a bare-version coord,
 * which matches the "any version >= bundled, same major" semantics we want (minus the
 * explicit upper bound NeoForge ships — dropping that upper bound on Forge avoids tripping
 * FG7's "Only fully-qualified sets allowed in multiple set scenario" parser, which rejects
 * certain valid Maven ranges when transitive deps also declare ranges).
 */
fun DependencyHandler.bundledLib(
    group: String,
    name: String,
    version: String,
    includeAsImplementation: Boolean = true,
) {
    val coords = "$group:$name"
    if (includeAsImplementation) {
        add("implementation", "$coords:$version")
    }
    add("jarJar", "$coords:$version")
}

repositories {
    // ForgeGradle 7 injects a synthetic "mavenized" repo of the Minecraft artifacts.
    minecraft.mavenizer(this)
    // fg is the ForgeGradle extension's shortcut accessor added by the plugin.
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
}

// Register jarJar FIRST — this both creates the task and the "jarJar" dependency
// configuration that bundledLib() below needs. Drop the classifier so the jarJar output
// becomes the primary user-facing artifact; rename `jar` to "slim" so the two don't collide.
jarJar.register {
    archiveClassifier.set(null as String?)
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("slim")
}

dependencies {
    // ForgeGradle 7 wraps the Minecraft coordinate via minecraft.dependency(...).
    implementation(minecraft.dependency("net.minecraftforge:forge:$minecraftVersion-$forgeVersion"))

    // Shared multi-loader module
    implementation(project(":common"))

    bundledLib("com.profesorfalken", "jPowerShell", "3.1.1")
    // kotlin-stdlib: the Kotlin plugin adds it to implementation automatically, we only
    // need to jarJar it for runtime bundling.
    bundledLib("org.jetbrains.kotlin", "kotlin-stdlib", "2.3.20", includeAsImplementation = false)
    // Kotlinx multiplatform artifacts resolve to multiple jar variants (jvm/js/native) —
    // FG7's JarJar rejects multi-artifact modules, so pin to the platform-specific -jvm
    // publication. (NeoForge's ModDev jarJar handles this transparently, but the MinecraftForge
    // JarJar plugin requires the caller to pick a single-artifact variant.)
    bundledLib("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", "1.10.2")
    bundledLib("org.jetbrains.kotlinx", "kotlinx-datetime-jvm", "0.7.1")
    bundledLib("org.jetbrains.kotlinx", "kotlinx-serialization-json-jvm", "1.7.3")
    // serialization-core is a transitive of serialization-json; jarJar it so the nested
    // jar contains the runtime classes, no implementation entry needed.
    bundledLib("org.jetbrains.kotlinx", "kotlinx-serialization-core-jvm", "1.7.3", includeAsImplementation = false)
}

val generateModMetadata by tasks.registering(ProcessResources::class) {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "forge_version" to forgeVersion,
        "forge_loader_version_range" to forgeLoaderVersionRange,
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

// Pull :common's resources (lang, pack.mcmeta, logo) into the Forge jar.
tasks.named<ProcessResources>("processResources") {
    from(commonResources)
}

// Bundle :common's compiled classes into the Forge jar. ForgeGradle makes :common visible
// on the dev runClient classpath via project() dependency, but the built jar distributed
// to end users only contains what the jar task writes. Without this, NicephoreForge would
// crash at runtime with NoClassDefFoundError on :common classes. Mirrors the equivalent
// pattern in :neoforge and :fabric.
tasks.named<Jar>("jar") {
    dependsOn(":common:classes")
    from(commonClasses)
}

