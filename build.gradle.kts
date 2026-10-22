import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-jvm-default=no-compatibility")
    }
}

dependencies {
    compileOnly(kotlin("stdlib"))
    // A locally built official API JAR also supports development without JitPack access.
    val localApiJar = providers.gradleProperty("spwApiJar")
    if (localApiJar.isPresent) {
        val apiJar = file(localApiJar.get())
        require(apiJar.isFile) { "spwApiJar must point to an existing official SPW API JAR: $apiJar" }
        compileOnly(files(apiJar))
    } else {
        compileOnly(libs.spw.workshop.api)
    }
    compileOnly(libs.pf4j)
    kapt(libs.pf4j)

    implementation(libs.gson)
    implementation(libs.okhttp)

    testImplementation(kotlin("stdlib"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockwebserver)
    testRuntimeOnly(libs.junit.platform.launcher)
}

val pluginName = "SLyric"

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Plugin-Class" to "com.gg.example.NeteaseLyricPlugin",
            "Plugin-Id" to "netease-lyric-provider",
            "Plugin-Name" to pluginName,
            "Plugin-Description" to "为 SPW 提供网易云的双语歌词匹配，支持翻译与罗马音独立开关以及部分歌曲的逐字歌词（符合SPL标准）。",
            "Plugin-Version" to project.version.toString(),
            "Plugin-Provider" to "Casper-003",
            "Plugin-Has-Config" to "true",
            "Plugin-Open-Source-Url" to "https://github.com/Casper-003/SPW-Lyric-Plugin",
        )
    }
}

// Keep the existing SPW ZIP layout; building no longer writes into a player's data directory.
val plugin by tasks.registering(Zip::class) {
    group = "distribution"
    description = "Build the SPW plugin ZIP in build/distributions."
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    archiveFileName.set("$pluginName-${project.version}.zip")
    into("classes") {
        from(tasks.named<Jar>("jar").map { zipTree(it.archiveFile) })
    }
    into("lib") {
        from(configurations.runtimeClasspath.map { files -> files.filter { it.extension == "jar" } })
    }
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.register<Copy>("installPlugin") {
    group = "distribution"
    description = "Copy the plugin ZIP to -PpluginInstallDir or the Windows SPW plugin directory."
    from(plugin.flatMap { it.archiveFile })
    into(providers.provider {
        providers.gradleProperty("pluginInstallDir").orNull
            ?: providers.environmentVariable("APPDATA").orNull?.let {
                "$it/Salt Player for Windows/workshop/plugins"
            }
            ?: error("Set -PpluginInstallDir=<directory> or APPDATA to install the plugin.")
    })
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("failed", "skipped") }
}

tasks.assemble { dependsOn(plugin) }
