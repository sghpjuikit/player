rootProject.apply {
    projectDir = file(".")
    buildFileName = "gradle/project.gradle.kts"


    include(":util")
    project(":util").apply {
        name = "util"
        projectDir = file("src/util")
        buildFileName = "../../gradle/util.gradle.kts"
    }

    include(":demo")
    project(":demo").apply {
        name = "demo"
        projectDir = file("src/demo")
        buildFileName = "../../gradle/demo.gradle.kts"
    }

    include(":widgets")
    project(":widgets").apply {
        name = "widgets"
        projectDir = file("app")

        file("app/widgets").listFiles().forEach {
            include(":widgets:${it.name}")
            project(":widgets:${it.name}").apply {
                name = "player-widgets-${it.name}"
                projectDir = it
                buildFileName = "../../../gradle/widgets.gradle.kts"
            }
        }
    }
}