buildDir = rootDir.resolve("build").resolve("widgets")

plugins {
    kotlin("jvm")
}

java.sourceSets {
    getByName("main") {
        java.srcDir(".")
    }
}

dependencies {
    compile(rootProject)
    compile(files(file(".").walkTopDown().filter { it.path.endsWith(".jar") }.toList().toTypedArray()))
}