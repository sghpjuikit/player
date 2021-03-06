
import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.text.Charsets.UTF_8

// ----- plugin block; evaluated before the script itself

plugins {
   kotlin("jvm") version "1.5.10"
   application
   id("com.github.ben-manes.versions") version "0.38.0"
}

// ----- util block; defined first to help IDE with syntax checking for erroneous code

val String.prjProp: String?
   get() = properties[this]?.toString()

fun Project.tests(configuration: Test.() -> Unit) {
   tasks { test { configuration() } }
}

// ----- build block

/** Working directory of the application */
val dirApp = file("app")
val dirJDKSystem = file("org.gradle.java.home".prjProp ?: "java.home".sysProp ?: failIO { "property 'org.gradle.java.home' not set up" })
val dirJdk = dirApp/"java"
val kotlinVersion: String by extra {
   buildscript.configurations["classpath"]
      .resolvedConfiguration.firstLevelModuleDependencies
      .find { it.moduleName=="org.jetbrains.kotlin.jvm.gradle.plugin" }!!.moduleVersion
}
val javaSupportedVersions = arrayOf(JavaVersion.VERSION_12)
val javaVersion = JavaVersion.current().also {
   require(it in javaSupportedVersions) {
      "" +
         "Java version $it can't be used.\n" +
         "Set one of ${javaSupportedVersions.joinToString()} in 'gradle.properties'" +
         "file (in project directory) with 'org.gradle.java.home' pointing to a supported JDK version.\n" +
         "Currently org.gradle.java.home=${"org.gradle.java.home".prjProp}"
   }
}

allprojects {
   apply(plugin = "kotlin")

   buildDir = file("player.buildDir".prjProp ?: rootDir/".gradle-build")/name

   tasks.withType<JavaCompile> {
      options.encoding = UTF_8.name()
      options.isIncremental = true
      options.isWarnings = true
      options.isDeprecation = true
      options.compilerArgs = listOf(
         "-Xlint:unchecked"
      )
   }

   tasks.withType<KotlinCompile> {
      kotlinOptions.apiVersion = "1.5"
      kotlinOptions.languageVersion = "1.5"
      kotlinOptions.suppressWarnings = false
      kotlinOptions.verbose = true
      kotlinOptions.freeCompilerArgs += listOf(
         "-progressive",
         "-Xno-call-assertions",
         "-Xno-param-assertions",
         "-Xjvm-default=all",
         "-Xlambdas=indy",
         "-Xuse-experimental=kotlin.Experimental",
         "-Xstring-concat=indy-with-constants"
      )
      kotlinOptions.javaParameters = true
      kotlinOptions.jdkHome = dirJdk.path
      kotlinOptions.jvmTarget = javaVersion.majorVersion
   }

   repositories {
      jcenter()
      mavenCentral()
      maven("https://jitpack.io")
   }

   dependencies {

      "Kotlin" group {
         implementation(kotlin("stdlib-jdk8"))
         implementation(kotlin("reflect"))
         implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.5.0")
         implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-javafx", "1.5.0")
         implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.5.0")
         implementation("org.jetbrains", "annotations", "21.0.1")
      }

      "JavaFX" group {
         val version = "17-ea+11"
         val os = org.gradle.internal.os.OperatingSystem.current()
         val classifier = when {
            os.isLinux -> "linux"
            os.isMacOsX -> "mac"
            os.isWindows -> "win"
            else -> failIO { "Unable to determine javafx dependency classifier due to unfamiliar system=$os" }
         }
         listOf("base", "controls", "graphics", "media", "swing").forEach {
            implementation("org.openjfx", "javafx-$it", version, classifier = classifier)
         }
         implementation("de.jensd", "fontawesomefx", "8.9")
      }

      "Logging" group {
         implementation("org.slf4j", "slf4j-api")
         implementation("org.slf4j", "jul-to-slf4j", "1.7.25")
         implementation("ch.qos.logback", "logback-classic", "1.2.3")
         implementation("io.github.microutils", "kotlin-logging", "2.0.8")
      }

      "Audio" group {
         implementation("uk.co.caprica", "vlcj", "4.7.1")
         implementation("org.bitbucket.ijabz", "jaudiotagger", "35ea31b")
      }

      "Native" group {
         implementation("net.java.dev.jna", "jna-platform", "5.8.0")
         implementation("com.1stleg", "jnativehook", "2.1.0")
      }

      "Misc" group {
         implementation("com.github.f4b6a3", "uuid-creator", "3.7.2")
         implementation("net.objecthunter", "exp4j", "0.4.8")
         implementation("org.atteo", "evo-inflector", "1.2.2")
         implementation("com.github.ajalt", "clikt", "2.1.0")
         implementation("org.apache.commons", "commons-text", "1.9")
      }

      "Image" group {
         implementation("com.github.umjammer", "javavp8decoder", "d51fe8f")
         implementation("com.drewnoakes", "metadata-extractor", "2.16.0")
         fun imageIO(name: String) = implementation("com.twelvemonkeys.imageio", "imageio-$name", "3.7.0")
         imageIO("bmp")
         imageIO("clippath")
         imageIO("hdr")
         imageIO("icns")
         imageIO("iff")
         imageIO("jpeg")
         imageIO("pcx")
         imageIO("pict")
         imageIO("pnm")
         imageIO("psd")
         imageIO("pdf")
         imageIO("tga")
         imageIO("sgi")
         imageIO("thumbsdb")
         imageIO("tiff")
      }

      "Http" group {
         implementation("io.ktor", "ktor-server-core", "1.6.0")
         implementation("io.ktor", "ktor-client-cio", "1.6.0")
      }

      "Test" group {
         testImplementation("io.kotest", "kotest-runner-junit5-jvm", "4.6.0")
         testImplementation("io.kotest", "kotest-assertions-core-jvm", "4.6.0")
      }

   }

   tests {
      useJUnitPlatform()
      testLogging.showStandardStreams = true
   }

}

sourceSets {
   main {
      java.srcDir("src/player/main")
      resources.srcDir("src/player/main")
   }
   test {
      java.srcDir("src/player/test")
      resources.srcDir("src/player/test")
   }
}

dependencies {
   implementation(project(":util"))
}

tasks {

   val copyLibs by creating(Sync::class) {
      group = "build"
      description = "Copies all libraries into the app dir"
      from(configurations.compileClasspath, project(":util").configurations.compileClasspath)
      into(dirApp/"lib")
      duplicatesStrategy = EXCLUDE
   }

   val linkJdk by creating(LinkJDK::class) {
      group = "build setup"
      description = "Links JDK to project relative directory"
      jdkLocation = dirJDKSystem
      linkLocation = dirJdk
   }

   @Suppress("UNUSED_VARIABLE")
   val generateFileHierarchy by creating(GenerateKtFileHierarchy::class) {
      group = "build setup"
      description = "Generates file hierarchy class and documentation"
      inFileHierarchy = project.rootDir/"file-info"
      outFileHierarchy = project.rootDir/"src"/"player"/"main"/"sp"/"it"/"pl"/"main"/"AppFiles.kt"
      outPackage = "sp.it.pl.main"
      outIndent = "   "
      outRootPath = """File("").absolutePath"""
   }

   @Suppress("UNUSED_VARIABLE")
   val generateSettings by creating(GenerateKtSettings::class) {
      group = "build setup"
      description = "Generates application settings class"
      outFile = project.rootDir/"src"/"player"/"main"/"sp"/"it"/"pl"/"main"/"AppSettings.kt"
      outPackage = "sp.it.pl.main"
      outIndent = "   "
      settings = appSetting
   }

   val jar by getting(Jar::class) {
      dependsOn(copyLibs)
      group = "build"
      destinationDirectory.set(dirApp)
      archiveFileName.set("SpitPlayer.jar")
   }

   "run"(JavaExec::class) {
      dependsOn(jar)  // the widgets need the jar on the classpath
      workingDir = dirApp
      args = listOf("--dev")
   }

   "build" {
      dependsOn(":widgets:build")
   }

   "compileKotlin" {
      dependsOn(linkJdk)
   }

   "clean"(Delete::class) {
      description = "Cleans up built dir, lib dir, user tmp dir and widget compilation output"
      delete(
         buildDir,
         dirApp/"lib",
         dirApp/"user"/"tmp",
         dirApp.resolve("widgets").listFiles().orEmpty().map { it/"out" }
      )
   }

}

application {
   applicationName = "Spit Player"
   mainClass.set("sp.it.pl.main.AppKt")
   applicationDefaultJvmArgs = listOf(
      "-Dfile.encoding=UTF-8",
      "-ms" + ("player.memoryMin".prjProp ?: "50m"),
      "-mx" + ("player.memoryMax".prjProp ?: "3g"),
      "-XX:MinHeapFreeRatio=5",  // Hotspot gc only
      "-XX:MaxHeapFreeRatio=10",  // Hotspot gc only
      "-XX:+UseStringDeduplication",
      "-XX:+UseCompressedOops",
      "-XX:+CompactStrings",  // OpenJ9 only
      *"player.jvmArgs".prjProp?.split(' ')?.toTypedArray().orEmpty(),
      "--illegal-access=permit"
   )
}