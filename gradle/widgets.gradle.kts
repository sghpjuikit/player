plugins {
    kotlin("jvm")
}

sourceSets {
    main {
        java.srcDir(".")
        java.exclude("**.class")
        resources.setSrcDirs(listOf())
    }
    test {
        java.setSrcDirs(listOf())
        resources.setSrcDirs(listOf())
    }
}

dependencies {
    compileOnly(rootProject)
    compileOnly(files(projectDir.listFiles().filter { it.path.endsWith(".jar") }))
}