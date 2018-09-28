plugins {
    kotlin("jvm")
}

sourceSets.getByName("main").java.srcDir(".")

dependencies {
    compile(rootProject)
    compile(files(projectDir.walkTopDown().filter { it.path.endsWith(".jar") }.toList()))
}