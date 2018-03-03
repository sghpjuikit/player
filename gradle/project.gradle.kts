import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.util.zip.ZipInputStream
import kotlin.text.Charsets.UTF_8

// Note: the plugins block is evaluated before the script itself, so no variables can be used
plugins {
    kotlin("jvm") version "1.2.30"
    application
    id("com.github.ben-manes.versions") version "0.17.0"
}

/** working directory of the application */
val workDir = file("working dir")
val javaVersion = JavaVersion.VERSION_1_9
val kotlinVersion: String by extra {
    buildscript.configurations["classpath"]
            .resolvedConfiguration.firstLevelModuleDependencies
            .find { it.moduleName=="org.jetbrains.kotlin.jvm.gradle.plugin" }!!.moduleVersion
}

if (JavaVersion.current()!=javaVersion)
    throw IllegalStateException("Invalid Java version: ${JavaVersion.current()}")

java {
    targetCompatibility = javaVersion
    sourceCompatibility = javaVersion
    sourceSets {
        getByName("main") {
            java.srcDir("src")
            resources.srcDir("src")
        }
    }
}

kotlin {
    copyClassesToJavaOutput = true
    experimental.coroutines = Coroutines.ENABLE
}

allprojects {
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    buildDir = file(properties["player.buildDir"] ?: "build").resolve(name)

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
    }
}

dependencies {

    // Kotlin
    compile("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", kotlinVersion)
    compile("org.jetbrains.kotlin", "kotlin-reflect", kotlinVersion)

    // Logging
    compile("org.slf4j", "slf4j-api", "1.7.25")
    compile("ch.qos.logback", "logback-classic", "1.2.3")
    compile("io.github.microutils", "kotlin-logging", "1.5.3")

    // JavaFX
    compile("de.jensd", "fontawesomefx", "8.9")
    compile("org.reactfx", "reactfx", "2.0-M5")
    compile("eu.hansolo", "tilesfx", "1.5.2") {
        exclude("com.googlecode.json-simple", "json-simple")
    }
    compile("eu.hansolo", "Medusa", "7.9")

    // Audio
    compile("uk.co.caprica", "vlcj", "3.10.1")
    compile("de.u-mass", "lastfm-java", "0.1.2")

    // misc
    compile("net.java.dev.jna", "jna-platform", "4.5.1")
    compile("com.1stleg", "jnativehook", "2.1.0")

    compile("net.objecthunter", "exp4j", "0.4.8")
    compile("org.atteo", "evo-inflector", "1.2.2")
    compile("com.thoughtworks.xstream", "xstream", "1.4.10") {
        exclude("xmlpull", "xmlpull")
        exclude("xpp3", "xpp3_min")
    }

    // Image
    compile("com.drewnoakes", "metadata-extractor", "2.11.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-core", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-bmp", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-jpeg", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-iff", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-icns", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-pcx", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-pict", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-clippath", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-hdr", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-pdf", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-pnm", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-psd", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-tga", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-sgi", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-thumbsdb", "3.4.0")
    //	compile("com.twelvemonkeys.imageio", "imageio-tiff", "3.4.0")

    compile(files(file("extra/lib").listFiles()))

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

    val jre by creating {
        val jdkLocal = workDir.resolve("jre")
        group = "build setup"
        description = "Makes JDK locally accessible in $jdkLocal"
        doFirst {
            if (!jdkLocal.exists()) {
                println("Making JDK locally accessible...")
                val jdkGlobalPath = System.getProperty("java.home").takeIf { it.isNotBlank() }
                        ?.let { file(it).toPath() }
                        ?: throw FileNotFoundException("Unable to find JDK")
                try {
                    Files.createSymbolicLink(jdkLocal.toPath(), jdkGlobalPath)
                } catch (e: Exception) {
                    println("Couldn't create a symbolic link from $jdkLocal to $jdkGlobalPath: ${e.message}")
                    if (System.getProperty("os.name").startsWith("Windows")) {
                        println("Trying junction...")
                        val process = Runtime.getRuntime().exec("cmd.exe /c mklink /j \"$jdkLocal\" \"$jdkGlobalPath\"")
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

    val kotlinc by creating {
        val kotlinc = workDir.resolve("kotlinc")
        group = "build setup"
        description = "Downloads the kotlin compiler into $kotlinc"
        doFirst {
            val kotlincUpToDate = kotlinc.resolve("build.txt").takeIf { it.exists() }?.readText()?.startsWith(kotlinVersion)==true
            if (!kotlincUpToDate) {
                if (kotlinc.exists()) {
                    println("Previous version of Kotlin compiler exists. Deleting...")
                    if (!kotlinc.deleteRecursively())
                        throw IOException("Failed to remove Kotlin compiler, location=$kotlinc")
                }

                try {
                    val url = URL("https://github.com/JetBrains/kotlin/releases/download/v$kotlinVersion/kotlin-compiler-$kotlinVersion.zip")
                    val zip = ZipInputStream(url.openStream())
                    while (true) {
                        val next = zip.nextEntry ?: break
                        if (!next.isDirectory) {
                            val file = workDir.resolve(next.name)
                            file.parentFile.mkdirs()
                            val out = file.outputStream()
                            zip.copyTo(out)
                        } else {
                            println("Downloading ${next.name}")
                        }
                    }
                    if (!kotlinc.exists())
                        throw IOException("Kotlinc has not been downloaded successfully! Maybe the remote file is not a zip?")

                    file("$workDir/kotlinc/bin/kotlinc").setExecutable(true) // Allow kotlinc to be executed on Unix
                } catch (e: FileNotFoundException) {
                    throw IOException("The remote file could not be found", e)
                }
            }
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
            workDir.resolve("user/log").deleteRecursively()
            workDir.resolve("user/tmp").deleteRecursively()
            workDir.resolve("lib").deleteRecursively()
            workDir.resolve("widgets").walkBottomUp()
                    .filter { it.path.endsWith("class") }
                    .fold(true, { res, it -> (it.delete() || !it.exists()) && res })
        }
    }

    "run"(JavaExec::class) {
        group = main
        workingDir = workDir
        dependsOn(copyLibs, jre, kotlinc, "jar")
    }

}

application {
    applicationName = "PlayerFX"
    mainClassName = "sp.it.pl.main.AppUtil"
    applicationDefaultJvmArgs = listOf(
            "-Dfile.encoding=UTF-8",
            "-ms"+(properties["player.memoryMin"] ?: "100m"),
            "-mx"+(properties["player.memoryMax"] ?: "3g"),
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
