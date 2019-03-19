plugins {
    kotlin("jvm")
}

sourceSets {
    getByName("main") {
        java.setSrcDirs(listOf("."))
    }
    getByName("test") {
        java.setSrcDirs(listOf())
    }
}

dependencies {
    compileOnly(rootProject)
    compileOnly(files(projectDir.walkTopDown().filter { it.path.endsWith(".jar") }.toList()))
}