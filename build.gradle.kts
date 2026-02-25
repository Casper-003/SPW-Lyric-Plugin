import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

group = "com.gg.example"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    // Kotlin 标准库
    compileOnly(kotlin("stdlib"))

    // SPW Workshop API
    project(":api").let {
        compileOnly(it)
        kapt(it)
    }

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // 新增这行
//     实际开发中应该使用下面的依赖
//    compileOnly(libs.spw.workshop.api)
//    kapt(libs.spw.workshop.api)
}

// 插件元数据配置
val pluginClass = "com.gg.example.NeteaseLyricPlugin"
val pluginId = "netease-lyric-provider"
val pluginHasConfig = "true"
val pluginVersion = "1.0.1"
val pluginProvider = "Casper-003"
val pluginName = "SLyric"
val pluginDescription = "为 SPW 提供网易云音乐的高精度双语歌词匹配，支持翻译与罗马音独立开关。"
val pluginRepository = "https://github.com/Casper-003/SPW-Lyric-Plugin"

// 配置主 JAR 任务
tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Plugin-Class" to pluginClass,
            "Plugin-Id" to pluginId,
            "Plugin-Name" to pluginName,
            "Plugin-Description" to pluginDescription,
            "Plugin-Version" to pluginVersion,
            "Plugin-Provider" to pluginProvider,
            "Plugin-Has-Config" to "true",
            "Plugin-Open-Source-Url" to pluginRepository,
        )
    }
}

// 创建插件分发包
tasks.register<Jar>("plugin") {
    destinationDirectory.set(
        file(System.getenv("APPDATA") + "/Salt Player for Windows/workshop/plugins/")
    )
    archiveFileName.set("$pluginName-$pluginVersion.zip")

    into("classes") {
        with(tasks.named<Jar>("jar").get())
    }
    dependsOn(configurations.runtimeClasspath)
    into("lib") {
        from({
            configurations.runtimeClasspath
                .get()
                .filter { it.name.endsWith("jar") }
        })
    }
    archiveExtension.set("zip")
}
