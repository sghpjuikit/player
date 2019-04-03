plugins {
    kotlin("jvm")
}

sourceSets {
    getByName("main") {
        java.setSrcDirs(listOf("main"))
        resources.setSrcDirs(listOf("main"))
    }
    getByName("test") {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("test"))
    }
}

dependencies {
    implementation(project(":util"))
}