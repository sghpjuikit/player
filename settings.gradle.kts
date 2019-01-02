
rootProject.buildFileName = "gradle/project.gradle.kts"

include(":widgets")
val widgets = project(":widgets")
widgets.projectDir = file("app/widgets")
widgets.buildFileName = "../../gradle/widgets.gradle.kts"
