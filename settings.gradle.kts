
rootProject.apply {
    buildFileName = "gradle/project.gradle.kts"
}

include(":widgets")
project(":widgets").apply {
    projectDir = file("working dir/widgets")
    buildFileName = "../../gradle/widgets.gradle.kts"
}
