import kotlin.text.Charsets.UTF_8
import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
import org.gradle.jvm.toolchain.JvmVendorSpec.ADOPTIUM
import org.gradle.jvm.toolchain.JvmVendorSpec.AMAZON
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// ----- plugin block; evaluated before the script itself
plugins {
   kotlin("jvm") version "2.1.20-RC"
   application
   id("com.github.ben-manes.versions") version "0.52.0"   // adds task `dependencyUpdates, see https://github.com/ben-manes/gradle-versions-plugin
   // id("com.jaredsburrows.license") version "0.9.8"   // adds task `licenseReport`, see https://github.com/jaredsburrows/gradle-license-plugin // incompatible with latest gradle
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
val javaVersionSupported = JavaVersion.VERSION_23

allprojects {
   apply(plugin = "kotlin")

   kotlin {
      jvmToolchain {
         languageVersion = JavaLanguageVersion.of(23)
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
         jvmTarget = JVM_23
      }
   }

   repositories {
      mavenCentral()
      // support git repositories and dependencies referring to commits
      maven("https://jitpack.io")
      // support file jars
      flatDir { dirs("lib") }
   }

   @Suppress("SpellCheckingInspection")
   dependencies {

      "Kotlin" group {
         // compatibility table https://kotlinlang.org/docs/releases.html#release-details
         implementation(kotlin("reflect", "2.1.20-RC"))
         implementation(kotlin("compiler-embeddable"))
         implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.1")
         implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-javafx", "1.10.1")
         implementation("org.jetbrains", "annotations", "26.0.2") // https://github.com/JetBrains/java-annotations/releases
      }

      "lib" group {
        implementation(fileTree("lib"))
      }

      "JavaFX" group {
         val version = "25-ea+5"
         val os = org.gradle.internal.os.OperatingSystem.current()
         val classifier = when {
            os.isLinux -> "linux"
            os.isMacOsX -> "mac"
            os.isWindows -> "win"
            else -> failIO { "Unable to determine javafx dependency classifier due to unfamiliar system=$os" }
         }

         listOf("base", "controls", "graphics", "media", "swing", "web").forEach {
            implementation("org.openjfx", "javafx-$it", version, classifier = classifier)
         }
         implementation("de.jensd", "fontawesomefx", "8.9")
      }

      "Logging" group {
         implementation("org.slf4j", "slf4j-api", "2.1.0-alpha1")
         implementation("org.slf4j", "jul-to-slf4j", "2.1.0-alpha1")
         implementation("ch.qos.logback", "logback-classic", "1.5.16") // https://logback.qos.ch/news.html
         implementation("io.github.oshai", "kotlin-logging-jvm", "7.0.4")
      }

      "Native" group {
         implementation("net.java.dev.jna", "jna-platform", "5.16.0")
         implementation("com.github.kwhat", "jnativehook", "2.2.2")
      }

      "Audio" group {
         implementation("uk.co.caprica", "vlcj", "4.8.3") {
            exclude("net.java.dev.jna", "jna-jpms")
            exclude("net.java.dev.jna", "jna-platform-jpms")
         }
         implementation("uk.co.caprica", "vlcj-javafx", "1.2.0")

         // implementation("net.jthink", "jaudiotagger", "3.0.1") // unmaintained
         implementation("com.github.RouHim", "jaudiotagger", "1.2.27") // >1.2.27 has build errors and jitpack has no jar, see https://jitpack.io/com/github/RouHim/jaudiotagger/
      }

      "Misc" group {
         implementation("com.github.f4b6a3", "uuid-creator", "6.0.0")
         implementation("com.github.ajalt.clikt", "clikt", "3.5.4")
         implementation("com.github.oshi", "oshi-core", "6.6.6")  // https://github.com/oshi/oshi/releases
         implementation("com.vladsch.flexmark", "flexmark-all", "0.64.8") {
            exclude("com.vladsch.flexmark", "flexmark-pdf-converter")
         }
         implementation("org.apache.pdfbox", "pdfbox", "3.0.4") // https://pdfbox.apache.org
         implementation("com.ezylang", "EvalEx", "3.4.0")
         implementation("com.ezylang", "EvalEx-big-math", "1.0.1")
         implementation("ch.obermuhlner", "big-math", "2.3.2")
      }

      "Image" group {
         implementation("com.github.hervegirod", "fxsvgimage", "1.1")

         val version = "3.12.0"
         implementation("com.drewnoakes", "metadata-extractor", "2.19.0")
         implementation("com.twelvemonkeys.imageio", "imageio-core", version)
         implementation("com.twelvemonkeys.imageio", "imageio-batik", version) // requires batik-all
         implementation("org.apache.xmlgraphics", "batik-all", "1.18")
         implementation("com.twelvemonkeys.imageio", "imageio-bmp", version)
         implementation("com.twelvemonkeys.imageio", "imageio-clippath", version)
         implementation("com.twelvemonkeys.imageio", "imageio-hdr", version)
         implementation("com.twelvemonkeys.imageio", "imageio-icns", version)
         implementation("com.twelvemonkeys.imageio", "imageio-iff", version)
         implementation("com.twelvemonkeys.imageio", "imageio-jpeg", version)
         implementation("com.twelvemonkeys.imageio", "imageio-metadata", version)
         implementation("com.twelvemonkeys.imageio", "imageio-pcx", version)
         implementation("com.twelvemonkeys.imageio", "imageio-pdf", version)
         implementation("com.twelvemonkeys.imageio", "imageio-pict", version)
         implementation("com.twelvemonkeys.imageio", "imageio-pnm", version)
         implementation("com.twelvemonkeys.imageio", "imageio-psd", version)
         implementation("com.twelvemonkeys.imageio", "imageio-reference", version)
         implementation("com.twelvemonkeys.imageio", "imageio-sgi", version)
         implementation("com.twelvemonkeys.imageio", "imageio-tga", version)
         implementation("com.twelvemonkeys.imageio", "imageio-thumbsdb", version)
         implementation("com.twelvemonkeys.imageio", "imageio-tiff", version)
         implementation("com.twelvemonkeys.imageio", "imageio-webp", version)
      }

      "Http" group {
         implementation("javax.jmdns","jmdns","3.4.1")
         implementation("io.ktor", "ktor-server-core", "3.1.0")
         implementation("io.ktor", "ktor-client-java", "3.1.0")
      }

      "Test" group {
         testImplementation("io.kotest", "kotest-runner-junit5-jvm", "6.0.0.M2")
         testImplementation("io.kotest", "kotest-assertions-core-jvm", "6.0.0.M2")
      }

      "Db" group {
         implementation("org.apache.fury", "fury-core", "0.9.0")
         implementation("org.apache.fury", "fury-kotlin", "0.9.0")
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

   val copyLibs by registering(Sync::class) {
      group = "build"
      description = "Copies all libraries into the app dir"
      from(configurations.runtimeClasspath, project(":util").configurations.runtimeClasspath)
      into(dirApp/"lib")
      duplicatesStrategy = EXCLUDE
   }

   val linkJdk by registering(LinkJDK::class) {
      group = "build setup"
      description = "Links JDK to project relative directory"
      linkLocation = dirJdk
   }

   val generateFileHierarchy by registering(GenerateKtFileHierarchy::class) {
      group = "build setup"
      description = "Generates file hierarchy class and documentation"
      inFileHierarchy = project.rootDir/"file-info"
      outFileHierarchy = project.rootDir/"src"/"player"/"main"/"sp"/"it"/"pl"/"main"/"AppFiles.kt"
      outPackage = "sp.it.pl.main"
      outIndent = "   "
      outRootPath = """File("").absolutePath"""
   }

   val generateSettings by registering(GenerateKtSettings::class) {
      group = "build setup"
      description = "Generates application settings class"
      outFile = project.rootDir/"src"/"player"/"main"/"sp"/"it"/"pl"/"main"/"AppSettings.kt"
      outPackage = "sp.it.pl.main"
      outIndent = "   "
      settings = appSetting
   }

   val cleanAppData by registering(CleanAppDataTask::class) {
      group = "application"
      description = "Deletes app data including user data"
      appDir = dirApp
   }

   val jar by getting(Jar::class) {
      dependsOn(copyLibs)
      group = "build"
      destinationDirectory = dirApp
      archiveFileName = "SpitPlayer.jar"
      manifest {
         attributes["Main-Class"] = "sp.it.pl.main.AppKt"
      }
   }

   "run"(JavaExec::class) {
      dependsOn(jar)  // the widgets need the jar on the classpath
      workingDir = dirApp
      mainClass = "sp.it.pl.main.AppKt"
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
   executableDir = "app"
   applicationDefaultJvmArgs = listOf(
      "-Dname=SpitPlayer",
      "-D--dev",
      "-Xms" + ("player.memoryMin".prjProp ?: "50m"),
      "-Xmx" + ("player.memoryMax".prjProp ?: "3g"),
      "-XX:MinHeapFreeRatio=5",  // Hotspot gc only
      "-XX:MaxHeapFreeRatio=10",  // Hotspot gc only
      "-XX:+UseStringDeduplication",
      "-XX:+UseCompressedOops",
      "-XX:+CompactStrings",  // OpenJ9 only
      *"player.jvmArgs".prjProp?.split(' ')?.toTypedArray().orEmpty(),
   )
}