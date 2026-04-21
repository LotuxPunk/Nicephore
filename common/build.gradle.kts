plugins {
    id("net.neoforged.moddev") version "2.0.141"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
}

val modId = project.property("mod_id") as String
val neoVersion = project.property("neo_version") as String

base {
    archivesName.set("$modId-common")
}

// Apply NeoForge moddev in "library mode" — provides deobfuscated Minecraft classes
// on the compile classpath without producing a mod jar or configuring NeoForge runs.
// fabric-loom + officialMojangMappings() does not work for MC 26.1 (no ProGuard mappings published).
neoForge {
    version = neoVersion
}

dependencies {
    // Kotlin stdlib + kotlinx libs are provided by the loader at runtime (fabric-language-kotlin bundles,
    // or NeoForge jarJars them). Here they are compileOnly so :common can reference them.
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.3.20")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    compileOnly("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    compileOnly("com.profesorfalken:jPowerShell:3.1.1")

    testImplementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.20")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("com.profesorfalken:jPowerShell:3.1.1")
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
