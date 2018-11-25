plugins {
    kotlin("jvm")
}

sourceSets {
    getByName("main").java.srcDir(".")
}

dependencies {
    implementation(rootProject)
    implementation(files(projectDir.walkTopDown().filter { it.path.endsWith(".jar") }.toList()))
}