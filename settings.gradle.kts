
include(":widgets")

rootProject.apply {
    projectDir = file(".")
    buildFileName = "gradle/project.gradle.kts"
}

project(":widgets").apply {
    projectDir = file("app/widgets")
    buildFileName = "../../gradle/widgets.gradle.kts"
}