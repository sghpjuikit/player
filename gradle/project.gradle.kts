import kotlin.text.Charsets.UTF_8
import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
import org.gradle.jvm.toolchain.JvmVendorSpec.ADOPTIUM
import org.gradle.jvm.toolchain.JvmVendorSpec.AMAZON
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// ----- plugin block; evaluated before the script itself
plugins {
   kotlin("jvm") version "2.0.0-RC1"
   application
   id("com.github.ben-manes.versions") version "0.51.0"   // adds task `dependencyUpdates, see https://github.com/ben-manes/gradle-versions-plugin
   // id("com.jaredsburrows.license") version "0.9.7"   // adds task `licenseReport`, see https://github.com/jaredsburrows/gradle-license-plugin // incompatible with latest gradle
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
val dirJdk = dirApp/"java"
val javaVersionSupported = JavaVersion.VERSION_21

allprojects {
   apply(plugin = "kotlin")

   kotlin {
      jvmToolchain {
         languageVersion = JavaLanguageVersion.of(21)
         vendor = ADOPTIUM
      }
      sourceSets.all {
         languageSettings.apply {
            languageVersion = "2.0"
         }
      }
   }

   layout.buildDirectory = file("player.buildDir".prjProp ?: rootDir/".gradle-build")/name

   tasks.withType<JavaCompile> {
      options.encoding = UTF_8.name()
      options.isIncremental = true
      options.isWarnings = true
      options.isDeprecation = true
      options.compilerArgs = listOf(
         "-Xlint:unchecked",
         "-Xlint:preview",
      )
      sourceCompatibility = javaVersionSupported.majorVersion
      targetCompatibility = javaVersionSupported.majorVersion
   }

   tasks.withType<KotlinCompile> {
      // https://kotlinlang.org/docs/gradle-compiler-options.html
      compilerOptions {
         apiVersion = KOTLIN_2_0
         languageVersion = KOTLIN_2_0
         suppressWarnings = false
         verbose = true
         freeCompilerArgs = listOf(
            "-Xno-call-assertions",
            "-Xno-param-assertions",
            "-Xjvm-default=all",
            "-Xlambdas=indy",
            "-Xstring-concat=indy-with-constants",
         )
         javaParameters = true
         jvmTarget = JVM_21
      }
   }

   repositories {
      mavenCentral()
      maven("https://jitpack.io")
      maven("https://mlt.jfrog.io/artifactory/mlt-mvn-releases-local") // for voice-cmu-slt-hsmm
      flatDir { dirs("lib") }
   }

   @Suppress("SpellCheckingInspection")
   dependencies {

      "Kotlin" group {
         // compatibility table https://kotlinlang.org/docs/releases.html#release-details
         implementation(kotlin("stdlib-jdk8"))
         implementation(kotlin("reflect"))
         implementation(kotlin("compiler-embeddable"))
         implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.8.1-Beta")
         implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-javafx", "1.8.1-Beta")
         implementation("org.jetbrains", "annotations", "24.1.0")
      }

      "lib" group {
        implementation(fileTree("lib"))
      }

      "JavaFX" group {
         val version = "23-ea+3"
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
         implementation("org.slf4j", "slf4j-api", "2.0.9")
         implementation("org.slf4j", "jul-to-slf4j", "2.0.9")
         implementation("ch.qos.logback", "logback-classic", "1.5.6") // https://logback.qos.ch/news.html
         implementation("io.github.microutils", "kotlin-logging", "3.0.4")
      }

      "Native" group {
         implementation("net.java.dev.jna", "jna-platform", "5.14.0")
         implementation("com.github.kwhat", "jnativehook", "2.2.2")
      }

      "Audio" group {
         implementation("uk.co.caprica", "vlcj", "4.8.2") {
            exclude("net.java.dev.jna", "jna-jpms")
            exclude("net.java.dev.jna", "jna-platform-jpms")
         }
         implementation("net.jthink", "jaudiotagger", "3.0.1")
      }

      "Misc" group {
         implementation("com.github.f4b6a3", "uuid-creator", "5.3.7")
         implementation("org.atteo", "evo-inflector", "1.3")
         implementation("com.github.ajalt.clikt", "clikt", "3.5.4")
         implementation("com.github.oshi", "oshi-core", "6.6.0")  // https://github.com/oshi/oshi/releases
         implementation("com.vladsch.flexmark", "flexmark-all", "0.64.8") {
            exclude("com.vladsch.flexmark", "flexmark-pdf-converter")
         }
         implementation("org.apache.pdfbox", "pdfbox", "3.0.2") // https://pdfbox.apache.org
         implementation("com.ezylang", "EvalEx", "3.2.0")
         implementation("com.ezylang", "EvalEx-big-math", "1.0.0")
         implementation("ch.obermuhlner", "big-math", "2.3.2")
      }

      "Image" group {
         implementation("com.drewnoakes", "metadata-extractor", "2.19.0")
         fun imageIO(name: String) = implementation("com.twelvemonkeys.imageio", "imageio-$name", "3.10.1")
         imageIO("bmp")
         imageIO("clippath")
         imageIO("core")
         imageIO("hdr")
         imageIO("icns")
         imageIO("iff")
         imageIO("jpeg")
         imageIO("metadata")
         imageIO("pcx")
         imageIO("pdf")
         imageIO("pict")
         imageIO("pnm")
         imageIO("psd")
         imageIO("reference")
         imageIO("sgi")
         imageIO("tga")
         imageIO("thumbsdb")
         imageIO("tiff")
         imageIO("webp")
      }

      "Http" group {
         implementation("javax.jmdns","jmdns","3.4.1")
         implementation("io.ktor", "ktor-server-core", "2.3.6")
         implementation("io.ktor", "ktor-client-java", "2.3.6")
      }

      "Test" group {
         testImplementation("io.kotest", "kotest-runner-junit5-jvm", "5.8.0")
         testImplementation("io.kotest", "kotest-assertions-core-jvm", "5.8.0")
      }

      "Db" group {
         implementation("org.furyio", "fury-core", "0.2.1")
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

javaToolchains.compilerFor {
   languageVersion = JavaLanguageVersion.of(20)
   vendor = ADOPTIUM
}


tasks {

   val copyLibs by creating(Sync::class) {
      group = "build"
      description = "Copies all libraries into the app dir"
      from(configurations.runtimeClasspath, project(":util").configurations.runtimeClasspath)
      into(dirApp/"lib")
      duplicatesStrategy = EXCLUDE
   }

   @Suppress("UNUSED_VARIABLE", "KotlinRedundantDiagnosticSuppress")
   val linkJdk by creating(LinkJDK::class) {
      group = "build setup"
      description = "Links JDK to project relative directory"
      linkLocation = dirJdk
   }

   @Suppress("UNUSED_VARIABLE", "KotlinRedundantDiagnosticSuppress")
   val generateFileHierarchy by creating(GenerateKtFileHierarchy::class) {
      group = "build setup"
      description = "Generates file hierarchy class and documentation"
      inFileHierarchy = project.rootDir/"file-info"
      outFileHierarchy = project.rootDir/"src"/"player"/"main"/"sp"/"it"/"pl"/"main"/"AppFiles.kt"
      outPackage = "sp.it.pl.main"
      outIndent = "   "
      outRootPath = """File("").absolutePath"""
   }

   @Suppress("UNUSED_VARIABLE", "KotlinRedundantDiagnosticSuppress")
   val generateSettings by creating(GenerateKtSettings::class) {
      group = "build setup"
      description = "Generates application settings class"
      outFile = project.rootDir/"src"/"player"/"main"/"sp"/"it"/"pl"/"main"/"AppSettings.kt"
      outPackage = "sp.it.pl.main"
      outIndent = "   "
      settings = appSetting
   }

   @Suppress("UNUSED_VARIABLE", "KotlinRedundantDiagnosticSuppress")
   val cleanAppData by creating(CleanAppDataTask::class) {
      group = "application"
      description = "Deletes app data including user data"
      appDir = dirApp
   }

   val jar by getting(Jar::class) {
      dependsOn(copyLibs)
      group = "build"
      destinationDirectory = dirApp
      archiveFileName = "SpitPlayer.jar"
   }

   "run"(JavaExec::class) {
      dependsOn(jar)  // the widgets need the jar on the classpath
      workingDir = dirApp
      args = listOf("--dev")
   }

   "assemble" {
      dependsOn(":widgets:assemble")
   }

   "build" {
      dependsOn(":widgets:build")
   }

}

// incompatible with latest gradle
//licenseReport {
//   generateCsvReport = false
//   generateHtmlReport = false
//   generateJsonReport = true
//   generateTextReport = false
//}

application {
   applicationName = "Spit Player"
   mainClass = "sp.it.pl.main.AppKt"
   applicationDefaultJvmArgs = listOf(
      "-Dname=SpitPlayer",
      "-Dfile.encoding=UTF-8",
      "-Xms" + ("player.memoryMin".prjProp ?: "50m"),
      "-Xmx" + ("player.memoryMax".prjProp ?: "3g"),
      "-XX:MinHeapFreeRatio=5",  // Hotspot gc only
      "-XX:MaxHeapFreeRatio=10",  // Hotspot gc only
      "-XX:+UseStringDeduplication",
      "-XX:+UseCompressedOops",
      "-XX:+CompactStrings",  // OpenJ9 only
      *"player.jvmArgs".prjProp?.split(' ')?.toTypedArray().orEmpty()
   )
}