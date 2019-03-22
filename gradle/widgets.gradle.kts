plugins {
    kotlin("jvm")
}

sourceSets {
    getByName("main") {
        java.setSrcDirs(listOf("."))
        java.exclude("**.class")
    }
    getByName("test") {
        java.setSrcDirs(listOf())
    }
}

dependencies {
    compileOnly(rootProject)
    compileOnly(files(projectDir.listFiles().filter { it.path.endsWith(".jar") }))
}