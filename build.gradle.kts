import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.2.20"
    //java
    application
    id("net.ltgt.apt") version "0.13"
    id("idea")
}

// source directories
java.sourceSets {
    getByName("main") {
        java.srcDir("src")
        resources.srcDir("src")
    }
}

/*idea {
    buildDir = file("build")
    module {
        sourceDirs += file('src')
        testSourceDirs += file('working dir/widgets')
    }
}*/

// configure kotlin
val kotlinVersion: String? by extra {
    buildscript.configurations["classpath"].resolvedConfiguration.firstLevelModuleDependencies
            .find { it.moduleName == "org.jetbrains.kotlin.jvm.gradle.plugin" }?.moduleVersion
}

allprojects {
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            suppressWarnings = true
        }
    }
}

tasks.withType<JavaCompile> {
    options.isWarnings = false
    options.isDeprecation = false
    // javac args
    options.compilerArgs = listOf("-Xlint:unchecked", "-nowarn",
            "--add-exports", "javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED",
            "--add-exports", "javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED",
            "--add-exports", "javafx.web/com.sun.webkit=ALL-UNNAMED",
            "--add-exports", "javafx.graphics/com.sun.glass.ui=ALL-UNNAMED")
}

application {
    mainClassName = "sp.it.pl.main.App"
    // jvm args
    applicationDefaultJvmArgs = listOf("-Xmx3g",
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
            "--add-opens", "javafx.web/com.sun.webkit=ALL-UNNAMED",
            "-Duser.dir=" + file("working dir"))
}

repositories {
    jcenter()
}

dependencies {

    // Kotlin
    compile("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", kotlinVersion)
    compile("org.jetbrains.kotlin", "kotlin-reflect", kotlinVersion)

    compile("org.jetbrains.kotlin", "kotlin-test", "1.2.20")

    // Java Native
    compile("net.java.dev.jna", "jna", "4.5.1")
    compile("net.java.dev.jna", "jna-platform", "4.5.1")
    compile("com.1stleg", "jnativehook", "2.0.3")

    // Logging
    compile("org.slf4j", "slf4j-api", "1.7.25")
    compile("ch.qos.logback", "logback-classic", "1.2.3")
    compile("io.github.microutils", "kotlin-logging", "1.5.3")

    // JavaFX
    compile("de.jensd", "fontawesomefx", "8.9")
    compile("org.reactfx", "reactfx", "2.0-M5")
    compile("eu.hansolo", "tilesfx", "1.5.2")
    compile("eu.hansolo", "Medusa", "7.9")

    // Player
    compile("uk.co.caprica", "vlcj", "3.10.1")
    compile("de.u-mass", "lastfm-java", "0.1.2")

    // annotation processor
    compile("org.atteo.classindex", "classindex", "3.4")
    apt("org.atteo.classindex", "classindex", "3.4")

    // misc
    compile("com.adobe.xmp", "xmpcore", "5.1.2")
    compile("com.drewnoakes", "metadata-extractor", "2.9.1")
    compile("net.objecthunter", "exp4j", "0.4.8")
    compile("org.atteo", "evo-inflector", "1.2.2")
    compile("com.thoughtworks.xstream", "xstream", "1.4.10")

    //	compile("com.twelvemonkeys.imageio", "imageio-core", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-bmp", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-jpeg", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-iff", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-icns", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-pcx", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-pict", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-clippath", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-hdr", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-pdf", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-pnm", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-psd", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-tga", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-sgi", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-thumbsdb", "3.3.2")
    //	compile("com.twelvemonkeys.imageio", "imageio-tiff", "3.3.2")

    compile(files(file("working dir/lib").listFiles()))
    // todo compile(fileTree(dir: "working dir/lib", include: "*.jar"))

}

println("Java version: ${JavaVersion.current()}")
println("Kotlin version: $kotlinVersion")

