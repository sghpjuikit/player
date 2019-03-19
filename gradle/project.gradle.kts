
import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.text.Charsets.UTF_8

// Note: the plugins block is evaluated before the script itself, so no variables can be used
plugins {
    id("com.gradle.build-scan") version "2.1"
    kotlin("jvm") version "1.3.0"
    application
    id("com.github.ben-manes.versions") version "0.20.0"
    id("de.undercouch.download") version "3.4.3"
    id("org.openjfx.javafxplugin") version "0.0.7"
}

/** Working directory of the application */
val dirApp = file("app")
val dirJdk = dirApp/"java"
val kotlinVersion: String by extra {
    buildscript.configurations["classpath"]
            .resolvedConfiguration.firstLevelModuleDependencies
            .find { it.moduleName=="org.jetbrains.kotlin.jvm.gradle.plugin" }!!.moduleVersion
}
val javaSupportedVersions = arrayOf(JavaVersion.VERSION_11).also {
    val javaVersion = JavaVersion.current()
    if (javaVersion !in it) {
        println(""+
                "org.gradle.java.home=${"org.gradle.java.home".prjProp}"+
                "Java version $javaVersion can't be used."+
                "Set one of ${it.joinToString()} as system default or create a 'gradle.properties'"+
                "file with 'org.gradle.java.home' pointing to a supported Java version"
        )
        throw IllegalStateException("Invalid Java version: ${JavaVersion.current()}")
    }
}

javafx {
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.fxml", "javafx.media", "javafx.swing", "javafx.web")
}

sourceSets {
    getByName("main") {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("src"))
    }
    getByName("test") {
        java.setSrcDirs(listOf("src-test"))
        resources.setSrcDirs(listOf("src-test"))
    }
}

allprojects {
    apply(plugin = "kotlin")
    buildDir = file("player.buildDir".prjProp ?: rootDir/"build")/name

    tasks.withType<JavaCompile> {
        options.encoding = UTF_8.name()
        options.isIncremental = true
        options.isWarnings = true
        options.isDeprecation = true
        options.compilerArgs = listOf(
                "-Xlint:unchecked",
                "--add-exports", "javafx.graphics/com.sun.glass.ui=ALL-SYSTEM",
                "--add-exports", "javafx.controls/com.sun.javafx.scene.control.skin=ALL-SYSTEM"
        )
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.suppressWarnings = false
        kotlinOptions.verbose = true
        kotlinOptions.freeCompilerArgs += listOf(
                "-progressive",
                "-Xjvm-default=enable"
        )
        kotlinOptions.javaParameters = true
        kotlinOptions.jdkHome = dirJdk.path
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    repositories {
        jcenter()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

dependencies {

    "Kotlin" requires {
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("reflect"))
        implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
        implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-javafx", "1.1.0")
    }

    "Logging" requires {
        implementation("org.slf4j", "slf4j-api")
        implementation("org.slf4j", "jul-to-slf4j", "1.7.25")
        implementation("ch.qos.logback", "logback-classic", "1.2.3")
        implementation("io.github.microutils", "kotlin-logging", "1.6.22")
    }

    "Audio" requires {
        implementation("uk.co.caprica", "vlcj", "4.0.6")
        implementation("de.u-mass", "lastfm-java", "0.1.2")
        implementation("com.github.goxr3plus", "Jaudiotagger", "V2.2.6")
    }

    "JavaFX" requires {
        implementation("de.jensd", "fontawesomefx", "8.9")
        implementation("org.reactfx", "reactfx", "2.0-M5")
    }

    "Native" requires {
        implementation("net.java.dev.jna", "jna-platform")
        implementation("com.1stleg", "jnativehook", "2.1.0")
    }

    "Misc" requires {
        implementation("net.objecthunter", "exp4j", "0.4.8")
        implementation("org.atteo", "evo-inflector", "1.2.2")
        implementation("com.thoughtworks.xstream", "xstream", "1.4.11.1")
    }

    "Image" requires {
        implementation("com.drewnoakes", "metadata-extractor", "2.11.0")
        fun imageIO(name: String) = implementation("com.twelvemonkeys.imageio", "imageio-$name", "3.4.1")
        imageIO("bmp")
        imageIO("jpeg")
        imageIO("iff")
        imageIO("icns")
        imageIO("pcx")
        imageIO("pict")
        imageIO("clippath")
        imageIO("hdr")
        imageIO("pdf")
        imageIO("pnm")
        imageIO("psd")
        imageIO("tga")
        imageIO("sgi")
        imageIO("thumbsdb")
        imageIO("tiff")
    }

    "Test" requires {
        testImplementation("io.kotlintest", "kotlintest-runner-junit5", "3.2.1")
    }

}

tasks {
    val main = "_Main"

    val copyLibs by creating(Sync::class) {
        group = "build"
        description = "Copies all libraries into the app dir"
        from(configurations.compileClasspath)
        into(dirApp/"lib")
    }

    val linkJdk by creating {
        group = "build setup"
        description = "Links $dirJdk to JDK"
        onlyIf { !dirJdk.exists() }
        doFirst {
            dirJdk.delete() // delete invalid symbolic link
            println("Making JDK locally accessible...")
            val jdkPath = "java.home".sysProp?.let { Paths.get(it) } ?: failIO { "Unable to find JDK" }
            try {
                Files.createSymbolicLink(dirJdk.toPath(), jdkPath)
            } catch (e: Exception) {
                println("Couldn't create a symbolic link from $dirJdk to $jdkPath: $e")
                if ("os.name".sysProp?.startsWith("Windows")==true) {
                    println("Trying junction...")
                    val process = Runtime.getRuntime().exec("""cmd.exe /c mklink /j "$dirJdk" "$jdkPath"""")
                    val exitValue = process.waitFor()
                    if (exitValue==0 && dirJdk.exists()) println("Junction successful!")
                    else failIO(e) { "Unable to make JDK locally accessible!\nmklink exit code: $exitValue" }
                } else {
                    failIO(e) { "Unable to make JDK locally accessible!" }
                }
            }
        }
    }

    val kotlinc by creating(Download::class) {
        val os = org.gradle.internal.os.OperatingSystem.current()
        val useExperimentalKotlinc = "player.kotlinc.experimental".prjProp?.toBoolean() ?: true
        val dirKotlinc = dirApp/"kotlinc"
        val fileKotlinVersion = dirKotlinc/"build.txt"
        val nameKotlinc = when {
            !useExperimentalKotlinc -> "kotlin-compiler-$kotlinVersion.zip"
            os.isLinux -> "experimental-kotlin-compiler-$kotlinVersion-linux-x64.zip"
            os.isMacOsX -> "experimental-kotlin-compiler-$kotlinVersion-macos-x64.zip"
            os.isWindows -> "experimental-kotlin-compiler-$kotlinVersion-windows-x64.zip"
            else -> failIO { "Unable to determine kotlinc version due to unfamiliar system=$os" }
        }
        val fileKotlinc = dirKotlinc/"bin"/"kotlinc"
        val zipKotlinc = dirKotlinc/nameKotlinc
        val eKotlinc = dirKotlinc/"experimental"
        group = "build setup"
        description = "Downloads the kotlin compiler into $dirKotlinc"
        onlyIf { !fileKotlinVersion.exists() || !fileKotlinVersion.readText().startsWith(kotlinVersion) || eKotlinc.exists() != useExperimentalKotlinc }
        src("https://github.com/JetBrains/kotlin/releases/download/v$kotlinVersion/$nameKotlinc")
        dest(dirKotlinc)
        doFirst {
            println("Obtaining Kotlin compiler experimental=$useExperimentalKotlinc from=$src")

            if (dirKotlinc.exists()) {
                println("Deleting obsolete version of Kotlin compiler...")
                dirKotlinc.deleteRecursively().orFailIO { "Failed to remove Kotlin compiler, location=$dirKotlinc" }
            }

            if (!dirKotlinc.exists()) {
                dirKotlinc.mkdir().orFailIO { "Failed to create directory=$dirKotlinc" }
            }
        }
        doLast {
            copy {
                from(zipTree(zipKotlinc))
                into(dirApp)
            }
            fileKotlinc.setExecutable(true).orFailIO { "Failed to make file=$fileKotlinc executable" }
            zipKotlinc.delete().orFailIO { "Failed to delete file=$zipKotlinc" } // clean up downloaded file
            zipKotlinc.createNewFile()  // allows running this task in offline mode
            if (useExperimentalKotlinc) eKotlinc.createNewFile()
        }
    }

    val jar by getting(Jar::class) {
        dependsOn(copyLibs, kotlinc)
        group = main
        destinationDirectory.set(dirApp)
        archiveFileName.set("PlayerFX.jar")
    }

    "clean"(Delete::class) {
        group = main
        description = "Cleans up built dir, lib dir, user tmp dir and widget compilation output"
        delete(
                buildDir,
                dirApp/"lib",
                dirApp/"user"/"tmp",
                dirApp.resolve("widgets").walkBottomUp().filter { it.path.endsWith("class") }.toList()
        )
    }

    "run"(JavaExec::class) {
        dependsOn(jar)  // the widgets need the jar on the classpath
        group = main
        workingDir = dirApp
    }

    "build" {
        dependsOn(":widgets:build")
        group = main
    }

    getByName("compileKotlin").dependsOn(linkJdk)

}

test {
    useJUnitPlatform { }
    testLogging.showStandardStreams = true
}

application {
    applicationName = "PlayerFX"
    mainClassName = "sp.it.pl.main.AppKt"
    applicationDefaultJvmArgs = listOf(
            "-Dfile.encoding=UTF-8",
            "-ms"+("player.memoryMin".prjProp ?: "100m"),
            "-mx"+("player.memoryMax".prjProp ?: "3g"),
            *"player.jvmArgs".prjProp?.split(' ')?.toTypedArray().orEmpty(),
            "--add-exports", "javafx.graphics/com.sun.glass.ui=ALL-UNNAMED",
            "--add-exports", "javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens", "java.base/java.text=ALL-UNNAMED",
            "--add-opens", "java.base/java.util.stream=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.desktop/java.awt.font=ALL-UNNAMED",
            "--add-opens", "javafx.controls/javafx.scene.control=ALL-UNNAMED",
            "--add-opens", "javafx.controls/javafx.scene.control.skin=ALL-UNNAMED",
            "--add-opens", "javafx.graphics/javafx.scene.image=ALL-UNNAMED",
            "--add-opens", "javafx.graphics/javafx.stage=ALL-UNNAMED",
            "--add-opens", "javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED",
            "--add-opens", "javafx.web/com.sun.webkit=ALL-UNNAMED"
    )
}


operator fun File.div(childName: String) = this.resolve(childName)

val String.prjProp: String?
    get() = properties[this]?.toString()

val String.sysProp: String?
    get() = System.getProperty(this)?.takeIf { it.isNotBlank() }

infix fun String.requires(block: () -> Unit) = block()

fun failIO(cause: Throwable? = null, message: () -> String): Nothing = throw IOException(message(), cause)

fun Boolean.orFailIO(message: () -> String) = also { if (!this) failIO(null, message) }

@Suppress("UNUSED_VARIABLE")
fun Project.test(configuration: Test.() -> Unit) {
    val test by tasks.getting(Test::class, configuration)
}