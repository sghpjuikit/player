
//buildDir = rootDir.resolve("build").resolve("widgets")

plugins {
    kotlin("jvm")
}

/*
java.sourceSets {
    getByName("main") {
        java.srcDir("../widgets")
    }
}
*/

dependencies {
    // include all libs from the actual Project
    compile(rootProject)

    // include all libs in the widgets directory and its subdirectories
    compile(files(file(".").listFiles().also { it.joinToString() }
            .flatMap {
                if(it.isDirectory) listOf<File>(*it.listFiles())
                else listOf<File>(it)
            }
            .filter { it.extension == "jar" }))
}