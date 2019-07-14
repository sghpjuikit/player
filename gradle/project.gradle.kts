
import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.text.Charsets.UTF_8

// ----- plugin block; evaluated before the script itself

plugins {
   kotlin("jvm") version "1.3.41"
   application
   id("com.github.ben-manes.versions") version "0.20.0"
   id("de.undercouch.download") version "3.4.3"
}

// ----- util block; defined first to help IDE with syntax checking for erroneous code

val String.prjProp: String?
   get() = properties[this]?.toString()

fun Project.tests(configuration: Test.() -> Unit) {
   tasks {
      test {
         configuration()
      }
   }
}

// ----- build block

/** Working directory of the application */
val dirApp = file("app")
val dirJdk = dirApp/"java"
val kotlinVersion: String by extra {
   buildscript.configurations["classpath"]
      .resolvedConfiguration.firstLevelModuleDependencies
      .find { it.moduleName=="org.jetbrains.kotlin.jvm.gradle.plugin" }!!.moduleVersion
}
val javaVersion = JavaVersion.current()
val javaSupportedVersions = arrayOf(JavaVersion.VERSION_11, JavaVersion.VERSION_12).also {
   if (javaVersion !in it) {
      println("" +
         "Java version $javaVersion can't be used.\n" +
         "Set one of ${it.joinToString()} as system default or create a 'gradle.properties'" +
         "file with 'org.gradle.java.home' pointing to a supported Java version.\n" +
         "Currently org.gradle.java.home=${"org.gradle.java.home".prjProp}"
      )
      throw IllegalStateException("Invalid Java version: ${JavaVersion.current()}")
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
         "--add-exports", "javafx.graphics/com.sun.glass.ui=ALL-UNNAMED",
         "--add-exports", "javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED"
      )
   }

   tasks.withType<KotlinCompile> {
      kotlinOptions.suppressWarnings = false
      kotlinOptions.verbose = true
      kotlinOptions.freeCompilerArgs += listOf(
         "-progressive",
         //                "-Xuse-javac",
         //                "-Xcompile-java",
         "-Xno-call-assertions",
         "-Xno-param-assertions",
         "-Xjvm-default=enable"
      )
      kotlinOptions.javaParameters = true
      kotlinOptions.jdkHome = dirJdk.path
      kotlinOptions.jvmTarget = "12"
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
         implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core")
         implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-javafx", "1.1.0")
         implementation("org.jetbrains", "annotations", "17.0.0")
      }

      "JavaFX" group {
         val version = "13-ea+9"
         val os = org.gradle.internal.os.OperatingSystem.current()
         val classifier = when {
            os.isLinux -> "linux"
            os.isMacOsX -> "mac"
            os.isWindows -> "win"
            else -> failIO { "Unable to determine javafx dependency classifier due to unfamiliar system=$os" }
         }
         val jfxLibs = arrayOf("base", "controls", "graphics", "fxml", "media", "swing", "web")
         for (lib in jfxLibs) {
            implementation("org.openjfx", "javafx-$lib", version, classifier = classifier)
         }
         implementation("de.jensd", "fontawesomefx", "8.9")
      }
      "Logging" group {
         implementation("org.slf4j", "slf4j-api")
         implementation("org.slf4j", "jul-to-slf4j", "1.7.25")
         implementation("ch.qos.logback", "logback-classic", "1.2.3")
         implementation("io.github.microutils", "kotlin-logging", "1.6.22")
      }

      "Audio" group {
         implementation("uk.co.caprica", "vlcj", "4.0.6")
         implementation("de.u-mass", "lastfm-java", "0.1.2")
         implementation("com.github.goxr3plus", "Jaudiotagger", "V2.2.6")
      }

      "Native" group {
         implementation("net.java.dev.jna", "jna-platform")
         implementation("com.1stleg", "jnativehook", "2.1.0")
      }

      "Misc" group {
         implementation("net.objecthunter", "exp4j", "0.4.8")
         implementation("org.atteo", "evo-inflector", "1.2.2")
         implementation("com.thoughtworks.xstream", "xstream", "1.4.11.1")
         implementation("com.github.ajalt", "clikt", "2.1.0")
      }

      "Image" group {
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

      "Test" group {
         testImplementation("io.kotlintest", "kotlintest-runner-junit5", "3.2.1")
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
   val main = "_Main"

   val copyLibs by creating(Sync::class) {
      group = "build"
      description = "Copies all libraries into the app dir"
      from(configurations.compileClasspath)
      into(dirApp/"lib")
   }

   val linkJdk by creating(LinkJDK::class) {
      group = "build setup"
      description = "Links JDK to project relative directory"
      linkDir = dirJdk
      jdkVersion = javaVersion
   }

   val kotlinc by creating(Download::class) {
      group = "build setup"
      description = "Downloads required version of the kotlin compiler"

      val os = org.gradle.internal.os.OperatingSystem.current()
      val useExperimentalKotlinc = "player.kotlinc.experimental".prjProp?.toBoolean() ?: true
      val kotlincDir = dirApp/"kotlinc"
      val kotlincVersionFile = kotlincDir/"version"
      val kotlincZipName = when {
         !useExperimentalKotlinc -> "kotlin-compiler-$kotlinVersion.zip"
         os.isLinux -> "experimental-kotlin-compiler-linux-x64.zip"
         os.isMacOsX -> "experimental-kotlin-compiler-macos-x64.zip"
         os.isWindows -> "experimental-kotlin-compiler-windows-x64.zip"
         else -> failIO { "Unable to determine kotlinc version due to unfamiliar system=$os" }
      }
      val kotlincBinary = kotlincDir/"bin"/"kotlinc"
      val kotlincZip = kotlincDir/kotlincZipName
      val kotlincLinkVersion = if (!useExperimentalKotlinc) kotlinVersion else "1.3.31"
      val kotlincLink = "https://github.com/JetBrains/kotlin/releases/download/v$kotlincLinkVersion/$kotlincZipName"

      doFirst {
         println("Obtaining Kotlin compiler experimental=$useExperimentalKotlinc from=$kotlincLink")
         if (kotlincDir.exists()) {
            println("Deleting obsolete version of Kotlin compiler...")
            kotlincDir.deleteRecursively().orFailIO { "Failed to remove Kotlin compiler, location=$kotlincDir" }
         }
         if (!kotlincDir.exists()) {
            kotlincDir.mkdirs().orFailIO { "Failed to create directory=$kotlincDir" }
         }
         println("Downloading...")
      }

      src(kotlincLink)
      dest(kotlincDir)
      setOnlyIf { !kotlincVersionFile.exists() || kotlincVersionFile.readText() != src.toString() }

      doLast {
         copy {
            from(if (kotlincZipName.endsWith("zip")) zipTree(kotlincZip) else tarTree(kotlincZip)) {
               eachFile { relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray()) }
               includeEmptyDirs = false
            }
            into(kotlincDir)
         }
         kotlincBinary.setExecutable(true).orFailIO { "Failed to make file=$kotlincBinary executable" }
         kotlincZip.delete().orFailIO { "Failed to clean up downloaded file=$kotlincZip" }
         kotlincVersionFile.writeText(src.toString())
      }
   }

   val generateFileHierarchy by creating(FileHierarchyInfo::class) {
      group = "build setup"
      description = "Generates file hierarchy class and documentation"
      inFileHierarchy = project.rootDir/"file-info"
      outFileHierarchy = project.rootDir/"src"/"player"/"main"/"sp"/"it"/"pl"/"main"/"AppFiles.kt"
      outPackage = "sp.it.pl.main"
      outIndent = "   "
      outRootPath = """File("").absolutePath"""
   }

   val jar by getting(Jar::class) {
      dependsOn(copyLibs, kotlinc, generateFileHierarchy)
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

application {
   applicationName = "PlayerFX"
   mainClassName = "sp.it.pl.main.AppKt"
   applicationDefaultJvmArgs = listOf(
      "-Dfile.encoding=UTF-8",
      "-ms" + ("player.memoryMin".prjProp ?: "50m"),
      "-mx" + ("player.memoryMax".prjProp ?: "3g"),
      "-XX:MinHeapFreeRatio=5",
      "-XX:MaxHeapFreeRatio=10",
      "-XX:+UseStringDeduplication",
      "-XX:+UseCompressedOops",
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
      "--add-opens", "javafx.graphics/com.sun.prism=ALL-UNNAMED",
      "--add-opens", "javafx.web/com.sun.webkit=ALL-UNNAMED"
   )
}