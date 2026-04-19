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

// Reference :common's SourceSetOutput via objects.fileCollection() rather than hardcoded
// paths. The wrapper produces a plain ConfigurableFileCollection that serializes cleanly
// into the configuration cache (direct SourceDirectorySet / SourceSetOutput references
// back-link to :common's KotlinBuildScript instance and are rejected).
val commonMainSourceSet = project(":common").sourceSets.getByName("main")
val commonResources = objects.fileCollection().from(commonMainSourceSet.resources.srcDirs)
val commonClasses = objects.fileCollection().from(commonMainSourceSet.output.classesDirs)

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

val modJsonProps: Map<String, Any> = mapOf(
    "version" to modVersion,
    "mod_id" to modId,
    "mod_name" to modName,
    "mod_license" to modLicense,
    "minecraft_version_range" to fabricMcRange,
)

// Template fabric.mod.json in a dedicated Copy task. The template lives in
// src/main/templates/ (not src/main/resources/) so processResources doesn't pick it
// up as a raw resource. Moving expand() out of filesMatching { ... } removes the
// Kotlin lambda that would otherwise capture the enclosing KotlinBuildScript, which
// the configuration cache refuses to serialize.
val generateFabricModJson by tasks.registering(Copy::class) {
    from("src/main/templates/fabric.mod.json")
    into(layout.buildDirectory.dir("generated/fabric-mod-json"))
    inputs.properties(modJsonProps)
    expand(modJsonProps)
}

tasks.named<ProcessResources>("processResources") {
    from(generateFabricModJson)               // pick up the expanded fabric.mod.json
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
