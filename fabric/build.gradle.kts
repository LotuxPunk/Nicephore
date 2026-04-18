import org.gradle.api.JavaVersion

plugins {
    // Version centralised in settings.gradle.kts pluginManagement (reads loom_version from gradle.properties).
    id("net.fabricmc.fabric-loom")
}

val modId = project.property("mod_id") as String
val modName = project.property("mod_name") as String
val modLicense = project.property("mod_license") as String
val modVersion = project.version.toString()
val minecraftVersion = project.property("minecraft_version") as String
val fabricLoaderVersion = project.property("fabric_loader_version") as String
val fabricApiVersion = project.property("fabric_api_version") as String
val fabricKotlinVersion = project.property("fabric_kotlin_version") as String
val fabricMcRange = project.property("fabric_minecraft_version_range") as String

val commonResources = project(":common").sourceSets.getByName("main").resources
val commonClasses = project(":common").sourceSets.getByName("main").output.classesDirs

base {
    archivesName.set("$modId-fabric")
}

dependencies {
    "minecraft"("com.mojang:minecraft:$minecraftVersion")

    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

    // Shared multi-loader module
    implementation(project(":common"))

    // jPowerShell is used by :common's WindowsClipboardManagerImpl. On Fabric, this class
    // is only instantiated when running on Windows (guarded by OperatingSystems.getOS()).
    // Needs to be on the runtime classpath for Windows Fabric users; pulled into the jar
    // via include so end users don't need a separate install.
    "include"(implementation("com.profesorfalken:jPowerShell:3.1.1")!!)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", modVersion)
    inputs.property("mod_id", modId)
    inputs.property("mod_name", modName)
    inputs.property("mod_license", modLicense)
    inputs.property("fabric_minecraft_version_range", fabricMcRange)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to modVersion,
                "mod_id" to modId,
                "mod_name" to modName,
                "mod_license" to modLicense,
                "minecraft_version_range" to fabricMcRange,
            )
        )
    }

    // Pull :common's resources (lang, pack.mcmeta, logo) into the Fabric jar.
    from(commonResources)
}

// Bundle :common's compiled classes into the Fabric jar. The dev-run classpath sees :common
// directly, but the packaged jar distributed to end users only contains what the jar task
// writes — without this, NicephoreFabric crashes at runtime with
// NoClassDefFoundError: com/vandendaelen/nicephore/Nicephore.
tasks.named<Jar>("jar") {
    dependsOn(":common:classes")
    from(commonClasses)
}
