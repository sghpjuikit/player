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
    kotlin("jvm") version "1.3.0"
    application
    id("com.github.ben-manes.versions") version "0.20.0"
    id("de.undercouch.download") version "3.4.3"
}

/** working directory of the application */
val dirProject = file("working dir")
val dirJdk = dirProject.resolve("java")
val kotlinVersion: String by extra {
    buildscript.configurations["classpath"]
            .resolvedConfiguration.firstLevelModuleDependencies
            .find { it.moduleName=="org.jetbrains.kotlin.jvm.gradle.plugin" }!!.moduleVersion
}
val javaSupportedVersions = arrayOf(JavaVersion.VERSION_1_9, JavaVersion.VERSION_1_10)

if (JavaVersion.current() !in javaSupportedVersions) {
    println("""org.gradle.java.home=${properties["org.gradle.java.home"]}
        |Java version ${JavaVersion.current()} can't be used.
        | Set one of ${javaSupportedVersions.joinToString()} as system default or create a "gradle.properties"
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
        kotlinOptions.jdkHome = dirJdk.path
        kotlinOptions.verbose = true
        kotlinOptions.suppressWarnings = false
        kotlinOptions.freeCompilerArgs += listOf("-progressive", "-Xjvm-default=enable")
    }

    repositories {
        jcenter()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

dependencies {

    // Kotlin
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-javafx", "1.0.+")

    // Audio
    implementation("uk.co.caprica", "vlcj", "3.10.+")
    implementation("de.u-mass", "lastfm-java", "0.1.2")
    implementation("com.github.goxr3plus", "Jaudiotagger", "V2.2.6")

    // JavaFX
    implementation("de.jensd", "fontawesomefx", "8.9")
    implementation("org.reactfx", "reactfx", "2.0-M5")
    implementation("eu.hansolo", "tilesfx", "1.6.+") { exclude("junit", "junit") }
    implementation("eu.hansolo", "Medusa", "8.0")

    // Logging
    implementation("org.slf4j", "slf4j-api")
    implementation("org.slf4j", "jul-to-slf4j", "1.7.25")
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    implementation("io.github.microutils", "kotlin-logging", "1.6.+")

    // Native
    implementation("net.java.dev.jna", "jna-platform", "5.0.+")
    implementation("com.1stleg", "jnativehook", "2.0.2") // don't update this to 2.1.0, it causes a critical error on linux

    // Misc
    implementation("net.objecthunter", "exp4j", "0.4.+")
    implementation("org.atteo", "evo-inflector", "1.2.+")
    implementation("com.thoughtworks.xstream", "xstream", "1.4.+")

    // Image
    implementation("com.drewnoakes", "metadata-extractor", "2.11.0")
    val imageioVersion = "3.4.1"
    implementation("com.twelvemonkeys.imageio", "imageio-bmp", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-jpeg", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-iff", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-icns", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-pcx", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-pict", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-clippath", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-hdr", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-pdf", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-pnm", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-psd", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-tga", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-sgi", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-thumbsdb", imageioVersion)
    implementation("com.twelvemonkeys.imageio", "imageio-tiff", imageioVersion)

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

    val linkJdk by creating {
        group = "build setup"
        description = "Links $dirJdk to JDK"
        doFirst {
            if (!dirJdk.exists()) {
                println("Making JDK locally accessible...")
                val jdkPath = System.getProperty("java.home").takeIf { it.isNotBlank() }
                        ?.let { Paths.get(it) }
                        ?: throw FileNotFoundException("Unable to find JDK")
                try {
                    Files.createSymbolicLink(dirJdk.toPath(), jdkPath)
                } catch (e: Exception) {
                    println("Couldn't create a symbolic link from $dirJdk to $jdkPath: $e")
                    if (System.getProperty("os.name").startsWith("Windows")) {
                        println("Trying junction...")
                        val process = Runtime.getRuntime().exec("cmd.exe /c mklink /j \"$dirJdk\" \"$jdkPath\"")
                        val exitValue = process.waitFor()
                        if (exitValue==0 && dirJdk.exists())
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
        val kotlinc = dirProject.resolve("kotlinc")
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
                into(dirProject)
            }
            file("$dirProject/kotlinc/bin/kotlinc").setExecutable(true)
        }
    }

    "jar"(Jar::class) {
        group = main
        destinationDir = dirProject
        archiveName = "PlayerFX.jar"
    }

    "clean"(Delete::class) {
        group = main
        description = "Cleans up temporary files"
        delete(dirProject.resolve("user/tmp"), buildDir,
                dirProject.resolve("widgets").walkBottomUp().filter { it.path.endsWith("class") }.toList())
    }

    "build" {
        dependsOn(":widgets:build")
        group = main
    }

    "run"(JavaExec::class) {
        dependsOn(copyLibs, kotlinc, "jar")
        group = main
        workingDir = dirProject
    }

    getByName("compileKotlin").dependsOn(linkJdk)

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
