import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.DownloadExtension
import de.undercouch.gradle.tasks.download.DownloadTaskPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.support.zipTo
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import kotlin.text.Charsets.UTF_8

// Note: the plugins block is evaluated before the script itself, so no variables can be used
plugins {
    kotlin("jvm") version "1.2.71"
    application
    id("com.github.ben-manes.versions") version "0.20.0"
    id("de.undercouch.download") version "3.4.3"
}

/** working directory of the application */
val workDir = file("working dir")
val kotlinVersion: String by extra {
    buildscript.configurations["classpath"]
            .resolvedConfiguration.firstLevelModuleDependencies
            .find { it.moduleName=="org.jetbrains.kotlin.jvm.gradle.plugin" }!!.moduleVersion
}
val supportedJavaVersions = arrayOf(JavaVersion.VERSION_1_9, JavaVersion.VERSION_1_10)

if (JavaVersion.current() !in supportedJavaVersions) {
    println("""org.gradle.java.home=${properties["org.gradle.java.home"]}
        |Java version ${JavaVersion.current()} can't be used.
        | Set one of ${supportedJavaVersions.joinToString()} as system default or create a "gradle.properties"
        | file with "org.gradle.java.home" pointing to a supported Java version""".trimMargin())
    throw IllegalStateException("Invalid Java version: ${JavaVersion.current()}")
}

sourceSets {
    getByName("main") {
        java.srcDir("src")
        resources.srcDir("src")
    }
}

kotlin {
    copyClassesToJavaOutput = true
    experimental.coroutines = Coroutines.ENABLE
}

allprojects {
    apply(plugin = "kotlin")
    buildDir = file(properties["player.buildDir"] ?: rootDir.resolve("build")).resolve(name)

    tasks.withType<JavaCompile> {
        options.encoding = UTF_8.name()
        options.isIncremental = true
        options.isWarnings = true
        options.isDeprecation = true
        options.compilerArgs = listOf(
                "-Xlint:unchecked",
                "--add-exports", "javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED",
                "--add-exports", "javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED",
                "--add-exports", "javafx.web/com.sun.webkit=ALL-UNNAMED",
                "--add-exports", "javafx.graphics/com.sun.glass.ui=ALL-UNNAMED"
        )
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    repositories {
        jcenter()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

dependencies {

    // Kotlin
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))

    // Logging
    compile("org.slf4j", "slf4j-api", "1.7.25")
    compile("org.slf4j", "jul-to-slf4j", "1.7.25")
    compile("ch.qos.logback", "logback-classic", "1.2.3")
    compile("io.github.microutils", "kotlin-logging", "1.6.10")

    // JavaFX
    compile("de.jensd", "fontawesomefx", "8.9")
    compile("org.reactfx", "reactfx", "2.0-M5")
    compile("eu.hansolo", "tilesfx", "1.6.4") {
        exclude("com.googlecode.json-simple", "json-simple")
    }
    compile("eu.hansolo", "Medusa", "8.0")

    // Native
    compile("net.java.dev.jna", "jna-platform", "4.5.2")
    compile("com.1stleg", "jnativehook", "2.0.2")   // due to a critical error on linux, don't update to 2.1.0

    // Misc
    compile("net.objecthunter", "exp4j", "0.4.8")
    compile("org.atteo", "evo-inflector", "1.2.2")
    compile("com.thoughtworks.xstream", "xstream", "1.4.10") {
        exclude("xmlpull", "xmlpull")
        exclude("xpp3", "xpp3_min")
    }

    // Audio
    compile("uk.co.caprica", "vlcj", "3.10.1")
    compile("de.u-mass", "lastfm-java", "0.1.2")
    compile("com.github.goxr3plus", "Jaudiotagger", "V2.2.6")

    // Image
    val imageioVersion = "3.4.1"
    compile("com.drewnoakes", "metadata-extractor", "2.11.0")
    compile("com.twelvemonkeys.imageio", "imageio-core", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-bmp", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-jpeg", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-iff", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-icns", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-pcx", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-pict", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-clippath", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-hdr", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-pdf", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-pnm", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-psd", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-tga", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-sgi", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-thumbsdb", imageioVersion)
    compile("com.twelvemonkeys.imageio", "imageio-tiff", imageioVersion)

}

tasks {
    val main = "_Main"

    val copyLibs by creating(Sync::class) {
        group = "build"
        description = "Copies all libraries into the working dir"
        into("working dir/lib")
        from(configurations.compile)
        exclude("*sources.jar", "*javadoc.jar")
    }

    val jdk by creating {
        val jdkLocal = workDir.resolve("java")
        group = "build setup"
        description = "Links $jdkLocal to JDK"
        doFirst {
            if (!jdkLocal.exists()) {
                println("Making JDK locally accessible...")
                val jdkPath = System.getProperty("java.home").takeIf { it.isNotBlank() }
                        ?.let { Paths.get(it) }
                        ?: throw FileNotFoundException("Unable to find JDK")
                try {
                    Files.createSymbolicLink(jdkLocal.toPath(), jdkPath)
                } catch (e: Exception) {
                    println("Couldn't create a symbolic link from $jdkLocal to $jdkPath: $e")
                    if (System.getProperty("os.name").startsWith("Windows")) {
                        println("Trying junction...")
                        val process = Runtime.getRuntime().exec("cmd.exe /c mklink /j \"$jdkLocal\" \"$jdkPath\"")
                        val exitValue = process.waitFor()
                        if (exitValue==0 && jdkLocal.exists())
                            println("Junction successful!")
                        else
                            throw IOException("Unable to make JDK locally accessible!\nmklink exit code: $exitValue", e)
                    } else {
                        throw IOException("Unable to make JDK locally accessible!", e)
                    }
                }
            }
        }
    }

    val kotlinc by creating(Download::class) {
        val kotlinc = workDir.resolve("kotlinc")
        group = "build setup"
        description = "Downloads the kotlin compiler into $kotlinc"
        onlyIf { kotlinc.resolve("build.txt").takeIf { it.exists() }?.readText()?.startsWith(kotlinVersion)!=true }
        doFirst {
            if (kotlinc.exists()) {
                println("Deleting obsolete version of Kotlin compiler...")
                if (!kotlinc.deleteRecursively())
                    throw IOException("Failed to remove Kotlin compiler, location=$kotlinc")
            }
        }
        src("https://github.com/JetBrains/kotlin/releases/download/v$kotlinVersion/kotlin-compiler-$kotlinVersion.zip")
        dest(buildDir)
        doLast {
            copy {
                from(zipTree(buildDir.resolve("kotlin-compiler-$kotlinVersion.zip")))
                into(workDir)
            }
            file("$workDir/kotlinc/bin/kotlinc").setExecutable(true)
        }
    }

    "jar"(Jar::class) {
        group = main
        destinationDir = workDir
        archiveName = "PlayerFX.jar"
    }

    "clean" {
        group = main
        description = "Cleans up temporary files"
        doFirst {
            workDir.resolve("user/tmp").deleteRecursively()
            workDir.resolve("lib").deleteRecursively()
            workDir.resolve("widgets").walkBottomUp()
                    .filter { it.path.endsWith("class") }
                    .fold(true) { res, it -> (it.delete() || !it.exists()) && res }
        }
    }

    "build" {
        group = main
        dependsOn(":widgets:build")
    }

    "run"(JavaExec::class) {
        group = main
        workingDir = workDir
        dependsOn(copyLibs, jdk, kotlinc, "jar")
    }

}

application {
    applicationName = "PlayerFX"
    mainClassName = "sp.it.pl.main.AppUtil"
    applicationDefaultJvmArgs = listOf(
            "-Dfile.encoding=UTF-8",
            "-ms"+(properties["player.memoryMin"] ?: "100m"),
            "-mx"+(properties["player.memoryMax"] ?: "3g"),
            *properties["player.jvmArgs"]?.toString()?.split(' ')?.toTypedArray().orEmpty(),
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens", "java.base/java.text=ALL-UNNAMED",
            "--add-opens", "java.base/java.util.stream=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.desktop/java.awt.font=ALL-UNNAMED",
            "--add-opens", "javafx.controls/javafx.scene.control=ALL-UNNAMED",
            "--add-opens", "javafx.controls/javafx.scene.control.skin=ALL-UNNAMED",
            "--add-opens", "javafx.graphics/com.sun.glass.ui=ALL-UNNAMED",
            "--add-opens", "javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED",
            "--add-opens", "javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED",
            "--add-opens", "javafx.graphics/javafx.scene.image=ALL-UNNAMED",
            "--add-opens", "javafx.web/com.sun.webkit=ALL-UNNAMED"
    )
}
