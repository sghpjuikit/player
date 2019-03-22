rootProject.apply {
    projectDir = file(".")
    buildFileName = "gradle/project.gradle.kts"

    include(":widgets")
    project(":widgets").apply {
        name = "widgets"
        projectDir = file("app")

        file("app/widgets").listFiles().filter { it.name!="META-INF" }.forEach {
            include(":widgets:${it.name}")
            project(":widgets:${it.name}").apply {
                name = "player-widgets-${it.name}"
                projectDir = it
                buildFileName = "../../../gradle/widgets.gradle.kts"
            }
        }
    }
}